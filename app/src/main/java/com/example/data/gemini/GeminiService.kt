package com.example.data.gemini

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("astra_gemini_prefs", Context.MODE_PRIVATE)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        const val PREF_CUSTOM_KEY = "custom_gemini_key"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        const val SYSTEM_PROMPT =
            "You are ASTRA, an advanced, intelligent, calm, confident, and respectful futuristic AI personal assistant for Android tablets. " +
            "Your tagline is 'Intelligence Beyond Limits'. " +
            "Communicate intelligently, helpfully, and with technological sophistication. " +
            "Keep verbal explanations clear, structured, and concise so they are pleasant to hear via speech synthesis. " +
            "You can guide users on device controls, tablet security via ASTRA GUARD, and general inquiries."
    }

    fun getEffectiveApiKey(): String {
        val customKey = prefs.getString(PREF_CUSTOM_KEY, "")?.trim() ?: ""
        if (customKey.isNotEmpty()) {
            return customKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
        return if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString(PREF_CUSTOM_KEY, key.trim()).apply()
    }

    fun clearCustomApiKey() {
        prefs.edit().remove(PREF_CUSTOM_KEY).apply()
    }

    fun hasValidApiKey(): Boolean {
        return getEffectiveApiKey().isNotEmpty()
    }

    suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(Exception("No API key configured. Please enter a valid Gemini API key."))
        }

        val startTime = System.currentTimeMillis()
        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val payload = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Ping. Respond with 'PONG'."))
                        })
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                })
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            okHttpClient.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    Result.success(elapsed)
                } else {
                    val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                    val message = parseErrorMessage(errorBody) ?: "HTTP ${response.code}: $errorBody"
                    Result.failure(Exception(message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured. Go to Settings to provide your API key.")
            )
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // Include system instruction via systemInstruction field
            val systemInstruction = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", SYSTEM_PROMPT))
                })
            }

            // Add previous conversation turns if provided
            for ((role, text) in history.takeLast(6)) {
                val apiRole = if (role.equals("USER", ignoreCase = true)) "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", apiRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", text))
                    })
                })
            }

            // Append current user prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", prompt))
                })
            })

            val payload = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", systemInstruction)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                })
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = parseErrorMessage(body) ?: "Server responded with code ${response.code}"
                    return@withContext Result.failure(Exception(errMsg))
                }

                val jsonResponse = JSONObject(body)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val replyText = parts.getJSONObject(0).optString("text", "")
                        return@withContext Result.success(replyText.trim())
                    }
                }
                Result.failure(Exception("No content returned from ASTRA core."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(errorBody: String): String? {
        return try {
            val json = JSONObject(errorBody)
            val errorObj = json.optJSONObject("error")
            errorObj?.optString("message")
        } catch (_: Exception) {
            null
        }
    }
}

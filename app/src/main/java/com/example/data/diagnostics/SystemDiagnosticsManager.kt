package com.example.data.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

data class SystemDiagnosticsReport(
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val usedRamBytes: Long = 0L,
    val ramPercentUsed: Float = 0f,
    val isLowMemory: Boolean = false,

    val totalStorageBytes: Long = 0L,
    val freeStorageBytes: Long = 0L,
    val usedStorageBytes: Long = 0L,
    val storagePercentUsed: Float = 0f,

    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val chargingSource: String = "Battery",
    val batteryHealth: String = "Good",
    val batteryVoltageMv: Int = 0,
    val batteryTemperatureC: Float = 0f,
    val batteryTechnology: String = "Li-ion",

    val deviceManufacturer: String = Build.MANUFACTURER,
    val deviceModel: String = Build.MODEL,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val securityPatch: String = Build.VERSION.SECURITY_PATCH ?: "Current",
    val cpuAbi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),

    val networkConnected: Boolean = true,
    val networkType: String = "Wi-Fi",
    val networkTransport: String = "TRANSPORT_WIFI",

    val pingLatencyMs: Long? = null,
    val pingStatus: String = "IDLE",
    val lastPingTimestamp: Long = 0L
)

class SystemDiagnosticsManager(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun gatherSystemReport(): SystemDiagnosticsReport {
        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem
        val availRam = memoryInfo.availMem
        val usedRam = totalRam - availRam
        val ramPercent = if (totalRam > 0) (usedRam.toFloat() / totalRam.toFloat()) * 100f else 0f

        // Storage
        val dataDir: File = Environment.getDataDirectory()
        val statFs = StatFs(dataDir.path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availBlocks = statFs.availableBlocksLong

        val totalStorage = totalBlocks * blockSize
        val freeStorage = availBlocks * blockSize
        val usedStorage = totalStorage - freeStorage
        val storagePercent = if (totalStorage > 0) (usedStorage.toFloat() / totalStorage.toFloat()) * 100f else 0f

        // Battery
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargeSource = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Mains"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Dock"
            else -> if (isCharging) "Charging" else "Battery Powered"
        }

        val healthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val batteryHealth = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal (Good)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat Warning"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Critical Depletion"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold Condition"
            else -> "Normal"
        }

        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = rawTemp / 10f
        val tech = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        // Network
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

        val isConnected = caps != null && (
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                )

        val (netType, transport) = when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (802.11 High-Speed)" to "WLAN"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular (LTE/5G)" to "WWAN"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Gigabit Ethernet" to "LAN"
            else -> "Disconnected / Unavailable" to "NONE"
        }

        return SystemDiagnosticsReport(
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            usedRamBytes = usedRam,
            ramPercentUsed = ramPercent,
            isLowMemory = memoryInfo.lowMemory,

            totalStorageBytes = totalStorage,
            freeStorageBytes = freeStorage,
            usedStorageBytes = usedStorage,
            storagePercentUsed = storagePercent,

            batteryLevel = batteryPct,
            isCharging = isCharging,
            chargingSource = chargeSource,
            batteryHealth = batteryHealth,
            batteryVoltageMv = voltageMv,
            batteryTemperatureC = tempC,
            batteryTechnology = tech,

            networkConnected = isConnected || caps != null,
            networkType = netType,
            networkTransport = transport
        )
    }

    suspend fun runNetworkPingTest(): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            // Attempt socket connect to Cloudflare / Google public DNS port 53 (ultra-fast, zero-overhead)
            Socket().use { socket ->
                socket.connect(InetSocketAddress("1.1.1.1", 53), 3000)
            }
            val latency = System.currentTimeMillis() - startTime
            true to latency
        } catch (e: Exception) {
            // Fallback to HTTP request
            try {
                val httpStart = System.currentTimeMillis()
                val request = Request.Builder()
                    .url("https://www.google.com/generate_204")
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - httpStart
                    response.isSuccessful to latency
                }
            } catch (e2: Exception) {
                false to -1L
            }
        }
    }

    suspend fun testApiEndpoint(url: String, apiKey: String): Triple<Boolean, Long, String> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val targetUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    "https://$url"
                }

                val requestBuilder = Request.Builder().url(targetUrl)
                if (apiKey.isNotBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                    requestBuilder.addHeader("x-goog-api-key", apiKey)
                }

                val request = requestBuilder.build()
                okHttpClient.newCall(request).execute().use { response ->
                    val latency = System.currentTimeMillis() - startTime
                    val code = response.code
                    val message = "HTTP $code - ${response.message.ifEmpty { "Response Received" }}"
                    Triple(response.isSuccessful || code in 200..399, latency, message)
                }
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                Triple(false, latency, e.localizedMessage ?: "Connection timed out or host unreachable.")
            }
        }
}

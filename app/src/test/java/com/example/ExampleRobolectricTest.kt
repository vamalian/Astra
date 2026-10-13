package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.pref.AdminPreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context verifies app name`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ASTRA NEXUS", appName)
  }

  @Test
  fun `default admin pin is 600 and verifies correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AdminPreferencesManager(context)

    assertTrue("Default PIN should be active initially", prefs.isDefaultPin())
    assertTrue("PIN 600 should authenticate successfully", prefs.verifyPin("600"))
    assertFalse("Incorrect PIN should be rejected", prefs.verifyPin("1234"))
  }

  @Test
  fun `admin pin change validates old pin and persists new pin`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AdminPreferencesManager(context)

    // Attempt to change with wrong old pin
    val wrongOld = prefs.changePin("999", "888")
    assertFalse("Changing with wrong current PIN should fail", wrongOld)

    // Change with valid old pin 600
    val success = prefs.changePin("600", "7890")
    assertTrue("Changing with correct current PIN should succeed", success)
    assertTrue("New PIN 7890 should authenticate", prefs.verifyPin("7890"))
    assertFalse("Old PIN 600 should no longer authenticate", prefs.verifyPin("600"))
  }

  @Test
  fun `export and import configuration profile`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AdminPreferencesManager(context)

    val exported = prefs.exportConfigJson()
    assertTrue("Exported JSON must identify ASTRA NEXUS", exported.contains("ASTRA NEXUS"))

    val importResult = prefs.importConfigJson(exported)
    assertTrue("Importing exported JSON should succeed", importResult.isSuccess)
  }
}

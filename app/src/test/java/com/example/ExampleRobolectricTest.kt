package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.compat.Arm32Emulator
import com.example.compat.SystemAbiAuditor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("32-Bit Bridge", appName)
  }

  @Test
  fun `verify system audit execution`() {
    val info = SystemAbiAuditor.auditDevice()
    assertNotNull(info)
    assertNotNull(info.supportedAbis)
    assertTrue(info.diagnosticSummary.isNotEmpty())
  }

  @Test
  fun `verify arm32 emulator step and execution`() {
    val emulator = Arm32Emulator()
    emulator.loadPresetProgram(Arm32Emulator.PresetProgram.HELLO_SYSCALL)
    val stepped = emulator.step()
    assertTrue(stepped)
    assertEquals(1, emulator.state.r0)

    emulator.runAll()
    assertTrue(emulator.consoleOutput.any { it.contains("KONSOL ÇIKTISI") })
  }
}

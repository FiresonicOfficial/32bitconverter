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

  @Test
  fun `verify file selection utility recents and storage scanner`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testApk = java.io.File(context.cacheDir, "test_sample.apk")
    testApk.writeText("fake apk binary")

    com.example.compat.FileSelectionUtility.recordRecentlySelectedApk(context, testApk)
    val recents = com.example.compat.FileSelectionUtility.getRecentlySelectedApks(context)
    assertTrue(recents.any { it.name == "test_sample.apk" })

    val scanned = com.example.compat.FileSelectionUtility.scanStorageForApks(context)
    assertNotNull(scanned)
  }

  @Test
  fun `verify android 9 64-bit emulator initialization and apk install`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.compat.Android9VirtualEnvironment.initialize(context)

    val apps = com.example.compat.Android9VirtualEnvironment.installedApps
    assertTrue(apps.any { it.packageName == "com.android.settings" })
    assertTrue(apps.any { it.packageName == "com.android.terminal" })
    assertTrue(apps.any { it.packageName == "com.android.packageinstaller" })

    // Test shell commands
    val uname = com.example.compat.Android9VirtualEnvironment.runShellCommand("uname -a")
    assertTrue(uname.contains("aarch64"))
    assertTrue(uname.contains("android9"))

    val getprop = com.example.compat.Android9VirtualEnvironment.runShellCommand("getprop ro.build.version.release")
    assertEquals("9", getprop.trim())

    val pmList = com.example.compat.Android9VirtualEnvironment.runShellCommand("pm list packages")
    assertTrue(pmList.contains("com.android.settings"))

    // Test install sample APK
    val dummyApk = java.io.File(context.cacheDir, "unit_test_app.apk")
    dummyApk.writeText("TEST_APK_CONTENT")
    val installResult = com.example.compat.Android9VirtualEnvironment.installApkFile(
        context,
        dummyApk,
        "Test Sanal Uygulama",
        false
    )
    assertTrue(installResult.isSuccess)
    val installedApp = installResult.getOrThrow()
    assertEquals("Test Sanal Uygulama", installedApp.appName)

    // Test launching app
    com.example.compat.Android9VirtualEnvironment.launchApp(installedApp)
    assertEquals(installedApp.packageName, com.example.compat.Android9VirtualEnvironment.activeForegroundApp.value?.packageName)
    assertTrue(com.example.compat.Android9VirtualEnvironment.runningProcesses.any { it.packageName == installedApp.packageName })

    // Test close app
    com.example.compat.Android9VirtualEnvironment.closeActiveApp()
    assertEquals(null, com.example.compat.Android9VirtualEnvironment.activeForegroundApp.value)
  }

  @Test
  fun `verify transferred apk architecture scanner detects 32bit and 64bit apps`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.compat.Android9VirtualEnvironment.initialize(context)

    val scannedList = com.example.compat.TransferredApkScanner.scanAllTransferredApks(context)
    assertTrue("En az bir uygulama taranabilmeli", scannedList.isNotEmpty())

    // Check retro 32-bit arcade detection
    val retro32 = scannedList.find { it.packageName == "com.retro.arcade32" }
    assertNotNull("32-bit örnek uygulama taranmış olmalı", retro32)
    assertTrue("retro32 32-bit olarak işaretlenmeli", retro32!!.is32Bit)
    assertTrue("Houdini çevirisi gerektirmeli", retro32.requiresTranslation)
    assertTrue("Mimari etiketi 32-Bit içermeli", retro32.architectureLabel.contains("32-Bit"))

    // Check ARM64 benchmark detection
    val bench64 = scannedList.find { it.packageName == "com.benchmark.arm64" }
    assertNotNull("64-bit örnek uygulama taranmış olmalı", bench64)
    assertTrue("bench64 64-bit olarak işaretlenmeli", bench64!!.is64Bit)
    assertTrue("Mimari etiketi 64-Bit içermeli", bench64.architectureLabel.contains("64-Bit"))
  }
}


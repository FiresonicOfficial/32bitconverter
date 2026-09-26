package com.example.model

import android.net.Uri

enum class CompatibilityVerdict {
    NATIVE_64_BIT,             // Already contains arm64-v8a native code
    PURE_DEX_COMPATIBLE,       // No native libraries; 100% works on 64-bit OS if install block bypassed
    STRIPPABLE_32BIT_STUBS,    // Has legacy/dummy 32-bit .so files that can be stripped to run in 64-bit ART
    NEEDS_BINARY_TRANSLATION,  // Has core 32-bit native engine; requires Tango / translation bridge
    UNKNOWN
}

data class NativeLibraryInfo(
    val name: String,
    val abi: String,
    val sizeBytes: Long,
    val is64Bit: Boolean,
    val elfMachine: String = ""
)

data class ApkAnalysisResult(
    val fileName: String,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val totalSizeBytes: Long,
    val dexCount: Int,
    val nativeLibraries: List<NativeLibraryInfo>,
    val supportedAbis: Set<String>,
    val verdict: CompatibilityVerdict,
    val compatibilityScore: Int, // 0 to 100
    val analysisDetails: List<String>,
    val recommendedActions: List<String>,
    val sourceUri: Uri? = null,
    val sourceFilePath: String? = null
)

data class DeviceAbiInfo(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val isStrict64BitOnly: Boolean,
    val supportedAbis: List<String>,
    val supported64BitAbis: List<String>,
    val supported32BitAbis: List<String>,
    val cpuArch: String,
    val hardwareAArch32Support: Boolean,
    val system32BitLibrariesPresent: Boolean,
    val zygoteMode: String,
    val translationLayerDetected: String?,
    val diagnosticSummary: String
)

data class PatchedApkRecord(
    val id: String,
    val originalName: String,
    val packageName: String,
    val patchedFilePath: String,
    val originalSizeBytes: Long,
    val patchedSizeBytes: Long,
    val patchTimestamp: Long,
    val patchMode: String,
    val isInstallable: Boolean,
    val clonedPackageName: String? = null,
    val clonedAppName: String? = null,
    val isClonedApp: Boolean = false
)

data class ArmRegisterState(
    val r0: Int = 0,
    val r1: Int = 0,
    val r2: Int = 0,
    val r3: Int = 0,
    val r4: Int = 0,
    val r5: Int = 0,
    val r6: Int = 0,
    val r7: Int = 0,
    val r8: Int = 0,
    val r9: Int = 0,
    val r10: Int = 0,
    val r11: Int = 0,
    val r12: Int = 0,
    val sp: Int = 0x7FFFF000,
    val lr: Int = 0,
    val pc: Int = 0x00010000,
    val flagN: Boolean = false,
    val flagZ: Boolean = false,
    val flagC: Boolean = false,
    val flagV: Boolean = false
)

data class DisassembledInstruction(
    val address: Int,
    val rawHex32: String,
    val arm32Mnemonic: String,
    val arm32Operands: String,
    val translatedArm64Mnemonic: String,
    val translatedArm64Operands: String,
    val explanation: String
)

data class DiscoveredApkFile(
    val file: java.io.File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val directoryCategory: String,
    val isSuggested32Bit: Boolean = false
)

data class Android9VirtualApp(
    val id: String,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean = false,
    val is32Bit: Boolean = false,
    val abi: String = "arm64-v8a",
    val targetSdk: Int = 28,
    val installPath: String,
    val installedAt: Long = System.currentTimeMillis(),
    val sourceApkPath: String? = null,
    val nativeLibs: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val appIconColorHex: Long = 0xFF00E5FF,
    val appIconSymbol: String = "A",
    val category: String = "Kullanıcı Uygulaması"
)

data class Android9Process(
    val pid: Int,
    val packageName: String,
    val appName: String,
    val memoryMb: Int,
    val cpuPercent: Float,
    val isRunning: Boolean,
    val startedAt: Long,
    val is32BitTranslated: Boolean = false
)

data class VirtualLogcatEntry(
    val timestamp: String,
    val pid: Int,
    val tag: String,
    val level: String, // "I", "D", "W", "E"
    val message: String
)

data class EmulatorSystemState(
    val osVersion: String = "9.0 (Pie)",
    val apiLevel: Int = 28,
    val buildId: String = "PQ3A.190801.002",
    val architecture: String = "arm64-v8a (64-Bit)",
    val kernelVersion: String = "4.9.148-android9-64-pie-qemu",
    val totalRamMb: Int = 4096,
    val usedRamMb: Int = 1420,
    val storageTotalGb: Int = 64,
    val storageUsedGb: Float = 14.2f,
    val batteryPercent: Int = 89,
    val isWifiConnected: Boolean = true,
    val isHoudini32BitTranslationActive: Boolean = true,
    val isArtJitEnabled: Boolean = true,
    val uptimeSeconds: Long = 1840L
)

data class TransferredApkArchitectureInfo(
    val id: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val is32Bit: Boolean,
    val is64Bit: Boolean,
    val isPureDex: Boolean,
    val primaryAbi: String,                 // e.g. "armeabi-v7a", "arm64-v8a", "x86", "x86_64", "universal-dex"
    val architectureLabel: String,          // e.g. "32-Bit ARMv7 (AArch32)", "64-Bit ARM64 (AArch64)", "Saf DEX (64-Bit ART)"
    val supportedAbis: List<String>,        // e.g. ["armeabi-v7a"]
    val nativeLibraries: List<NativeLibraryInfo>,
    val totalSizeBytes: Long,
    val dexCount: Int,
    val executionMode: String,              // "ARM32 -> ARM64 Houdini İkili Çeviri" vs "64-Bit Yerel ART JIT"
    val compatibilityVerdict: CompatibilityVerdict,
    val installPath: String,
    val sourceApkPath: String? = null,
    val scannedAt: Long = System.currentTimeMillis(),
    val appIconColorHex: Long = 0xFF00E5FF,
    val appIconSymbol: String = "A",
    val requiresTranslation: Boolean = is32Bit,
    val originSource: String = "Android 9 VM"
)

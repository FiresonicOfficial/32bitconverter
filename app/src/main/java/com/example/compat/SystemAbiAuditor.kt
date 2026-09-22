package com.example.compat

import android.os.Build
import com.example.model.DeviceAbiInfo
import java.io.File

object SystemAbiAuditor {

    fun auditDevice(): DeviceAbiInfo {
        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val supported64 = Build.SUPPORTED_64_BIT_ABIS.toList()
        val supported32 = Build.SUPPORTED_32_BIT_ABIS.toList()

        val isStrict64BitOnly = supported32.isEmpty()

        // Check /system/lib for 32-bit bionic libc
        val sys32LibExists = File("/system/lib/libc.so").exists() || File("/system/lib").isDirectory

        // Check hardware AArch32 capability from cpuinfo
        val hwAArch32 = checkHardwareAArch32()

        // Detect zygote mode
        val zygoteMode = getSystemProperty("ro.zygote", "zygote64")

        // Detect known translation engines
        val translationEngine = detectTranslationLayer()

        val summary = when {
            isStrict64BitOnly && !hwAArch32 -> {
                "Cihazınız tamamen 64-bit'tir (Donanım CPU çekirdeklerinde ve OS düzeyinde 32-bit desteği kaldırılmıştır). 32-bit uygulamaları çalıştırmak için Tango ikili çevirici (binary translator) veya APK 64-bit dönüştürücü gereklidir."
            }
            isStrict64BitOnly && hwAArch32 -> {
                "Cihazınızın işlemcisi donanımsal olarak 32-bit destekliyor ancak işletim sistemi (OS) 32-bit kütüphaneleri dahil etmemiştir. Zygisk modülleri veya ADB ABI zorlamasıyla 32-bit etkinleştirilebilir."
            }
            else -> {
                "Cihazınız hem 32-bit (armeabi-v7a) hem de 64-bit (arm64-v8a) uygulamaları yerel olarak desteklemektedir."
            }
        }

        return DeviceAbiInfo(
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            manufacturer = Build.MANUFACTURER,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            sdkInt = Build.VERSION.SDK_INT,
            isStrict64BitOnly = isStrict64BitOnly,
            supportedAbis = supportedAbis,
            supported64BitAbis = supported64,
            supported32BitAbis = supported32,
            cpuArch = System.getProperty("os.arch") ?: "aarch64",
            hardwareAArch32Support = hwAArch32,
            system32BitLibrariesPresent = sys32LibExists,
            zygoteMode = zygoteMode,
            translationLayerDetected = translationEngine,
            diagnosticSummary = summary
        )
    }

    private fun checkHardwareAArch32(): Boolean {
        // Modern ARMv9 cores (Cortex-X3, Cortex-X4, Cortex-A520, Cortex-A720) dropped AArch32 state completely.
        // If supported32 is not empty, AArch32 is clearly available.
        if (Build.SUPPORTED_32_BIT_ABIS.isNotEmpty()) return true

        try {
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.exists()) {
                val text = cpuInfo.readText()
                // AArch32 execution state CPUs support 'swp', 'half', 'thumb', 'fastmult', 'vfp' etc in features
                val featuresLine = text.lines().firstOrNull { it.startsWith("Features") } ?: ""
                if (featuresLine.contains("thumb", ignoreCase = true) || featuresLine.contains("vfp", ignoreCase = true)) {
                    return true
                }
                // Check CPU part
                // 0xd42 = Cortex-X3 (no aarch32)
                // 0xd48 = Cortex-X4 (no aarch32)
                // 0xd47 = Cortex-A520 (no aarch32)
                if (text.contains("0xd42") || text.contains("0xd48") || text.contains("0xd47")) {
                    return false
                }
            }
        } catch (_: Exception) {}

        // In 64-bit-only Android, default is false
        return false
    }

    private fun detectTranslationLayer(): String? {
        val tangoPaths = listOf(
            "/data/adb/modules/tango",
            "/system/bin/tango",
            "/data/local/tmp/tango"
        )
        for (path in tangoPaths) {
            if (File(path).exists()) return "Tango ARM32-to-ARM64 Translator"
        }

        val houdiniPaths = listOf(
            "/system/lib64/libhoudini.so",
            "/vendor/lib64/libhoudini.so"
        )
        for (path in houdiniPaths) {
            if (File(path).exists()) return "Intel/Android LibHoudini"
        }

        if (File("/data/adb/modules/zygisk_32_bit").exists()) {
            return "Zygisk 32-Bit Enabler"
        }

        return null
    }

    private fun getSystemProperty(key: String, defaultValue: String): String {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            getMethod.invoke(null, key, defaultValue) as? String ?: defaultValue
        } catch (_: Exception) {
            defaultValue
        }
    }
}

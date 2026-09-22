package com.example.compat

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.example.model.ApkAnalysisResult
import com.example.model.CompatibilityVerdict
import com.example.model.NativeLibraryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ApkAnalyzer {

    suspend fun analyzeApkFromUri(context: Context, uri: Uri): ApkAnalysisResult = withContext(Dispatchers.IO) {
        // Copy to temporary cache file for reliable random access and analysis
        val tempFile = File(context.cacheDir, "inspect_target_${System.currentTimeMillis()}.apk")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            analyzeApkFile(context, tempFile, uri)
        } finally {
            // Keep temp file if needed for patching, or clean up later
        }
    }

    suspend fun analyzeInstalledApp(context: Context, packageName: String): ApkAnalysisResult = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val apkFile = File(appInfo.sourceDir)
        val result = analyzeApkFile(context, apkFile, null)
        val packageInfo = pm.getPackageInfo(packageName, 0)
        val label = pm.getApplicationLabel(appInfo).toString()
        result.copy(
            appName = label,
            packageName = packageName,
            versionName = packageInfo.versionName ?: "1.0",
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
            minSdk = appInfo.minSdkVersion,
            targetSdk = appInfo.targetSdkVersion
        )
    }

    fun analyzeApkFile(context: Context, file: File, sourceUri: Uri?): ApkAnalysisResult {
        val nativeLibs = mutableListOf<NativeLibraryInfo>()
        val supportedAbis = mutableSetOf<String>()
        var dexCount = 0
        var fileName = file.name

        // Try reading package details via PackageManager
        val pm = context.packageManager
        val pkgArchiveInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
        var packageName = pkgArchiveInfo?.packageName ?: "bilinmeyen.paket"
        var versionName = pkgArchiveInfo?.versionName ?: "1.0"
        var versionCode = 1L
        var minSdk = 21
        var targetSdk = 28
        var appName = file.nameWithoutExtension

        if (pkgArchiveInfo != null) {
            val appInfo = pkgArchiveInfo.applicationInfo
            if (appInfo != null) {
                appInfo.sourceDir = file.absolutePath
                appInfo.publicSourceDir = file.absolutePath
                try {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    if (label.isNotBlank()) appName = label
                } catch (_: Exception) {}

                minSdk = appInfo.minSdkVersion
                targetSdk = appInfo.targetSdkVersion
            }
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkgArchiveInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgArchiveInfo.versionCode.toLong()
            }
        }

        try {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry: ZipEntry = entries.nextElement()
                    val name = entry.name

                    if (name.startsWith("classes") && name.endsWith(".dex")) {
                        dexCount++
                    }

                    if (name.startsWith("lib/") && name.endsWith(".so")) {
                        val parts = name.split("/")
                        if (parts.size >= 3) {
                            val abi = parts[1]
                            val libName = parts.last()
                            supportedAbis.add(abi)

                            var elfMachine = ""
                            var is64 = abi.contains("64")

                            // Read ELF Header to verify bitness and machine type
                            try {
                                zip.getInputStream(entry).use { stream ->
                                    val headerBytes = ByteArray(52)
                                    val read = stream.read(headerBytes)
                                    if (read >= 20 && headerBytes[0] == 0x7F.toByte() &&
                                        headerBytes[1] == 'E'.code.toByte() &&
                                        headerBytes[2] == 'L'.code.toByte() &&
                                        headerBytes[3] == 'F'.code.toByte()
                                    ) {
                                        val elfClass = headerBytes[4].toInt() // 1 = 32-bit, 2 = 64-bit
                                        is64 = elfClass == 2
                                        val machine = (headerBytes[18].toInt() and 0xFF) or
                                                ((headerBytes[19].toInt() and 0xFF) shl 8)
                                        elfMachine = when (machine) {
                                            0x28 -> "ARM (AArch32)"
                                            0xB7 -> "AArch64"
                                            0x03 -> "x86"
                                            0x3E -> "x86_64"
                                            else -> "Machine 0x${Integer.toHexString(machine)}"
                                        }
                                    }
                                }
                            } catch (_: Exception) {}

                            nativeLibs.add(
                                NativeLibraryInfo(
                                    name = libName,
                                    abi = abi,
                                    sizeBytes = entry.size,
                                    is64Bit = is64,
                                    elfMachine = elfMachine
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Evaluate verdict
        val has64Bit = nativeLibs.any { it.is64Bit }
        val has32Bit = nativeLibs.any { !it.is64Bit }
        val hasOnly32Bit = has32Bit && !has64Bit
        val isPureDex = nativeLibs.isEmpty()

        val verdict: CompatibilityVerdict
        val score: Int
        val details = mutableListOf<String>()
        val actions = mutableListOf<String>()

        when {
            has64Bit && !has32Bit -> {
                verdict = CompatibilityVerdict.NATIVE_64_BIT
                score = 100
                details.add("Uygulama yerel olarak 64-bit (arm64-v8a) kütüphaneleri içermektedir.")
                details.add("Herhangi bir dönüştürücüye ihtiyaç duymadan 64-bit OS'lerde doğrudan çalışır.")
                actions.add("Doğrudan yüklenebilir.")
            }
            has64Bit && has32Bit -> {
                verdict = CompatibilityVerdict.NATIVE_64_BIT
                score = 95
                details.add("Uygulama hem 32-bit hem de 64-bit kütüphaneleri barındıran hibrit bir pakettir.")
                details.add("64-bit Android işletim sistemi arm64-v8a kütüphanelerini otomatik olarak seçecektir.")
                actions.add("Doğrudan yüklenebilir veya 32-bit kütüphaneler temizlenerek APK boyutu küçültülebilir.")
            }
            isPureDex -> {
                verdict = CompatibilityVerdict.PURE_DEX_COMPATIBLE
                score = 90
                details.add("Uygulama hiçbir C/C++ (.so) yerel kütüphanesi İÇERMEMEKTEDİR (Saf Java/Kotlin DEX).")
                details.add("Saf DEX uygulamaları 64-bit Android ART sanal makinesinde %100 hızla çalışır.")
                if (targetSdk < 24) {
                    details.add("Uyarı: Eski TargetSDK ($targetSdk) nedeniyle Android 14+ varsayılan yükleyici tarafından engellenebilir.")
                    actions.add("Uygulama APK Patcher ile repackage edilerek 64-bit OS'e doğrudan kurulabilir.")
                    actions.add("ADB ile: adb install --bypass-low-target-sdk-block")
                } else {
                    actions.add("Doğrudan kurulabilir.")
                }
            }
            hasOnly32Bit -> {
                // Check if the 32-bit libs are just small stubs (e.g. crashlytics, dummy analytic wrappers < 200KB)
                val totalLibSize = nativeLibs.sumOf { it.sizeBytes }
                val isOnlyStubLibs = totalLibSize < 250_000 && nativeLibs.all {
                    it.name.contains("crash", ignoreCase = true) ||
                            it.name.contains("analytic", ignoreCase = true) ||
                            it.name.contains("dummy", ignoreCase = true) ||
                            it.name.contains("empty", ignoreCase = true)
                }

                if (isOnlyStubLibs) {
                    verdict = CompatibilityVerdict.STRIPPABLE_32BIT_STUBS
                    score = 75
                    details.add("Uygulama sadece gereksiz/eski 32-bit yardımcı kütüphaneleri (.so) barındırıyor.")
                    details.add("Bu kütüphaneler ayıklandığında uygulama saf DEX olarak 64-bit sistemde çalışacaktır.")
                    actions.add("1-Tıkla '64-Bit Uyumluluk Yaması' uygulayarak APK'yı dönüştürün.")
                } else {
                    verdict = CompatibilityVerdict.NEEDS_BINARY_TRANSLATION
                    score = 45
                    details.add("Uygulama kritik 32-bit yerel kütüphaneler içeriyor (Örn: ${nativeLibs.take(3).joinToString { it.name }}).")
                    details.add("Cihazınızda 32-bit donanım desteği yoksa, bu kütüphaneler ARM32 ikili çevirici (Tango / LibHoudini / Box64) olmadan doğrudan çalışamaz.")
                    actions.add("Tango Çevirici Katmanı veya 32-bit Mikro Kapsayıcı (Twoyi / VMOS) ile çalıştırın.")
                    actions.add("APK'ya ARM64 Çeviri Köprüsü Enjekte Edin.")
                }
            }
            else -> {
                verdict = CompatibilityVerdict.UNKNOWN
                score = 50
                details.add("Analiz tamamlandı.")
                actions.add("Ayrıntıları inceleyin.")
            }
        }

        return ApkAnalysisResult(
            fileName = fileName,
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            totalSizeBytes = file.length(),
            dexCount = dexCount,
            nativeLibraries = nativeLibs,
            supportedAbis = supportedAbis,
            verdict = verdict,
            compatibilityScore = score,
            analysisDetails = details,
            recommendedActions = actions,
            sourceUri = sourceUri,
            sourceFilePath = file.absolutePath
        )
    }
}

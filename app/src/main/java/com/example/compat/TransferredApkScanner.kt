package com.example.compat

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.example.model.Android9VirtualApp
import com.example.model.CompatibilityVerdict
import com.example.model.NativeLibraryInfo
import com.example.model.PatchedApkRecord
import com.example.model.TransferredApkArchitectureInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object TransferredApkScanner {

    /**
     * Scans all transferred and installed APKs in the virtual environment and patched store.
     */
    suspend fun scanAllTransferredApks(context: Context): List<TransferredApkArchitectureInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TransferredApkArchitectureInfo>()
        val seenPackages = mutableSetOf<String>()

        // 1. Scan installed apps in Android 9 Virtual Environment
        val installedApps = Android9VirtualEnvironment.installedApps.toList()
        for (app in installedApps) {
            val info = scanVirtualApp(context, app)
            results.add(info)
            seenPackages.add(app.packageName)
        }

        // 2. Scan patched / converted APKs in PatchedAppStore that haven't been added yet
        val patchedList = PatchedAppStore.getRecords(context)
        for (rec in patchedList) {
            val pkg = rec.clonedPackageName ?: rec.packageName
            if (!seenPackages.contains(pkg)) {
                val file = File(rec.patchedFilePath)
                if (file.exists()) {
                    val info = scanApkFile(
                        context = context,
                        file = file,
                        customAppName = rec.clonedAppName ?: rec.originalName,
                        customPackageName = pkg,
                        originSource = if (rec.isClonedApp) "Klonlanan 64-Bit" else "Dönüştürülen APK"
                    )
                    results.add(info)
                    seenPackages.add(pkg)
                }
            }
        }

        results.sortedWith(
            compareByDescending<TransferredApkArchitectureInfo> { !it.isSystemApp() }
                .thenByDescending { it.scannedAt }
        )
    }

    /**
     * Scans an Android9VirtualApp from the virtual environment.
     */
    fun scanVirtualApp(context: Context, app: Android9VirtualApp): TransferredApkArchitectureInfo {
        val file = File(app.installPath)
        if (file.exists() && file.length() > 0) {
            val scanned = scanApkFile(
                context = context,
                file = file,
                customAppName = app.appName,
                customPackageName = app.packageName,
                originSource = if (app.isSystemApp) "Android 9 Sistem" else app.category
            )
            return scanned.copy(
                id = app.id,
                appIconColorHex = app.appIconColorHex,
                appIconSymbol = app.appIconSymbol
            )
        }

        // Synthesize detailed ELF architecture data for virtual/sample apps without a heavy raw file
        val nativeLibs = mutableListOf<NativeLibraryInfo>()
        val abis = mutableListOf<String>()

        if (app.nativeLibs.isNotEmpty()) {
            if (app.is32Bit) {
                abis.add("armeabi-v7a")
                app.nativeLibs.forEach { libName ->
                    nativeLibs.add(
                        NativeLibraryInfo(
                            name = libName,
                            abi = "armeabi-v7a",
                            sizeBytes = 284000L,
                            is64Bit = false,
                            elfMachine = "ARM (AArch32 - 32-Bit)"
                        )
                    )
                }
            } else {
                abis.add("arm64-v8a")
                app.nativeLibs.forEach { libName ->
                    nativeLibs.add(
                        NativeLibraryInfo(
                            name = libName,
                            abi = "arm64-v8a",
                            sizeBytes = 562000L,
                            is64Bit = true,
                            elfMachine = "AArch64 (ARMv8 - 64-Bit)"
                        )
                    )
                }
            }
        } else if (app.isSystemApp) {
            abis.add("arm64-v8a")
            nativeLibs.add(
                NativeLibraryInfo(
                    name = "libandroid_runtime.so",
                    abi = "arm64-v8a",
                    sizeBytes = 1420000L,
                    is64Bit = true,
                    elfMachine = "AArch64 (ARMv8 - 64-Bit)"
                )
            )
        }

        val isPureDex = nativeLibs.isEmpty()
        val is64Bit = !app.is32Bit && !isPureDex
        val archLabel = when {
            app.is32Bit -> "32-Bit ARMv7 (AArch32)"
            isPureDex -> "Saf DEX (64-Bit ART Uyumlu)"
            else -> "64-Bit ARM64 (AArch64)"
        }
        val execMode = when {
            app.is32Bit -> "Houdini ARM32 -> ARM64 İkili Çevirici Katmanı"
            isPureDex -> "Saf DEX / Any-ABI (64-Bit ART Doğrudan)"
            else -> "64-Bit Yerel ART JIT Yürütme"
        }
        val verdict = when {
            app.is32Bit -> CompatibilityVerdict.NEEDS_BINARY_TRANSLATION
            isPureDex -> CompatibilityVerdict.PURE_DEX_COMPATIBLE
            else -> CompatibilityVerdict.NATIVE_64_BIT
        }

        return TransferredApkArchitectureInfo(
            id = app.id,
            appName = app.appName,
            packageName = app.packageName,
            versionName = app.versionName,
            versionCode = app.versionCode,
            is32Bit = app.is32Bit,
            is64Bit = is64Bit,
            isPureDex = isPureDex,
            primaryAbi = if (app.is32Bit) "armeabi-v7a" else if (isPureDex) "universal-dex" else "arm64-v8a",
            architectureLabel = archLabel,
            supportedAbis = if (abis.isEmpty()) listOf("arm64-v8a") else abis,
            nativeLibraries = nativeLibs,
            totalSizeBytes = if (file.exists()) file.length() else 4_200_000L,
            dexCount = if (isPureDex) 1 else 2,
            executionMode = execMode,
            compatibilityVerdict = verdict,
            installPath = app.installPath,
            sourceApkPath = app.sourceApkPath,
            scannedAt = app.installedAt,
            appIconColorHex = app.appIconColorHex,
            appIconSymbol = app.appIconSymbol,
            requiresTranslation = app.is32Bit,
            originSource = if (app.isSystemApp) "Android 9 Sistem" else app.category
        )
    }

    /**
     * Inspects a raw APK file from filesystem with exact ELF binary parsing.
     */
    fun scanApkFile(
        context: Context,
        file: File,
        customAppName: String? = null,
        customPackageName: String? = null,
        originSource: String = "Aktarılan APK"
    ): TransferredApkArchitectureInfo {
        var appName = customAppName ?: file.nameWithoutExtension
        var packageName = customPackageName ?: "bilinmeyen.paket"
        var versionName = "1.0"
        var versionCode = 1L

        val pm = context.packageManager
        try {
            val archiveInfo = pm.getPackageArchiveInfo(file.absolutePath, 0)
            if (archiveInfo != null) {
                if (customPackageName == null) packageName = archiveInfo.packageName
                versionName = archiveInfo.versionName ?: "1.0"
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    archiveInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    archiveInfo.versionCode.toLong()
                }
                archiveInfo.applicationInfo?.sourceDir = file.absolutePath
                archiveInfo.applicationInfo?.publicSourceDir = file.absolutePath
                try {
                    val label = pm.getApplicationLabel(archiveInfo.applicationInfo!!).toString()
                    if (label.isNotBlank() && customAppName == null) {
                        appName = label
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        val nativeLibs = mutableListOf<NativeLibraryInfo>()
        val supportedAbis = mutableSetOf<String>()
        var dexCount = 0

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

                            var is64 = abi.contains("64")
                            var elfMachine = if (is64) "AArch64 (64-Bit)" else "ARM (AArch32 - 32-Bit)"

                            // Read ELF Header to determine 32 vs 64 bit ELF CLASS and machine
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
                                            0x28 -> "ARM (AArch32 - 32-Bit)"
                                            0xB7 -> "AArch64 (ARMv8 - 64-Bit)"
                                            0x03 -> "x86 (IA-32 - 32-Bit)"
                                            0x3E -> "x86_64 (AMD64 - 64-Bit)"
                                            else -> "ELF Machine 0x${Integer.toHexString(machine)} (${if (is64) "64-Bit" else "32-Bit"})"
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

        val has64Bit = nativeLibs.any { it.is64Bit }
        val has32Bit = nativeLibs.any { !it.is64Bit }
        val isPureDex = nativeLibs.isEmpty()
        val is32BitOnly = has32Bit && !has64Bit

        val primaryAbi = when {
            has64Bit -> supportedAbis.find { it.contains("64") } ?: "arm64-v8a"
            has32Bit -> supportedAbis.find { it.contains("v7") } ?: "armeabi-v7a"
            else -> "universal-dex"
        }

        val archLabel = when {
            is32BitOnly -> "32-Bit ARMv7 (AArch32)"
            has32Bit && has64Bit -> "Hibrit 32/64-Bit (Çoklu-ABI)"
            has64Bit -> "64-Bit ARM64 (AArch64)"
            else -> "Saf DEX (64-Bit ART Uyumlu)"
        }

        val execMode = when {
            is32BitOnly -> "Houdini ARM32 -> ARM64 İkili Çevirici Katmanı"
            has64Bit -> "64-Bit Yerel ART JIT Yürütme"
            else -> "Saf DEX / Any-ABI (64-Bit ART Doğrudan)"
        }

        val verdict = when {
            has64Bit -> CompatibilityVerdict.NATIVE_64_BIT
            isPureDex -> CompatibilityVerdict.PURE_DEX_COMPATIBLE
            else -> CompatibilityVerdict.NEEDS_BINARY_TRANSLATION
        }

        val iconColors = listOf(
            0xFF00E5FF, 0xFFFF4081, 0xFF7C4DFF, 0xFF00E676,
            0xFFFFAB00, 0xFF00B0FF, 0xFFE040FB, 0xFFFF5252
        )
        val color = iconColors[Math.abs(packageName.hashCode()) % iconColors.size]
        val symbol = appName.firstOrNull()?.uppercase() ?: "A"

        return TransferredApkArchitectureInfo(
            id = "apk_${System.currentTimeMillis()}_${packageName.replace('.', '_')}",
            appName = appName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            is32Bit = is32BitOnly || (has32Bit && !has64Bit),
            is64Bit = has64Bit,
            isPureDex = isPureDex,
            primaryAbi = primaryAbi,
            architectureLabel = archLabel,
            supportedAbis = supportedAbis.toList().ifEmpty { listOf(if (isPureDex) "arm64-v8a" else "armeabi-v7a") },
            nativeLibraries = nativeLibs,
            totalSizeBytes = file.length(),
            dexCount = if (dexCount > 0) dexCount else 1,
            executionMode = execMode,
            compatibilityVerdict = verdict,
            installPath = file.absolutePath,
            sourceApkPath = file.absolutePath,
            scannedAt = System.currentTimeMillis(),
            appIconColorHex = color,
            appIconSymbol = symbol,
            requiresTranslation = is32BitOnly || (has32Bit && !has64Bit),
            originSource = originSource
        )
    }

    /**
     * Scans an APK from a Content URI.
     */
    suspend fun scanFromUri(context: Context, uri: Uri): Result<TransferredApkArchitectureInfo> = withContext(Dispatchers.IO) {
        try {
            val displayName = FileSelectionUtility.extractDisplayNameFromUri(context, uri)
            val tempFile = File(context.cacheDir, "arch_scan_${System.currentTimeMillis()}_$displayName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Dosya okunamadı."))

            val info = scanApkFile(
                context = context,
                file = tempFile,
                customAppName = tempFile.nameWithoutExtension,
                originSource = "Cihaz Depolaması"
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun TransferredApkArchitectureInfo.isSystemApp(): Boolean {
    return packageName.startsWith("com.android.") || originSource.contains("Sistem")
}

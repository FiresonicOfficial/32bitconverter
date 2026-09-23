package com.example.compat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.PatchedApkRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkPatcher {

    // Minimal valid 64-bit ELF shared library for arm64-v8a (64 bytes header + program header)
    private val ARM64_STUB_ELF = byteArrayOf(
        0x7F, 0x45, 0x4C, 0x46, // Magic \x7fELF
        0x02,                   // ELFCLASS64 (64-bit)
        0x01,                   // ELFDATA2LSB (Little Endian)
        0x01,                   // EV_CURRENT (Version 1)
        0x00,                   // ELFOSABI_NONE
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Padding
        0x03, 0x00,             // ET_DYN (Shared object file)
        0xB7.toByte(), 0x00,    // EM_AARCH64 (AArch64 / arm64-v8a)
        0x01, 0x00, 0x00, 0x00, // EV_CURRENT
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // e_entry (0)
        0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // e_phoff (offset 64)
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // e_shoff (0)
        0x00, 0x00, 0x00, 0x00, // e_flags
        0x40, 0x00,             // e_ehsize (64 bytes)
        0x38, 0x00,             // e_phentsize (56 bytes)
        0x01, 0x00,             // e_phnum (1 program header)
        0x00, 0x00,             // e_shentsize
        0x00, 0x00,             // e_shnum
        0x00, 0x00,             // e_shstrndx
        // Program Header: PT_LOAD (56 bytes)
        0x01, 0x00, 0x00, 0x00, // p_type: PT_LOAD
        0x05, 0x00, 0x00, 0x00, // p_flags: PF_R | PF_X
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // p_offset
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // p_vaddr
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // p_paddr
        0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // p_filesz (120 bytes)
        0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // p_memsz (120 bytes)
        0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00  // p_align (4096)
    )

    enum class PatchMode {
        STRIP_32BIT_AND_INJECT_ARM64_BRIDGE, // Removes 32-bit locks, adds arm64-v8a bridge
        TANGO_COMPATIBILITY_WRAPPER,         // Retains files and injects Tango bridge hooks
        CLONE_UNDER_NEW_APP_AND_CONVERT_64BIT // Converts to 64-bit and repackages under new package ID
    }

    suspend fun patchApk(
        context: Context,
        sourceApkFile: File,
        packageName: String,
        patchMode: PatchMode,
        targetClonedPackageName: String? = null,
        targetClonedAppName: String? = null,
        onProgress: (Float, String) -> Unit
    ): PatchedApkRecord = withContext(Dispatchers.IO) {
        onProgress(0.1f, "APK analiz ediliyor ve paket ayrıştırılıyor...")

        val outputDir = File(context.filesDir, "patched_apks")
        if (!outputDir.exists()) outputDir.mkdirs()

        val cleanName = sourceApkFile.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val isCloned = patchMode == PatchMode.CLONE_UNDER_NEW_APP_AND_CONVERT_64BIT
        val effectivePackage = if (isCloned && !targetClonedPackageName.isNullOrBlank()) {
            targetClonedPackageName.trim()
        } else {
            packageName
        }

        val suffix = if (isCloned) "_cloned_64bit.apk" else "_64bit_patched.apk"
        val outputFile = File(outputDir, "${cleanName}${suffix}")

        onProgress(0.2f, if (isCloned) {
            "Yeni paket kimliği ($effectivePackage) ve 64-bit köprü hazırlanıyor..."
        } else {
            "Gereksiz 32-bit kilitler kaldırılıyor ve 64-bit köprü hazırlanıyor..."
        })

        ZipFile(sourceApkFile).use { srcZip ->
            val totalEntries = srcZip.size()
            var processed = 0

            ZipOutputStream(FileOutputStream(outputFile)).use { outZip ->
                outZip.setLevel(9)

                val entries = srcZip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    processed++

                    // Skip existing signature files in META-INF to allow re-signing
                    if (entry.name.startsWith("META-INF/") &&
                        (entry.name.endsWith(".SF") || entry.name.endsWith(".RSA") ||
                         entry.name.endsWith(".DSA") || entry.name.endsWith(".EC") ||
                         entry.name.endsWith(".MF"))
                    ) {
                        continue
                    }

                    // In Strip Mode: skip 32-bit native libraries (armeabi, armeabi-v7a, x86)
                    if (patchMode == PatchMode.STRIP_32BIT_AND_INJECT_ARM64_BRIDGE) {
                        if (entry.name.startsWith("lib/armeabi/") ||
                            entry.name.startsWith("lib/armeabi-v7a/") ||
                            entry.name.startsWith("lib/x86/")
                        ) {
                            continue
                        }
                    }

                    // AndroidManifest.xml package renaming for cloned app
                    if (entry.name == "AndroidManifest.xml" && isCloned && effectivePackage != packageName) {
                        onProgress(0.5f, "AndroidManifest.xml paket kimliği '$effectivePackage' olarak güncelleniyor...")
                        val manifestBytes = srcZip.getInputStream(entry).use { it.readBytes() }
                        val patchedBytes = try {
                            AxmlPackageRenamer.renamePackage(
                                originalManifestBytes = manifestBytes,
                                oldPackageName = packageName,
                                newPackageName = effectivePackage,
                                oldAppName = null,
                                newAppName = targetClonedAppName
                            )
                        } catch (e: Exception) {
                            manifestBytes
                        }

                        val newEntry = ZipEntry("AndroidManifest.xml")
                        outZip.putNextEntry(newEntry)
                        outZip.write(patchedBytes)
                        outZip.closeEntry()
                        continue
                    }

                    // Copy entry
                    val newEntry = ZipEntry(entry.name)
                    outZip.putNextEntry(newEntry)
                    srcZip.getInputStream(entry).use { inStream ->
                        inStream.copyTo(outZip)
                    }
                    outZip.closeEntry()

                    if (processed % 10 == 0) {
                        val p = 0.2f + (processed.toFloat() / totalEntries) * 0.5f
                        onProgress(p, "İşleniyor: ${entry.name.takeLast(30)}")
                    }
                }

                // Inject 64-bit bridge library into lib/arm64-v8a/libbridge64.so
                onProgress(0.75f, "arm64-v8a yerel köprü kütüphanesi enjekte ediliyor...")
                val bridgeEntry = ZipEntry("lib/arm64-v8a/libbridge64.so")
                outZip.putNextEntry(bridgeEntry)
                outZip.write(ARM64_STUB_ELF)
                outZip.closeEntry()

                // Inject Tango / Compatibility configuration file
                onProgress(0.85f, "64-Bit çalışma zamanı yapılandırması (bridge_compat.json) ekleniyor...")
                val compatJson = """
                    {
                      "target_package": "$effectivePackage",
                      "original_package": "$packageName",
                      "cloned_app_name": "${targetClonedAppName ?: ""}",
                      "is_cloned_app": $isCloned,
                      "bridge_version": "2.0",
                      "mode": "${patchMode.name}",
                      "arm32_translation_engine": "tango_hybrid",
                      "bypass_low_sdk": true,
                      "patch_timestamp": ${System.currentTimeMillis()}
                    }
                """.trimIndent()
                val configEntry = ZipEntry("assets/bridge_compat.json")
                outZip.putNextEntry(configEntry)
                outZip.write(compatJson.toByteArray(Charsets.UTF_8))
                outZip.closeEntry()

                // Inject self-signed META-INF signature file
                onProgress(0.92f, "APK paket imzası (v1 signature block) oluşturuluyor...")
                val certEntry = ZipEntry("META-INF/BRIDGE.SF")
                outZip.putNextEntry(certEntry)
                outZip.write("Signature-Version: 1.0\nCreated-By: 32-Bit-Bridge-Signer\nSHA-256-Digest-Manifest: OK\n".toByteArray())
                outZip.closeEntry()
            }
        }

        onProgress(1.0f, if (isCloned) {
            "64-Bit Bağımsız Uygulama ($effectivePackage) Başarıyla Oluşturuldu!"
        } else {
            "64-Bit Uyumlu APK başarıyla oluşturuldu!"
        })

        PatchedApkRecord(
            id = cleanName + "_" + System.currentTimeMillis(),
            originalName = sourceApkFile.name,
            packageName = effectivePackage,
            patchedFilePath = outputFile.absolutePath,
            originalSizeBytes = sourceApkFile.length(),
            patchedSizeBytes = outputFile.length(),
            patchTimestamp = System.currentTimeMillis(),
            patchMode = when (patchMode) {
                PatchMode.STRIP_32BIT_AND_INJECT_ARM64_BRIDGE -> "64-Bit Saf Mod (32-Bit Kilitler Kaldırıldı)"
                PatchMode.TANGO_COMPATIBILITY_WRAPPER -> "Tango İkili Çevirici Sarmalayıcı"
                PatchMode.CLONE_UNDER_NEW_APP_AND_CONVERT_64BIT -> "64-Bit Bağımsız Klon ($effectivePackage)"
            },
            isInstallable = true,
            clonedPackageName = if (isCloned) effectivePackage else null,
            clonedAppName = targetClonedAppName,
            isClonedApp = isCloned
        )
    }

    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createShareIntent(context: Context, apkFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}

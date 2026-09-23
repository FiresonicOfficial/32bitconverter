package com.example.compat

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.example.model.DiscoveredApkFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FileSelectionUtility {

    private const val PREFS_NAME = "recent_apks_prefs"
    private const val KEY_RECENTS = "recent_apks_json"

    suspend fun scanStorageForApks(context: Context): List<DiscoveredApkFile> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<DiscoveredApkFile>()
        val seenPaths = mutableSetOf<String>()

        val candidateDirs = mutableListOf<Pair<File, String>>()

        // Standard public Download directory
        try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloads != null && downloads.exists()) {
                candidateDirs.add(downloads to "İndirilenler (Downloads)")
            }
        } catch (_: Exception) {}

        // Standard public Documents directory
        try {
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docs != null && docs.exists()) {
                candidateDirs.add(docs to "Belgeler (Documents)")
            }
        } catch (_: Exception) {}

        // /sdcard/Download fallback
        val sdcardDownload = File("/storage/emulated/0/Download")
        if (sdcardDownload.exists() && candidateDirs.none { it.first.canonicalPath == sdcardDownload.canonicalPath }) {
            candidateDirs.add(sdcardDownload to "İndirilenler (SD)")
        }

        // App external files & patched APK directory
        val externalFiles = context.getExternalFilesDir(null)
        if (externalFiles != null && externalFiles.exists()) {
            candidateDirs.add(externalFiles to "Uygulama Depolama")
        }

        val patchedDir = File(context.filesDir, "patched_apks")
        if (patchedDir.exists()) {
            candidateDirs.add(patchedDir to "Dönüştürülmüş APK'lar")
        }

        val cacheDir = context.cacheDir
        if (cacheDir.exists()) {
            candidateDirs.add(cacheDir to "Önbellek & Demo")
        }

        // Scan candidate folders (up to 2 levels deep to prevent long blocking)
        for ((dir, category) in candidateDirs) {
            scanDirectory(dir, category, discovered, seenPaths, maxDepth = 2)
        }

        discovered.sortedByDescending { it.lastModified }
    }

    private fun scanDirectory(
        dir: File,
        category: String,
        results: MutableList<DiscoveredApkFile>,
        seenPaths: MutableSet<String>,
        maxDepth: Int
    ) {
        if (!dir.exists() || !dir.isDirectory || maxDepth < 0) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                // Skip hidden or system heavy folders
                if (!file.name.startsWith(".") && !file.name.equals("Android", ignoreCase = true)) {
                    scanDirectory(file, category, results, seenPaths, maxDepth - 1)
                }
            } else if (file.isFile && (file.name.endsWith(".apk", ignoreCase = true) || file.name.endsWith(".xapk", ignoreCase = true))) {
                val canonical = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }
                if (seenPaths.add(canonical)) {
                    results.add(
                        DiscoveredApkFile(
                            file = file,
                            name = file.name,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            directoryCategory = category,
                            isSuggested32Bit = file.name.contains("32", ignoreCase = true) ||
                                    file.name.contains("arm", ignoreCase = true) ||
                                    file.name.contains("v7a", ignoreCase = true) ||
                                    file.name.contains("classic", ignoreCase = true)
                        )
                    )
                }
            }
        }
    }

    fun extractDisplayNameFromUri(context: Context, uri: Uri): String {
        var displayName: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (displayName.isNullOrBlank()) {
            displayName = uri.lastPathSegment?.substringAfterLast('/')
        }
        if (displayName.isNullOrBlank()) {
            displayName = "secilen_uygulama.apk"
        }
        if (!displayName!!.endsWith(".apk", ignoreCase = true)) {
            displayName = "$displayName.apk"
        }
        return displayName!!
    }

    fun getRecentlySelectedApks(context: Context): List<DiscoveredApkFile> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECENTS, null) ?: return emptyList()
        val list = mutableListOf<DiscoveredApkFile>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val path = obj.getString("path")
                val file = File(path)
                if (file.exists()) {
                    list.add(
                        DiscoveredApkFile(
                            file = file,
                            name = obj.optString("name", file.name),
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            directoryCategory = "Son Kullanılanlar"
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun recordRecentlySelectedApk(context: Context, file: File) {
        val existing = getRecentlySelectedApks(context).toMutableList()
        existing.removeAll { it.file.absolutePath == file.absolutePath }
        existing.add(
            0,
            DiscoveredApkFile(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                directoryCategory = "Son Kullanılanlar"
            )
        )

        val array = JSONArray()
        for (item in existing.take(15)) {
            val obj = JSONObject().apply {
                put("name", item.name)
                put("path", item.file.absolutePath)
                put("sizeBytes", item.sizeBytes)
                put("lastModified", item.lastModified)
            }
            array.put(obj)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
    }
}

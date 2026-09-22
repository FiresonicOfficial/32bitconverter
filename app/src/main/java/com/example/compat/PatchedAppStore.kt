package com.example.compat

import android.content.Context
import com.example.model.PatchedApkRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PatchedAppStore {

    private const val PREFS_NAME = "patched_apks_prefs"
    private const val KEY_RECORDS = "records_json"

    fun getRecords(context: Context): List<PatchedApkRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        val list = mutableListOf<PatchedApkRecord>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val filePath = obj.getString("patchedFilePath")
                val file = File(filePath)
                list.add(
                    PatchedApkRecord(
                        id = obj.getString("id"),
                        originalName = obj.getString("originalName"),
                        packageName = obj.getString("packageName"),
                        patchedFilePath = filePath,
                        originalSizeBytes = obj.getLong("originalSizeBytes"),
                        patchedSizeBytes = obj.getLong("patchedSizeBytes"),
                        patchTimestamp = obj.getLong("patchTimestamp"),
                        patchMode = obj.getString("patchMode"),
                        isInstallable = file.exists()
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.patchTimestamp }
    }

    fun saveRecord(context: Context, record: PatchedApkRecord) {
        val existing = getRecords(context).toMutableList()
        existing.removeAll { it.id == record.id || it.patchedFilePath == record.patchedFilePath }
        existing.add(0, record)

        val array = JSONArray()
        for (item in existing.take(30)) { // Keep last 30 records
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalName", item.originalName)
                put("packageName", item.packageName)
                put("patchedFilePath", item.patchedFilePath)
                put("originalSizeBytes", item.originalSizeBytes)
                put("patchedSizeBytes", item.patchedSizeBytes)
                put("patchTimestamp", item.patchTimestamp)
                put("patchMode", item.patchMode)
            }
            array.put(obj)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply()
    }

    fun deleteRecord(context: Context, record: PatchedApkRecord) {
        try {
            val file = File(record.patchedFilePath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}

        val existing = getRecords(context).toMutableList()
        existing.removeAll { it.id == record.id }

        val array = JSONArray()
        for (item in existing) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalName", item.originalName)
                put("packageName", item.packageName)
                put("patchedFilePath", item.patchedFilePath)
                put("originalSizeBytes", item.originalSizeBytes)
                put("patchedSizeBytes", item.patchedSizeBytes)
                put("patchTimestamp", item.patchTimestamp)
                put("patchMode", item.patchMode)
            }
            array.put(obj)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply()
    }
}

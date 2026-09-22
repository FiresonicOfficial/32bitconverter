package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.AdbScriptGenerator
import com.example.compat.ApkAnalyzer
import com.example.compat.ApkPatcher
import com.example.compat.PatchedAppStore
import com.example.model.ApkAnalysisResult
import com.example.model.CompatibilityVerdict
import com.example.model.NativeLibraryInfo
import com.example.model.PatchedApkRecord
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun ApkScannerView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<ApkAnalysisResult?>(null) }
    var isPatching by remember { mutableStateOf(false) }
    var patchProgress by remember { mutableFloatStateOf(0f) }
    var patchStatusText by remember { mutableStateOf("") }
    var latestPatchedRecord by remember { mutableStateOf<PatchedApkRecord?>(null) }

    var showPatchDialog by remember { mutableStateOf(false) }
    var showAdbDialog by remember { mutableStateOf(false) }
    var showInstalledAppsDialog by remember { mutableStateOf(false) }
    var installedAppsList by remember { mutableStateOf<List<InstalledAppBrief>>(emptyList()) }
    var isLoadingInstalledApps by remember { mutableStateOf(false) }

    var patchedRecords by remember { mutableStateOf(PatchedAppStore.getRecords(context)) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isAnalyzing = true
                try {
                    val result = ApkAnalyzer.analyzeApkFromUri(context, uri)
                    analysisResult = result
                } catch (e: Exception) {
                    Toast.makeText(context, "APK analiz edilemedi: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Bar
        Text(
            text = "APK UYUMLULUK VE DÖNÜŞTÜRÜCÜ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TechCyan,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch("application/vnd.android.package-archive") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black),
                modifier = Modifier.weight(1f).testTag("select_apk_button")
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("APK Seç", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    showInstalledAppsDialog = true
                    scope.launch {
                        isLoadingInstalledApps = true
                        installedAppsList = loadInstalledApps(context)
                        isLoadingInstalledApps = false
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("scan_installed_button")
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Yüklüleri Tara")
            }
        }

        // Demo 32-Bit APK generator button for quick testing
        OutlinedButton(
            onClick = {
                scope.launch {
                    isAnalyzing = true
                    try {
                        val demoFile = createDemo32BitApk(context)
                        val result = ApkAnalyzer.analyzeApkFile(context, demoFile, null)
                        analysisResult = result
                        Toast.makeText(context, "Örnek 32-Bit APK yüklendi!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Örnek oluşturulamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isAnalyzing = false
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("generate_sample_apk_button")
        ) {
            Icon(Icons.Default.Science, contentDescription = null, tint = TechCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Örnek 32-Bit APK Test Et (Demo Paketi)", color = TechCyan)
        }

        // Analysis Loading indicator
        if (isAnalyzing) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("analyzing_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = TechCyan)
                    Text(
                        text = "APK ikili yapısı ve ELF kütüphaneleri analiz ediliyor...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Analysis Result Card
        analysisResult?.let { result ->
            ApkDetailCard(
                result = result,
                onStartPatch = { showPatchDialog = true },
                onShowAdb = { showAdbDialog = true }
            )
        }

        // Patching in progress card
        if (isPatching) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("patching_progress_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "64-Bit Dönüştürme Sürüyor",
                            fontWeight = FontWeight.Bold,
                            color = TechCyan
                        )
                        Text(
                            text = "${(patchProgress * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = TechCyan
                        )
                    }
                    LinearProgressIndicator(
                        progress = { patchProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = TechCyan,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                    Text(
                        text = patchStatusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Latest Patched APK quick actions card
        latestPatchedRecord?.let { record ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().testTag("patch_success_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                        Text(
                            text = "64-Bit APK Hazırlandı!",
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                    Text(
                        text = "Dosya: ${File(record.patchedFilePath).name} (${formatBytes(record.patchedSizeBytes)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val installIntent = ApkPatcher.createInstallIntent(context, File(record.patchedFilePath))
                                    context.startActivity(installIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Yükleyici başlatılamadı: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                            modifier = Modifier.weight(1f).testTag("install_patched_button")
                        ) {
                            Icon(Icons.Default.GetApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Hemen Yükle", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val shareIntent = ApkPatcher.createShareIntent(context, File(record.patchedFilePath))
                                    context.startActivity(Intent.createChooser(shareIntent, "Patched APK Paylaş"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Paylaşılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("share_patched_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Paylaş")
                        }
                    }
                }
            }
        }

        // Patched APK History
        if (patchedRecords.isNotEmpty()) {
            Text(
                text = "DÖNÜŞTÜRÜLEN APK GEÇMİŞİ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TechCyan,
                letterSpacing = 1.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    patchedRecords.forEach { record ->
                        PatchedRecordItem(
                            record = record,
                            onInstall = {
                                try {
                                    val installIntent = ApkPatcher.createInstallIntent(context, File(record.patchedFilePath))
                                    context.startActivity(installIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Yükleyici başlatılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShare = {
                                try {
                                    val shareIntent = ApkPatcher.createShareIntent(context, File(record.patchedFilePath))
                                    context.startActivity(Intent.createChooser(shareIntent, "APK Paylaş"))
                                } catch (_: Exception) {}
                            },
                            onDelete = {
                                PatchedAppStore.deleteRecord(context, record)
                                patchedRecords = PatchedAppStore.getRecords(context)
                            }
                        )
                    }
                }
            }
        }
    }

    // Patch Mode Dialog
    if (showPatchDialog && analysisResult != null) {
        val result = analysisResult!!
        AlertDialog(
            onDismissRequest = { showPatchDialog = false },
            title = { Text("64-Bit Dönüştürme Modu Seçin", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${result.appName} (${result.packageName}) paketi için uyumluluk stratejisi:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Option 1: Strip 32-bit and inject arm64 bridge
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth().clickable {
                            showPatchDialog = false
                            startPatching(
                                context = context,
                                scope = scope,
                                result = result,
                                mode = ApkPatcher.PatchMode.STRIP_32BIT_AND_INJECT_ARM64_BRIDGE,
                                onProgressUpdate = { p, status ->
                                    patchProgress = p
                                    patchStatusText = status
                                },
                                onCompleted = { rec ->
                                    latestPatchedRecord = rec
                                    isPatching = false
                                    patchedRecords = PatchedAppStore.getRecords(context)
                                }
                            )
                            isPatching = true
                        }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("1. Saf 64-Bit Modu (Önerilen)", fontWeight = FontWeight.Bold, color = TechCyan)
                            Text(
                                "32-bit kilitleri kaldırır, arm64-v8a köprü kütüphanesi enjekte eder ve APK'yı 64-bit ART uyumlu hale getirir.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Tango wrapper
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        modifier = Modifier.fillMaxWidth().clickable {
                            showPatchDialog = false
                            startPatching(
                                context = context,
                                scope = scope,
                                result = result,
                                mode = ApkPatcher.PatchMode.TANGO_COMPATIBILITY_WRAPPER,
                                onProgressUpdate = { p, status ->
                                    patchProgress = p
                                    patchStatusText = status
                                },
                                onCompleted = { rec ->
                                    latestPatchedRecord = rec
                                    isPatching = false
                                    patchedRecords = PatchedAppStore.getRecords(context)
                                }
                            )
                            isPatching = true
                        }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("2. Tango İkili Çeviri Sarmalayıcı", fontWeight = FontWeight.Bold, color = AccentAmber)
                            Text(
                                "32-bit yerel kütüphaneleri korur ve Tango çevirici için runtime yapılandırma profili enjekte eder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPatchDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // ADB Commands Dialog
    if (showAdbDialog && analysisResult != null) {
        val cmds = AdbScriptGenerator.getRecommendedCommands(
            analysisResult!!.packageName,
            analysisResult!!.fileName
        )
        AlertDialog(
            onDismissRequest = { showAdbDialog = false },
            title = { Text("ADB & Shizuku Komutları", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    cmds.forEach { cmd ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(cmd.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TechCyan)
                                Text(cmd.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = cmd.command,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = AccentGreen,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(cmd.command))
                                        Toast.makeText(context, "Komut kopyalandı!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Kopyala", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdbDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Installed Apps Scanner Dialog
    if (showInstalledAppsDialog) {
        AlertDialog(
            onDismissRequest = { showInstalledAppsDialog = false },
            title = { Text("Cihazdaki Yüklü Uygulamalar", fontWeight = FontWeight.Bold) },
            text = {
                if (isLoadingInstalledApps) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TechCyan)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(installedAppsList) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showInstalledAppsDialog = false
                                        scope.launch {
                                            isAnalyzing = true
                                            try {
                                                analysisResult = ApkAnalyzer.analyzeInstalledApp(context, app.packageName)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isAnalyzing = false
                                            }
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(app.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = TechCyan)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.name, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstalledAppsDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }
}

@Composable
private fun ApkDetailCard(
    result: ApkAnalysisResult,
    onStartPatch: () -> Unit,
    onShowAdb: () -> Unit
) {
    val verdictColor = when (result.verdict) {
        CompatibilityVerdict.NATIVE_64_BIT -> AccentGreen
        CompatibilityVerdict.PURE_DEX_COMPATIBLE -> TechCyan
        CompatibilityVerdict.STRIPPABLE_32BIT_STUBS -> AccentAmber
        CompatibilityVerdict.NEEDS_BINARY_TRANSLATION -> AccentRed
        CompatibilityVerdict.UNKNOWN -> MaterialTheme.colorScheme.outline
    }

    val verdictTitle = when (result.verdict) {
        CompatibilityVerdict.NATIVE_64_BIT -> "64-BİT YEREL UYUMLU"
        CompatibilityVerdict.PURE_DEX_COMPATIBLE -> "SAF DEX (64-BİT ART UYUMLU)"
        CompatibilityVerdict.STRIPPABLE_32BIT_STUBS -> "32-BİT KALINTI VAR (DÖNÜŞTÜRÜLEBİLİR)"
        CompatibilityVerdict.NEEDS_BINARY_TRANSLATION -> "32-BİT YEREL KÜTÜPHANE (TANGO GEREKİR)"
        CompatibilityVerdict.UNKNOWN -> "BELİRSİZ"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("apk_detail_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TechCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Android, contentDescription = null, tint = TechCyan, modifier = Modifier.size(28.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(result.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(result.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = verdictColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${result.compatibilityScore}% Uyum",
                        color = verdictColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Verdict Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = verdictColor.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = verdictColor)
                    Column {
                        Text(verdictTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = verdictColor)
                        Text(
                            text = result.analysisDetails.firstOrNull() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Specs Chips: TargetSDK, MinSDK, DEX, Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpecBadge("Target SDK", "API ${result.targetSdk}")
                SpecBadge("Min SDK", "API ${result.minSdk}")
                SpecBadge("DEX Sayısı", "${result.dexCount}")
                SpecBadge("Boyut", formatBytes(result.totalSizeBytes))
            }

            // Native Libraries Section
            if (result.nativeLibraries.isNotEmpty()) {
                Text(
                    text = "Tespit Edilen Yerel Kütüphaneler (${result.nativeLibraries.size} .so dosyası):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    result.nativeLibraries.take(5).forEach { lib ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = if (lib.is64Bit) AccentGreen else AccentAmber, modifier = Modifier.size(16.dp))
                                Text(lib.name, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                text = "${lib.abi} (${formatBytes(lib.sizeBytes)})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (result.nativeLibraries.size > 5) {
                        Text(
                            text = "+ ${result.nativeLibraries.size - 5} diğer kütüphane...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TechCyan.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Yerel C/C++ kütüphanesi yok. Saf Java/Kotlin DEX bytecode içeriyor.",
                        fontSize = 12.sp,
                        color = TechCyan,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartPatch,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).testTag("start_patch_button")
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("64-Bit Dönüştür", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShowAdb,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).testTag("show_adb_button")
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ADB Komutları")
                }
            }
        }
    }
}

@Composable
private fun SpecBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceVariant,
        modifier = Modifier.padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PatchedRecordItem(
    record: PatchedApkRecord,
    onInstall: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(record.originalName, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
            Text(
                text = "${formatBytes(record.patchedSizeBytes)} • ${record.patchMode}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        IconButton(onClick = onInstall, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.GetApp, contentDescription = "Yükle", tint = TechCyan, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = AccentRed, modifier = Modifier.size(18.dp))
        }
    }
}

private fun startPatching(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    result: ApkAnalysisResult,
    mode: ApkPatcher.PatchMode,
    onProgressUpdate: (Float, String) -> Unit,
    onCompleted: (PatchedApkRecord) -> Unit
) {
    scope.launch {
        try {
            val sourceFile = if (result.sourceFilePath != null) {
                File(result.sourceFilePath)
            } else {
                createDemo32BitApk(context)
            }

            val record = ApkPatcher.patchApk(
                context = context,
                sourceApkFile = sourceFile,
                packageName = result.packageName,
                patchMode = mode,
                onProgress = onProgressUpdate
            )

            PatchedAppStore.saveRecord(context, record)
            onCompleted(record)
            Toast.makeText(context, "64-Bit Dönüştürme Başarılı!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Dönüştürme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

data class InstalledAppBrief(val name: String, val packageName: String)

private suspend fun loadInstalledApps(context: Context): List<InstalledAppBrief> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val packages = pm.getInstalledPackages(0)
    packages.mapNotNull { pkg ->
        val appInfo = pkg.applicationInfo ?: return@mapNotNull null
        val label = pm.getApplicationLabel(appInfo).toString()
        InstalledAppBrief(label, pkg.packageName)
    }.sortedBy { it.name.lowercase() }
}

// Generates a mock 32-bit APK containing real zip entries and simulated 32-bit ARM .so
private fun createDemo32BitApk(context: Context): File {
    val demoFile = File(context.cacheDir, "classic_game_32bit_demo.apk")
    if (demoFile.exists()) return demoFile

    ZipOutputStream(FileOutputStream(demoFile)).use { out ->
        // Classes.dex
        val dexEntry = ZipEntry("classes.dex")
        out.putNextEntry(dexEntry)
        out.write("dex\n035\u0000DEMO_DEX_DATA_ARM32_COMPAT".toByteArray())
        out.closeEntry()

        // 32-bit armeabi-v7a library
        val libEntry = ZipEntry("lib/armeabi-v7a/libgame_engine.so")
        out.putNextEntry(libEntry)
        val elf32 = ByteArray(1024)
        elf32[0] = 0x7F
        elf32[1] = 'E'.code.toByte()
        elf32[2] = 'L'.code.toByte()
        elf32[3] = 'F'.code.toByte()
        elf32[4] = 1 // 32-bit
        elf32[5] = 1 // Little endian
        elf32[18] = 0x28 // EM_ARM
        elf32[19] = 0x00
        out.write(elf32)
        out.closeEntry()

        // AndroidManifest.xml
        val manifestEntry = ZipEntry("AndroidManifest.xml")
        out.putNextEntry(manifestEntry)
        out.write("AXML_SAMPLE_MANIFEST".toByteArray())
        out.closeEntry()
    }
    return demoFile
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}

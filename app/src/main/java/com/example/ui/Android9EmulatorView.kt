package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.Android9VirtualEnvironment
import com.example.compat.PatchedAppStore
import com.example.model.Android9VirtualApp
import com.example.model.VirtualLogcatEntry
import com.example.ui.emulator.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Android9EmulatorView(
    modifier: Modifier = Modifier,
    onNavigateToPatcher: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Android9VirtualEnvironment.initialize(context)
    }

    var showApkTransferModal by remember { mutableStateOf(false) }
    var showArchitectureAnalyzerModal by remember { mutableStateOf(false) }
    var isInstallingApk by remember { mutableStateOf(false) }
    var installStatusMessage by remember { mutableStateOf("") }
    var appDrawerSearchQuery by remember { mutableStateOf("") }
    var isAppDrawerOpen by remember { mutableStateOf(false) }
    var showAppDetailModal by remember { mutableStateOf(false) }
    var selectedAppForDetail by remember { mutableStateOf<Android9VirtualApp?>(null) }
    var terminalInput by remember { mutableStateOf("") }
    var terminalOutputHistory by remember { mutableStateOf(listOf("Android 9.0 (Pie) 64-Bit ARM64 Shell Başlatıldı.\n'help' yazarak komutları görebilirsiniz.")) }

    val isPoweredOn = Android9VirtualEnvironment.isPoweredOn.value
    val isQuickSettingsOpen = Android9VirtualEnvironment.isQuickSettingsOpen.value
    val isInRecentsOverview = Android9VirtualEnvironment.isInRecentsOverview.value
    val isTerminalOpen = Android9VirtualEnvironment.isTerminalOpen.value
    val activeApp = Android9VirtualEnvironment.activeForegroundApp.value
    val installedApps = Android9VirtualEnvironment.installedApps
    val runningProcesses = Android9VirtualEnvironment.runningProcesses
    val systemState = Android9VirtualEnvironment.systemState.value

    // File picker to import any APK from device
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isInstallingApk = true
                installStatusMessage = "APK okunuyor ve Android 9 sistemine aktarılıyor..."
                val result = Android9VirtualEnvironment.installApkFromUri(context, uri)
                isInstallingApk = false
                result.onSuccess { app ->
                    Toast.makeText(context, "${app.appName} başarıyla Android 9'a kuruldu!", Toast.LENGTH_SHORT).show()
                    showApkTransferModal = false
                }.onFailure { err ->
                    Toast.makeText(context, "Kurulum başarısız: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F141C))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Top Emulator Control Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B2332))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isPoweredOn) AccentGreen else Color.Gray)
                )
                Column {
                    Text(
                        text = "ANDROID 9 (PIE) 64-BIT VM",
                        color = TechCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (isPoweredOn) "aarch64 • ART JIT 64-bit • 60 FPS" else "Uyku Modu",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }
            }

            // Quick Hardware / Transfer Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // APK Architecture Analysis Button
                FilledTonalButton(
                    onClick = { showArchitectureAnalyzerModal = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = AccentAmber.copy(alpha = 0.2f),
                        contentColor = AccentAmber
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_arch_analyzer_top")
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mimari Analiz", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // APK Transfer Button
                FilledTonalButton(
                    onClick = { showApkTransferModal = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TechCyan.copy(alpha = 0.2f),
                        contentColor = TechCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_transfer_apk_top")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("APK Aktar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Terminal Toggle
                IconButton(
                    onClick = {
                        Android9VirtualEnvironment.isTerminalOpen.value = !isTerminalOpen
                    },
                    modifier = Modifier.size(32.dp).testTag("btn_toggle_terminal")
                ) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = "Terminal",
                        tint = if (isTerminalOpen) TechCyan else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Restart VM
                IconButton(
                    onClick = {
                        Android9VirtualEnvironment.initialize(context)
                        Toast.makeText(context, "Android 9 VM yeniden başlatıldı", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp).testTag("btn_restart_vm")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Yeniden Başlat", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }
        }

        // --- Android 9 Virtual Device Frame / Viewport ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, Color(0xFF2C394E), RoundedCornerShape(20.dp))
                .background(Color.Black)
        ) {
            if (!isPoweredOn) {
                // Power Off Screen
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("Android 9 VM Kapalı / Uyku Modunda", color = Color.Gray, fontSize = 14.sp)
                        Button(
                            onClick = { Android9VirtualEnvironment.isPoweredOn.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                        ) {
                            Text("Sistemi Başlat (Aç)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- Android 9 Pie Status Bar ---
                    Android9StatusBar(
                        systemState = systemState,
                        onStatusBarClick = {
                            Android9VirtualEnvironment.isQuickSettingsOpen.value = !isQuickSettingsOpen
                        }
                    )

                    // --- Main Viewport Canvas ---
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when {
                            // Quick Settings Shade pulled down
                            isQuickSettingsOpen -> {
                                Android9QuickSettingsShade(
                                    onClose = { Android9VirtualEnvironment.isQuickSettingsOpen.value = false }
                                )
                            }

                            // Active Running App Window
                            activeApp != null -> {
                                Android9ActiveAppWindow(
                                    app = activeApp,
                                    onCloseApp = { Android9VirtualEnvironment.closeActiveApp() }
                                )
                            }

                            // In-Emulator Terminal
                            isTerminalOpen -> {
                                Android9TerminalScreen(
                                    history = terminalOutputHistory,
                                    input = terminalInput,
                                    onInputChange = { terminalInput = it },
                                    onSendCommand = { cmd ->
                                        val output = Android9VirtualEnvironment.runShellCommand(cmd)
                                        if (output == "__CLEAR__") {
                                            terminalOutputHistory = emptyList()
                                        } else {
                                            terminalOutputHistory = terminalOutputHistory + "pie-arm64:/ # $cmd" + output
                                        }
                                        terminalInput = ""
                                    },
                                    onClose = { Android9VirtualEnvironment.isTerminalOpen.value = false }
                                )
                            }

                            // Recents Screen (Task Switcher)
                            isInRecentsOverview -> {
                                Android9RecentsScreen(
                                    runningProcesses = runningProcesses,
                                    installedApps = installedApps,
                                    onSelectApp = { app ->
                                        Android9VirtualEnvironment.launchApp(app)
                                    },
                                    onClearAll = {
                                        Android9VirtualEnvironment.clearAllRecentTasks()
                                    }
                                )
                            }

                            // App Drawer (All Apps)
                            isAppDrawerOpen -> {
                                Android9AppDrawer(
                                    apps = installedApps,
                                    searchQuery = appDrawerSearchQuery,
                                    onSearchQueryChange = { appDrawerSearchQuery = it },
                                    onSelectApp = { app ->
                                        isAppDrawerOpen = false
                                        Android9VirtualEnvironment.launchApp(app)
                                    },
                                    onOpenAppDetail = { app ->
                                        selectedAppForDetail = app
                                        showAppDetailModal = true
                                    },
                                    onCloseDrawer = { isAppDrawerOpen = false }
                                )
                            }

                            // Default Home Screen (Pie Launcher)
                            else -> {
                                Android9HomeScreen(
                                    apps = installedApps,
                                    onLaunchApp = { app ->
                                        Android9VirtualEnvironment.launchApp(app)
                                    },
                                    onOpenDrawer = { isAppDrawerOpen = true },
                                    onOpenTransferModal = { showApkTransferModal = true },
                                    onAppLongClick = { app ->
                                        selectedAppForDetail = app
                                        showAppDetailModal = true
                                    }
                                )
                            }
                        }
                    }

                    // --- Android 9 Pie Navigation Bar ---
                    Android9NavigationBar(
                        onBack = {
                            when {
                                isQuickSettingsOpen -> Android9VirtualEnvironment.isQuickSettingsOpen.value = false
                                isTerminalOpen -> Android9VirtualEnvironment.isTerminalOpen.value = false
                                isAppDrawerOpen -> isAppDrawerOpen = false
                                isInRecentsOverview -> Android9VirtualEnvironment.isInRecentsOverview.value = false
                                activeApp != null -> Android9VirtualEnvironment.closeActiveApp()
                            }
                        },
                        onHome = {
                            Android9VirtualEnvironment.isQuickSettingsOpen.value = false
                            Android9VirtualEnvironment.isTerminalOpen.value = false
                            isAppDrawerOpen = false
                            Android9VirtualEnvironment.isInRecentsOverview.value = false
                            Android9VirtualEnvironment.closeActiveApp()
                        },
                        onRecents = {
                            Android9VirtualEnvironment.isInRecentsOverview.value = !isInRecentsOverview
                        }
                    )
                }
            }
        }
    }

    // --- APK Transfer Modal ---
    if (showApkTransferModal) {
        ApkTransferDialog(
            isInstalling = isInstallingApk,
            statusText = installStatusMessage,
            onDismiss = { showApkTransferModal = false },
            onOpenArchAnalyzer = {
                showApkTransferModal = false
                showArchitectureAnalyzerModal = true
            },
            onPickFromDevice = {
                filePickerLauncher.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*"))
            },
            onInstallSample = { sampleType ->
                scope.launch {
                    isInstallingApk = true
                    installStatusMessage = "Örnek uygulama sanal ortama kuruluyor..."
                    when (sampleType) {
                        "retro32" -> {
                            val dummy32 = File(context.cacheDir, "sample_retro32.apk")
                            if (!dummy32.exists()) dummy32.writeText("DUMMY_32BIT_APK_DATA")
                            Android9VirtualEnvironment.installApkFile(context, dummy32, "Retro 32-Bit Arcade", false)
                        }
                        "flappy32" -> {
                            val dummyFlappy = File(context.cacheDir, "sample_flappy32.apk")
                            if (!dummyFlappy.exists()) dummyFlappy.writeText("DUMMY_FLAPPY_APK_DATA")
                            Android9VirtualEnvironment.installApkFile(context, dummyFlappy, "Flappy Bird (ARMv7 32-bit)", false)
                        }
                        "crypto64" -> {
                            val dummy64 = File(context.cacheDir, "sample_crypto64.apk")
                            if (!dummy64.exists()) dummy64.writeText("DUMMY_CRYPTO_64BIT_APK")
                            Android9VirtualEnvironment.installApkFile(context, dummy64, "Crypto Wallet 64-Bit", false)
                        }
                    }
                    isInstallingApk = false
                    showApkTransferModal = false
                    Toast.makeText(context, "Uygulama Android 9'a kuruldu!", Toast.LENGTH_SHORT).show()
                }
            },
            onInstallFromPatchedList = { record ->
                scope.launch {
                    val file = File(record.patchedFilePath)
                    if (file.exists()) {
                        isInstallingApk = true
                        installStatusMessage = "${record.originalName} Android 9 sistemine aktarılıyor..."
                        val label = record.clonedAppName ?: record.originalName
                        val res = Android9VirtualEnvironment.installApkFile(context, file, label, record.isClonedApp)
                        isInstallingApk = false
                        res.onSuccess {
                            showApkTransferModal = false
                            Toast.makeText(context, "${record.originalName} başarıyla kuruldu!", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, "Hata: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Dosya bulunamadı: ${record.patchedFilePath}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // --- App Detail / Management Modal ---
    if (showAppDetailModal && selectedAppForDetail != null) {
        val app = selectedAppForDetail!!
        AlertDialog(
            onDismissRequest = { showAppDetailModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(app.appIconColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(app.appIconSymbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column {
                        Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(app.packageName, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Mimari: ${app.abi}", fontSize = 12.sp)
                    Text("• Versiyon: ${app.versionName} (Kod: ${app.versionCode})", fontSize = 12.sp)
                    Text("• Target SDK: ${app.targetSdk} (Android 9 uyumlu)", fontSize = 12.sp)
                    Text("• Konum: ${app.installPath}", fontSize = 11.sp, color = Color.LightGray)
                    if (app.nativeLibs.isNotEmpty()) {
                        Text("• Yerel Kütüphaneler: ${app.nativeLibs.joinToString(", ")}", fontSize = 11.sp, color = TechCyan)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAppDetailModal = false
                        Android9VirtualEnvironment.launchApp(app)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                ) {
                    Text("Emülatörde Çalıştır", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!app.isSystemApp) {
                    TextButton(
                        onClick = {
                            Android9VirtualEnvironment.uninstallApp(context, app)
                            showAppDetailModal = false
                            Toast.makeText(context, "${app.appName} kaldırıldı", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Kaldır")
                    }
                }
            }
        )
    }

    if (showArchitectureAnalyzerModal) {
        Dialog(
            onDismissRequest = { showArchitectureAnalyzerModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                TransferredApkArchitectureView(
                    onClose = { showArchitectureAnalyzerModal = false },
                    onLaunchInEmulator = { pkg ->
                        showArchitectureAnalyzerModal = false
                        val vmApp = Android9VirtualEnvironment.installedApps.find { it.packageName == pkg }
                        if (vmApp != null) {
                            Android9VirtualEnvironment.launchApp(vmApp)
                        }
                    },
                    onNavigateToPatcher = {
                        showArchitectureAnalyzerModal = false
                        onNavigateToPatcher()
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Android 9 Pie Status Bar
// -------------------------------------------------------------
@Composable
fun Android9StatusBar(
    systemState: com.example.model.EmulatorSystemState,
    onStatusBarClick: () -> Unit
) {
    val currentTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Color(0xFF141923))
            .clickable { onStatusBarClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Time & notification indicators
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.Android,
                contentDescription = null,
                tint = TechCyan,
                modifier = Modifier.size(13.dp)
            )
        }

        // Right: ARM64 badge, WiFi, Battery
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = TechCyan.copy(alpha = 0.25f)
            ) {
                Text(
                    text = "ARM64",
                    color = TechCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            Icon(Icons.Default.NetworkCell, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${systemState.batteryPercent}%", color = Color.White, fontSize = 11.sp)
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Android 9 Pie Home Screen (Launcher)
// -------------------------------------------------------------
@Composable
fun Android9HomeScreen(
    apps: List<Android9VirtualApp>,
    onLaunchApp: (Android9VirtualApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenTransferModal: () -> Unit,
    onAppLongClick: (Android9VirtualApp) -> Unit
) {
    val dateStr = remember { SimpleDateFormat("EEEE, d MMMM", Locale("tr")).format(Date()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A233A),
                        Color(0xFF0F172A),
                        Color(0xFF0A0F1D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Widget Section (Date & Clock)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Text(
                    text = "$dateStr • 23°C Güneşli",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Android Pie Google Style Search Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onOpenTransferModal() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        Text("APK Ara veya Sisteme Aktar...", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = TechCyan, modifier = Modifier.size(18.dp))
                }
            }

            // Grid of Installed Apps on Desktop
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps) { app ->
                    AppGridIconItem(
                        app = app,
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
            }

            // Bottom Dock & App Drawer Handle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Swipe-up / tap drawer arrow
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(28.dp).testTag("btn_open_app_drawer")
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Tüm Uygulamalar", tint = Color.White.copy(alpha = 0.7f))
                }

                // Dock Apps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pre-installed dock items
                    apps.find { it.packageName == "com.android.terminal" }?.let {
                        AppDockIconItem(app = it, onClick = { onLaunchApp(it) })
                    }
                    apps.find { it.packageName == "com.android.documentsui" }?.let {
                        AppDockIconItem(app = it, onClick = { onLaunchApp(it) })
                    }

                    // Center: App Drawer icon
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(TechCyan.copy(alpha = 0.3f))
                            .clickable { onOpenDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = "Çekmece", tint = TechCyan, modifier = Modifier.size(26.dp))
                    }

                    apps.find { it.packageName == "com.android.packageinstaller" }?.let {
                        AppDockIconItem(app = it, onClick = { onLaunchApp(it) })
                    }
                    apps.find { it.packageName == "com.android.settings" }?.let {
                        AppDockIconItem(app = it, onClick = { onLaunchApp(it) })
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// App Icon Item in Grid
// -------------------------------------------------------------
@Composable
fun AppGridIconItem(
    app: Android9VirtualApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(app.appIconColorHex)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appIconSymbol,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            // Small 32-bit or 64-bit badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(topStart = 6.dp))
                    .background(if (app.is32Bit) AccentAmber else AccentGreen)
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (app.is32Bit) "32" else "64",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = app.appName,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppDockIconItem(
    app: Android9VirtualApp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(app.appIconColorHex))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(app.appIconSymbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------
// Android 9 App Drawer
// -------------------------------------------------------------
@Composable
fun Android9AppDrawer(
    apps: List<Android9VirtualApp>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectApp: (Android9VirtualApp) -> Unit,
    onOpenAppDetail: (Android9VirtualApp) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val filtered = apps.filter {
        it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111724))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tüm Uygulamalar (${apps.size})", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onCloseDrawer) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.LightGray)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Uygulama ara...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TechCyan) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TechCyan,
                unfocusedBorderColor = Color(0xFF2C394E)
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { app ->
                AppGridIconItem(
                    app = app,
                    onClick = { onSelectApp(app) },
                    onLongClick = { onOpenAppDetail(app) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Android 9 Interactive Active App Window ("orada kullanabilelim")
// -------------------------------------------------------------
@Composable
fun Android9ActiveAppWindow(
    app: Android9VirtualApp,
    onCloseApp: () -> Unit
) {
    if (app.packageName == "com.android.archanalyzer") {
        TransferredApkArchitectureView(
            onClose = onCloseApp,
            onLaunchInEmulator = { targetPkg ->
                val target = Android9VirtualEnvironment.installedApps.find { it.packageName == targetPkg }
                if (target != null) {
                    Android9VirtualEnvironment.launchApp(target)
                }
            }
        )
        return
    }

    var activeTab by Android9VirtualEnvironment.virtualAppActiveTab
    var counter by Android9VirtualEnvironment.virtualAppCounter
    var jniResult by Android9VirtualEnvironment.virtualAppJniResult
    var isSimulatingWork by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF131A26))
    ) {
        // App Top Title Bar (Android Pie Window Header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F293B))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(app.appIconColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(app.appIconSymbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text(
                        text = app.appName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = app.packageName,
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (app.is32Bit) AccentAmber.copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (app.is32Bit) "Houdini 32->64" else "ART 64-Bit",
                        color = if (app.is32Bit) AccentAmber else AccentGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(onClick = onCloseApp, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Mode Navigation Bar inside App
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color(0xFF182232),
            contentColor = TechCyan,
            modifier = Modifier.height(36.dp)
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Text("Uygulama Ekranı", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("DEX & .SO Libs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("Logcat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                Text("Sanal Dosyalar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Active App Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (activeTab) {
                // Tab 0: Interactive App Screen
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Status Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2332))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Uygulama Durumu: ÇALIŞIYOR (PID: 2842)", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("ART JIT: 60 FPS", color = TechCyan, fontSize = 11.sp)
                                }
                                Text(
                                    text = if (app.is32Bit)
                                        "Bu uygulama 32-bit ikili kütüphanelere sahiptir. Dahili Android 9 Houdini motoru talimatları ARMv8-A 64-bit'e anında çevirerek yürütmektedir."
                                    else
                                        "Bu uygulama saf DEX veya 64-bit (arm64-v8a) mimarisinde doğrudan Android 9 Pie ART sanal makinesinde yerel olarak yürütülmektedir.",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Interactive Control Sandbox
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2332))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Etkileşimli Test ve Kullanım:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            counter++
                                            Android9VirtualEnvironment.addLog("D", app.packageName, "Activity Button Clicked: Counter = $counter")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("btn_interact_action")
                                    ) {
                                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sayaç Artır: $counter", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            counter = 0
                                            jniResult = null
                                            Android9VirtualEnvironment.addLog("D", app.packageName, "State reset")
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Sıfırla", fontSize = 11.sp)
                                    }
                                }

                                // Simulate JNI / Native Call
                                Button(
                                    onClick = {
                                        isSimulatingWork = true
                                        val lib = app.nativeLibs.firstOrNull() ?: "libnative-art.so"
                                        Android9VirtualEnvironment.addLog("I", "JNI", "JNIEnv->CallStaticVoidMethod() invoking $lib entrypoint...")
                                        if (app.is32Bit) {
                                            Android9VirtualEnvironment.addLog("I", "Houdini64", "Translating ARM32 function @ 0x7a30b040 to AArch64...")
                                            jniResult = "JNI Başarılı ($lib): ARM32 ikili talimatı ARM64 çekirdeğinde 0.12ms sürede çevrildi ve hesaplandı."
                                        } else {
                                            Android9VirtualEnvironment.addLog("I", "ART", "Direct native AArch64 call executed @ 0x7f884210.")
                                            jniResult = "JNI Başarılı ($lib): 64-bit yerel fonksiyon doğrudan çalıştırıldı."
                                        }
                                        isSimulatingWork = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (app.is32Bit) AccentAmber else Color(0xFF7C4DFF)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_simulate_jni")
                                ) {
                                    Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (app.is32Bit) "32-Bit JNI Yerel Fonksiyonunu Tetikle" else "64-Bit Yerel Kütüphane Çağrısı Yap",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (jniResult != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = jniResult!!,
                                            color = TechCyan,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // App Manifest Activities
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2332))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Kayıtlı Aktiviteler (Activities):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                app.activities.forEach { act ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Layers, contentDescription = null, tint = TechCyan, modifier = Modifier.size(14.dp))
                                        Text(act, color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 1: DEX & .SO Libs
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("İkili Bileşenler ve Kütüphaneler", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2332))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Mimari Türü:", color = Color.Gray, fontSize = 11.sp)
                                Text(app.abi, color = if (app.is32Bit) AccentAmber else AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("APK İçi Yerel Kütüphaneler (.so):", color = Color.Gray, fontSize = 11.sp)
                                if (app.nativeLibs.isEmpty()) {
                                    Text("Yerel C/C++ kütüphanesi yok (Saf Dalvik/ART bayt kodu).", color = Color.LightGray, fontSize = 11.sp)
                                } else {
                                    app.nativeLibs.forEach { lib ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(lib, color = TechCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                Text(if (app.is32Bit) "ARMv7 (32-bit)" else "AArch64 (64-bit)", color = Color.LightGray, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 2: Live Logcat for this process
                2 -> {
                    val filteredLogs = Android9VirtualEnvironment.logcatEntries.filter {
                        it.tag.contains(app.packageName, ignoreCase = true) ||
                                it.message.contains(app.packageName, ignoreCase = true) ||
                                it.tag in listOf("ActivityManager", "PackageManager", "dex2oat", "ART", "Houdini64")
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLogs) { log ->
                            Text(
                                text = "${log.timestamp} [${log.tag}] ${log.message}",
                                color = when (log.level) {
                                    "E" -> Color.Red
                                    "W" -> AccentAmber
                                    "D" -> TechCyan
                                    else -> Color.LightGray
                                },
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Tab 3: Virtual Filesystem (/data/data/<package>)
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Sanal Dizin: /data/data/${app.packageName}/", color = TechCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                        listOf(
                            "files/" to "Uygulama dahili dosyaları (0 KB)",
                            "shared_prefs/" to "Ayarlar ve anahtar-değer deposu (4 KB)",
                            "databases/" to "SQLite sanal veri tabanları",
                            "cache/" to "Geçici ART önbellek dosyaları (64 KB)",
                            "lib/" to "Yerel kütüphane sembolik bağları (arm64-v8a)"
                        ).forEach { (dir, desc) ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2332))) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(dir, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Text(desc, color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Android 9 Quick Settings Shade
// -------------------------------------------------------------
@Composable
fun Android9QuickSettingsShade(
    onClose: () -> Unit
) {
    var wifiOn by remember { mutableStateOf(true) }
    var houdiniOn by remember { mutableStateOf(true) }
    var artJitOn by remember { mutableStateOf(true) }
    var airplaneOn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6141D2C))
            .clickable(onClick = onClose)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Android 9 Pie Hızlı Ayarlar", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Kapat", tint = Color.White)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            QuickSettingToggle(
                label = "Wi-Fi 64",
                icon = Icons.Default.Wifi,
                isActive = wifiOn,
                onClick = { wifiOn = !wifiOn }
            )
            QuickSettingToggle(
                label = "32->64 Houdini",
                icon = Icons.Default.Transform,
                isActive = houdiniOn,
                onClick = { houdiniOn = !houdiniOn }
            )
            QuickSettingToggle(
                label = "ART JIT",
                icon = Icons.Default.Speed,
                isActive = artJitOn,
                onClick = { artJitOn = !artJitOn }
            )
            QuickSettingToggle(
                label = "Uçak Modu",
                icon = Icons.Default.AirplanemodeActive,
                isActive = airplaneOn,
                onClick = { airplaneOn = !airplaneOn }
            )
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2332))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sistem Bilgisi: Android 9.0 (API 28)", color = TechCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Çekirdek: 4.9.148-android9-64-pie-qemu", color = Color.LightGray, fontSize = 11.sp)
                Text("Mimari: aarch64 / arm64-v8a", color = AccentGreen, fontSize = 11.sp)
                Text("RAM Kullanımı: 1.4 GB / 4.0 GB LPDDR4", color = Color.LightGray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun QuickSettingToggle(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isActive) TechCyan else Color.White.copy(alpha = 0.15f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (isActive) Color.Black else Color.White, modifier = Modifier.size(22.dp))
        }
        Text(label, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

// -------------------------------------------------------------
// Android 9 Recents Screen (Task Switcher)
// -------------------------------------------------------------
@Composable
fun Android9RecentsScreen(
    runningProcesses: List<com.example.model.Android9Process>,
    installedApps: List<Android9VirtualApp>,
    onSelectApp: (Android9VirtualApp) -> Unit,
    onClearAll: () -> Unit
) {
    val activeUserApps = runningProcesses.mapNotNull { proc ->
        installedApps.find { it.packageName == proc.packageName }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D131F))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Son Kullanılanlar", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

        if (activeUserApps.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Açık uygulama yok", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(activeUserApps) { app ->
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .height(280.dp)
                            .clickable { onSelectApp(app) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2332))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(app.appIconColorHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(app.appIconSymbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(app.appName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = TechCyan)
                                    Text("Önizleme", color = Color.Gray, fontSize = 10.sp)
                                }
                            }

                            Button(
                                onClick = { onSelectApp(app) },
                                colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Aç", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        if (activeUserApps.isNotEmpty()) {
            OutlinedButton(
                onClick = onClearAll,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tümünü Temizle", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// Android 9 In-Emulator Terminal (Shell)
// -------------------------------------------------------------
@Composable
fun Android9TerminalScreen(
    history: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Android 9 64-Bit Root Shell (sh)", color = AccentGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }

        // Quick Command Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chips = listOf("help", "uname -a", "getprop", "cat /proc/cpuinfo", "pm list packages", "ps -ef", "df -h", "free -m", "clear")
            items(chips) { cmd ->
                SuggestionChip(
                    onClick = { onSendCommand(cmd) },
                    label = { Text(cmd, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                )
            }
        }

        // Output History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(history) { line ->
                Text(
                    text = line,
                    color = if (line.startsWith("pie-arm64")) TechCyan else Color(0xFFDDDDDD),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Input Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("pie-arm64:/ # ", color = AccentGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            TextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f).testTag("terminal_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            IconButton(onClick = { onSendCommand(input) }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", tint = TechCyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Android 9 Pie Navigation Bar (Pill Gesture + Back + Recents)
// -------------------------------------------------------------
@Composable
fun Android9NavigationBar(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRecents: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(Color(0xFF141923))
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Triangle (Pie style)
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp).testTag("nav_btn_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }

        // Home Pill (Android 9 Pie horizontal pill)
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(10.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable(onClick = onHome)
                .testTag("nav_btn_home")
        )

        // Recents Square
        IconButton(onClick = onRecents, modifier = Modifier.size(36.dp).testTag("nav_btn_recents")) {
            Icon(Icons.Default.CropSquare, contentDescription = "Son Uygulamalar", tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}

// -------------------------------------------------------------
// APK Transfer & Installation Dialog
// -------------------------------------------------------------
@Composable
fun ApkTransferDialog(
    isInstalling: Boolean,
    statusText: String,
    onDismiss: () -> Unit,
    onPickFromDevice: () -> Unit,
    onInstallSample: (String) -> Unit,
    onInstallFromPatchedList: (com.example.model.PatchedApkRecord) -> Unit,
    onOpenArchAnalyzer: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val patchedList = remember { PatchedAppStore.getRecords(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = TechCyan)
                Text("Android 9'a APK Aktar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            if (isInstalling) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = TechCyan)
                    Text(statusText, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Shortcut to Transferred APK Architecture Scanner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArchAnalyzer() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                Text("Aktarılan APK Mimari Analizi (32/64-Bit)", fontSize = 11.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = TechCyan
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text("Cihazdan", fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text("Dönüştürülenler", fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                            Text("Örnek APK'lar", fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                        }
                    }

                    when (selectedTab) {
                        // Tab 0: Pick from device storage
                        0 -> {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Cihazınızdaki herhangi bir .apk dosyasını seçerek doğrudan Android 9 64-bit emülatör sistemine aktarıp çalıştırabilirsiniz.",
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                                Button(
                                    onClick = onPickFromDevice,
                                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_pick_apk_file")
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("APK Dosyası Seç (.apk)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Tab 1: Converted / Patched APKs
                        1 -> {
                            if (patchedList.isEmpty()) {
                                Text("Henüz dönüştürülmüş APK bulunmuyor. APK Çevirici sekmesinden bir APK yamalayabilirsiniz.", fontSize = 11.sp, color = Color.Gray)
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .height(180.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(patchedList) { rec ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F293B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(rec.clonedAppName ?: rec.originalName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                                    Text(rec.clonedPackageName ?: rec.packageName, fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Button(
                                                    onClick = { onInstallFromPatchedList(rec) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Kur", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Tab 2: Sample apps
                        2 -> {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SampleApkInstallRow(
                                    title = "Retro 32-Bit Arcade (ARMv7)",
                                    desc = "Eski 32-bit yerel oyun kütüphaneleri içerir. Houdini çevirisini test eder.",
                                    onClick = { onInstallSample("retro32") }
                                )
                                SampleApkInstallRow(
                                    title = "Flappy Bird (32-Bit Klasik)",
                                    desc = "Klasik 32-bit oyun simülasyonu.",
                                    onClick = { onInstallSample("flappy32") }
                                )
                                SampleApkInstallRow(
                                    title = "Crypto Wallet 64-Bit",
                                    desc = "64-bit yerel kütüphanelerle optimize edilmiş uygulama.",
                                    onClick = { onInstallSample("crypto64") }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = TechCyan)
            }
        }
    )
}

@Composable
fun SampleApkInstallRow(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Text(desc, fontSize = 10.sp, color = Color.Gray)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("Yükle", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

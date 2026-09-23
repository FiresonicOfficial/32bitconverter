package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.FileSelectionUtility
import com.example.model.DiscoveredApkFile
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileSelectionTab(val title: String) {
    SYSTEM_PICKER("Sistem Seçici"),
    STORAGE_SCAN("Depolama Taraması"),
    RECENTS_AND_DEMO("Son / Örnek APK")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionUtilityModal(
    onApkUriSelected: (Uri) -> Unit,
    onApkFileSelected: (File) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(FileSelectionTab.STORAGE_SCAN) }
    var searchQuery by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var discoveredApks by remember { mutableStateOf<List<DiscoveredApkFile>>(emptyList()) }
    var recentApks by remember { mutableStateOf<List<DiscoveredApkFile>>(emptyList()) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    fun refreshStorage() {
        scope.launch {
            isScanning = true
            discoveredApks = FileSelectionUtility.scanStorageForApks(context)
            recentApks = FileSelectionUtility.getRecentlySelectedApks(context)
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        refreshStorage()
    }

    // Android OpenDocument picker launcher supporting all APK MIME types
    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onApkUriSelected(uri)
            onDismissRequest()
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .padding(vertical = 12.dp)
                .testTag("file_selection_utility_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TechCyan.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = TechCyan, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(
                                text = "APK Dosya Seçim Aracı",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Dönüştürmek için cihaz depolamasından APK seçin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Tab Switcher
                PrimaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTab.ordinal),
                            color = TechCyan,
                            width = 36.dp
                        )
                    }
                ) {
                    FileSelectionTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) TechCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // Tab Contents
                when (selectedTab) {
                    FileSelectionTab.STORAGE_SCAN -> {
                        StorageScanSection(
                            isScanning = isScanning,
                            discoveredApks = discoveredApks,
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { searchQuery = it },
                            selectedCategory = selectedCategoryFilter,
                            onCategorySelected = { selectedCategoryFilter = it },
                            onRefresh = { refreshStorage() },
                            onSelectApk = { apk ->
                                FileSelectionUtility.recordRecentlySelectedApk(context, apk.file)
                                onApkFileSelected(apk.file)
                                onDismissRequest()
                            },
                            onOpenSystemPicker = {
                                systemPickerLauncher.launch(
                                    arrayOf(
                                        "application/vnd.android.package-archive",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                        )
                    }

                    FileSelectionTab.SYSTEM_PICKER -> {
                        SystemPickerSection(
                            onLaunchPicker = {
                                systemPickerLauncher.launch(
                                    arrayOf(
                                        "application/vnd.android.package-archive",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                        )
                    }

                    FileSelectionTab.RECENTS_AND_DEMO -> {
                        RecentsAndDemoSection(
                            recentApks = recentApks,
                            onSelectRecent = { apk ->
                                onApkFileSelected(apk.file)
                                onDismissRequest()
                            },
                            onSelectDemo = {
                                val demoFile = createDemo32BitApk(context)
                                onApkFileSelected(demoFile)
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageScanSection(
    isScanning: Boolean,
    discoveredApks: List<DiscoveredApkFile>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onRefresh: () -> Unit,
    onSelectApk: (DiscoveredApkFile) -> Unit,
    onOpenSystemPicker: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Refresh Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("APK adı veya klasör ara...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TechCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("storage_search_field")
            )

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .testTag("refresh_storage_scan_button")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TechCyan)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = TechCyan)
                }
            }
        }

        // Category Filter Chips
        val categories = listOf("Tümü") + discoveredApks.map { it.directoryCategory }.distinct()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.take(4).forEach { cat ->
                val isSelected = (cat == "Tümü" && selectedCategory == null) || (cat == selectedCategory)
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(if (cat == "Tümü") null else cat) },
                    label = { Text(cat, fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Filtered APK List
        val filteredList = discoveredApks.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.directoryCategory.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.directoryCategory == selectedCategory
            matchesQuery && matchesCategory
        }

        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = TechCyan)
                    Text("Cihaz depolaması taranıyor...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Default.FolderOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Aramaya uygun APK dosyası bulunamadı." else "Standart depolama klasörlerinde APK bulunamadı.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onOpenSystemPicker,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tüm Dosyalardan Seç", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("discovered_apk_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { apk ->
                    ApkFileItemRow(apk = apk, onClick = { onSelectApk(apk) })
                }
            }
        }
    }
}

@Composable
private fun ApkFileItemRow(
    apk: DiscoveredApkFile,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (apk.isSuggested32Bit) AccentAmber.copy(alpha = 0.2f) else TechCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = if (apk.isSuggested32Bit) AccentAmber else TechCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = apk.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = apk.directoryCategory,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• ${formatBytes(apk.sizeBytes)} • ${dateFormat.format(Date(apk.lastModified))}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onClick) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Seç", tint = TechCyan, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SystemPickerSection(
    onLaunchPicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(TechCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TechCyan, modifier = Modifier.size(32.dp))
                }

                Text(
                    text = "Android Sistem Dosya Yöneticisi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Google Drive, İndirilenler klasörü, SD kart, harici USB veya diğer uygulamalardan doğrudan bir .APK dosyası seçin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onLaunchPicker,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("launch_system_file_picker_button")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Dosya Seçicisini Aç", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick tip guide
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                    Text("APK Nerede Bulunur?", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentAmber)
                }
                Text(
                    text = "• Tarayıcı ile indirdiyseniz: Dosyalar > İndirilenler (Download)\n" +
                            "• Telegram / WhatsApp üzerinden aldıysanız: Dahili Depolama > Telegram/Media\n" +
                            "• Bilgisayardan aktardıysanız: /sdcard kök dizini veya Belgeler",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentsAndDemoSection(
    recentApks: List<DiscoveredApkFile>,
    onSelectRecent: (DiscoveredApkFile) -> Unit,
    onSelectDemo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Demo APK quick action card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = TechCyan.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TechCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = TechCyan, modifier = Modifier.size(24.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Örnek 32-Bit APK Testi", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TechCyan)
                    Text(
                        "libgame_engine.so içeren örnek 32-bit (armeabi-v7a) APK test paketi.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onSelectDemo,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("load_demo_apk_button")
                ) {
                    Text("Yükle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Recent APKs
        Text(
            text = "SON SEÇİLEN APK DOSYALARI",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (recentApks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Henüz seçilmiş bir APK kaydı bulunmuyor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentApks) { apk ->
                    ApkFileItemRow(apk = apk, onClick = { onSelectRecent(apk) })
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}

private fun createDemo32BitApk(context: Context): File {
    val demoFile = File(context.cacheDir, "classic_game_32bit_demo.apk")
    if (demoFile.exists()) return demoFile

    java.util.zip.ZipOutputStream(java.io.FileOutputStream(demoFile)).use { out ->
        val dexEntry = java.util.zip.ZipEntry("classes.dex")
        out.putNextEntry(dexEntry)
        out.write("dex\n035\u0000DEMO_DEX_DATA_ARM32_COMPAT".toByteArray())
        out.closeEntry()

        val libEntry = java.util.zip.ZipEntry("lib/armeabi-v7a/libgame_engine.so")
        out.putNextEntry(libEntry)
        val elf32 = ByteArray(1024)
        elf32[0] = 0x7F
        elf32[1] = 'E'.code.toByte()
        elf32[2] = 'L'.code.toByte()
        elf32[3] = 'F'.code.toByte()
        elf32[4] = 1 // 32-bit
        elf32[5] = 1 // Little endian
        elf32[18] = 0x28 // EM_ARM
        out.write(elf32)
        out.closeEntry()

        val manifestEntry = java.util.zip.ZipEntry("AndroidManifest.xml")
        out.putNextEntry(manifestEntry)
        out.write("AXML_SAMPLE_MANIFEST".toByteArray())
        out.closeEntry()
    }
    return demoFile
}

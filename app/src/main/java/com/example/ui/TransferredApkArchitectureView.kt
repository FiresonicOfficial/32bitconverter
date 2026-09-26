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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.Android9VirtualEnvironment
import com.example.compat.TransferredApkScanner
import com.example.model.NativeLibraryInfo
import com.example.model.TransferredApkArchitectureInfo
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferredApkArchitectureView(
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    onLaunchInEmulator: ((packageName: String) -> Unit)? = null,
    onNavigateToPatcher: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apkList by remember { mutableStateOf<List<TransferredApkArchitectureInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, 32BIT, 64BIT, PUREDEX
    var selectedApkForDeepDive by remember { mutableStateOf<TransferredApkArchitectureInfo?>(null) }

    fun refreshList() {
        scope.launch {
            isLoading = true
            apkList = TransferredApkScanner.scanAllTransferredApks(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val result = TransferredApkScanner.scanFromUri(context, uri)
                isLoading = false
                result.onSuccess { scanned ->
                    Toast.makeText(context, "${scanned.appName} mimari analizi tamamlandı: ${scanned.architectureLabel}", Toast.LENGTH_SHORT).show()
                    // Also install to virtual VM so user can immediately execute it
                    Android9VirtualEnvironment.installApkFromUri(context, uri)
                    refreshList()
                }.onFailure { err ->
                    Toast.makeText(context, "Tarama hatası: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Filter calculations
    val totalCount = apkList.size
    val count32Bit = apkList.count { it.is32Bit }
    val count64Bit = apkList.count { it.is64Bit }
    val countPureDex = apkList.count { it.isPureDex }

    val filteredList = apkList.filter { item ->
        val matchesFilter = when (selectedFilter) {
            "32BIT" -> item.is32Bit
            "64BIT" -> item.is64Bit
            "PUREDEX" -> item.isPureDex
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                item.appName.contains(searchQuery, ignoreCase = true) ||
                item.packageName.contains(searchQuery, ignoreCase = true) ||
                item.nativeLibraries.any { it.name.contains(searchQuery, ignoreCase = true) }
        matchesFilter && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C1017))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Top Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161E2E))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp).testTag("btn_close_arch_view")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = TechCyan)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TechCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = TechCyan, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "Aktarılan APK Mimari Analizi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "32-Bit (ARMv7) & 64-Bit (ARM64) İkili Kütüphane ve ELF Tarayıcısı",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }

            IconButton(
                onClick = { refreshList() },
                modifier = Modifier.size(34.dp).testTag("btn_refresh_arch_scan")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Yeniden Tara", tint = TechCyan)
            }
        }

        // --- Summary Stats Dashboard ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Toplam APK",
                value = "$totalCount",
                accentColor = Color.White,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "32-Bit (Houdini)",
                value = "$count32Bit",
                accentColor = AccentAmber,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "64-Bit Yerel",
                value = "$count64Bit",
                accentColor = TechCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Saf DEX",
                value = "$countPureDex",
                accentColor = AccentPurple,
                modifier = Modifier.weight(1f)
            )
        }

        // --- Architecture Ratio Bar ---
        if (totalCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF131926))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mimari Dağılımı Oranı",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Text(
                        text = "%${if (totalCount > 0) (count32Bit * 100 / totalCount) else 0} 32-Bit • %${if (totalCount > 0) ((count64Bit + countPureDex) * 100 / totalCount) else 0} 64-Bit/DEX",
                        fontSize = 10.sp,
                        color = TechCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF232D3F))
                ) {
                    if (count32Bit > 0) {
                        Box(
                            modifier = Modifier
                                .weight(count32Bit.toFloat())
                                .fillMaxHeight()
                                .background(AccentAmber)
                        )
                    }
                    if (count64Bit > 0) {
                        Box(
                            modifier = Modifier
                                .weight(count64Bit.toFloat())
                                .fillMaxHeight()
                                .background(TechCyan)
                        )
                    }
                    if (countPureDex > 0) {
                        Box(
                            modifier = Modifier
                                .weight(countPureDex.toFloat())
                                .fillMaxHeight()
                                .background(AccentPurple)
                        )
                    }
                }
            }
        }

        // --- Action Buttons: Pick APK & Add Samples ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("btn_pick_apk_arch_scan")
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("APK Tara (+)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        // Install a dummy 32-bit sample app for immediate testing
                        val sampleFile = File(context.cacheDir, "sample_32bit_game_${System.currentTimeMillis()}.apk")
                        sampleFile.writeText("DUMMY_32BIT_GAME_BINARY")
                        Android9VirtualEnvironment.installApkFile(
                            context = context,
                            file = sampleFile,
                            customLabel = "Space Shooter 32",
                            isCloned = false
                        )
                        refreshList()
                        Toast.makeText(context, "32-Bit test APK'sı sisteme aktarıldı!", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentAmber),
                modifier = Modifier.weight(1f).testTag("btn_add_sample_32bit")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("32-Bit Test APK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // --- Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Uygulama, paket veya .so kütüphanesi ara...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Temizle", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("arch_search_field"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TechCyan,
                unfocusedBorderColor = Color(0xFF2C394E),
                focusedContainerColor = Color(0xFF131926),
                unfocusedContainerColor = Color(0xFF131926),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // --- Filter Chips ---
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                "ALL" to "Tümü ($totalCount)",
                "32BIT" to "32-Bit ($count32Bit)",
                "64BIT" to "64-Bit ($count64Bit)",
                "PUREDEX" to "Saf DEX ($countPureDex)"
            )
            items(filters) { (key, label) ->
                val isSelected = selectedFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (key == "32BIT") AccentAmber.copy(alpha = 0.25f) else TechCyan.copy(alpha = 0.25f),
                        selectedLabelColor = if (key == "32BIT") AccentAmber else TechCyan,
                        containerColor = Color(0xFF161E2E),
                        labelColor = Color.LightGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (key == "32BIT") AccentAmber.copy(alpha = 0.5f) else TechCyan.copy(alpha = 0.5f)
                    )
                )
            }
        }

        // --- APK List ---
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = TechCyan)
                    Text("APK mimari yapıları ve ELF kütüphaneleri taranıyor...", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FilterListOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    Text("Kriterlere uygun aktarılan APK bulunamadı.", color = Color.Gray, fontSize = 13.sp)
                    Text("Cihazınızdan yeni bir APK taratabilir veya örnek ekleyebilirsiniz.", color = Color.DarkGray, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    TransferredApkCard(
                        item = item,
                        onOpenDeepDive = { selectedApkForDeepDive = item },
                        onLaunch = {
                            val vmApp = Android9VirtualEnvironment.installedApps.find { app -> app.packageName == item.packageName }
                            if (vmApp != null) {
                                Android9VirtualEnvironment.launchApp(vmApp)
                                onLaunchInEmulator?.invoke(item.packageName)
                            } else {
                                Toast.makeText(context, "${item.appName} emülatörde başlatılıyor...", Toast.LENGTH_SHORT).show()
                                onLaunchInEmulator?.invoke(item.packageName)
                            }
                        },
                        onNavigateToPatcher = onNavigateToPatcher
                    )
                }
            }
        }
    }

    // --- Deep Dive Architecture Dialog ---
    if (selectedApkForDeepDive != null) {
        ApkArchitectureDeepDiveDialog(
            info = selectedApkForDeepDive!!,
            onDismiss = { selectedApkForDeepDive = null },
            onLaunch = {
                val pkg = selectedApkForDeepDive!!.packageName
                selectedApkForDeepDive = null
                onLaunchInEmulator?.invoke(pkg)
            },
            onNavigateToPatcher = {
                selectedApkForDeepDive = null
                onNavigateToPatcher?.invoke()
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransferredApkCard(
    item: TransferredApkArchitectureInfo,
    onOpenDeepDive: () -> Unit,
    onLaunch: () -> Unit,
    onNavigateToPatcher: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.is32Bit) AccentAmber.copy(alpha = 0.4f) else TechCyan.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("arch_card_${item.packageName}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Main Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(item.appIconColorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.appIconSymbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.appName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.packageName,
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "v${item.versionName} • ${item.originSource} • ${(item.totalSizeBytes / 1024)} KB",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Prominent Architecture Badge
                val badgeColor = when {
                    item.is32Bit -> AccentAmber
                    item.isPureDex -> AccentPurple
                    else -> TechCyan
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.is32Bit) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (item.is32Bit) "32-Bit (ARMv7)" else if (item.isPureDex) "Saf DEX" else "64-Bit (ARM64)",
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Execution Mode Banner
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0F141F),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Çalışma Modu: ${item.executionMode}",
                        fontSize = 10.sp,
                        color = if (item.is32Bit) AccentAmber else Color.LightGray,
                        fontWeight = if (item.is32Bit) FontWeight.Medium else FontWeight.Normal
                    )
                    Text(
                        text = "${item.dexCount} DEX",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Native Libraries Summary & Toggle
            if (item.nativeLibraries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = TechCyan, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${item.nativeLibraries.size} adet yerel kütüphane (.so)",
                            fontSize = 11.sp,
                            color = TechCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Genişlet",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Expanded Native Libraries List
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F141F))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.nativeLibraries.forEach { lib ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (lib.is64Bit) TechCyan else AccentAmber)
                                    )
                                    Text(
                                        text = lib.name,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (lib.is64Bit) TechCyan.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (lib.is64Bit) "ELF64 (64-Bit)" else "ELF32 (32-Bit)",
                                        color = if (lib.is64Bit) TechCyan else AccentAmber,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Yerel kütüphane (.so) içermez. Saf DEX yürütmesiyle doğrudan 64-bit ART uyumludur.",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Launch in VM Button
                Button(
                    onClick = onLaunch,
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).testTag("btn_launch_vm_${item.packageName}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Emülatörde Aç", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // Deep Dive Architecture Inspection
                OutlinedButton(
                    onClick = onOpenDeepDive,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    modifier = Modifier.testTag("btn_deep_dive_${item.packageName}")
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ELF Detayı", fontSize = 11.sp)
                }

                // Convert to 64-bit button if 32-bit
                if (item.is32Bit && onNavigateToPatcher != null) {
                    OutlinedButton(
                        onClick = onNavigateToPatcher,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentAmber)
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("64-Bit'e Çevir", fontSize = 11.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Detailed Architecture Inspection Dialog (ELF & Registers)
// -------------------------------------------------------------
@Composable
fun ApkArchitectureDeepDiveDialog(
    info: TransferredApkArchitectureInfo,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onNavigateToPatcher: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Architecture, contentDescription = null, tint = TechCyan)
                Text("Mimari Analiz: ${info.appName}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Architecture verdict banner
                val verdictColor = if (info.is32Bit) AccentAmber else TechCyan
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = verdictColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, verdictColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Mimari Türü: ${info.architectureLabel}",
                            color = verdictColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (info.is32Bit) {
                                "Bu APK yalnızca 32-bit (armeabi-v7a / AArch32) ikili kütüphaneleri içerir. 64-bit saf donanımlarda doğrudan çalışamaz; Android 9 VM içerisinde Houdini ARM32 -> ARM64 ikili çevirici ile yürütülür."
                            } else if (info.isPureDex) {
                                "Bu APK yerel C/C++ ikili kodu (.so) barındırmaz. Saf Dalvik/ART bayt kodundan oluştuğu için 64-bit Android sistemlerinde doğrudan ve en yüksek verimle çalışır."
                            } else {
                                "Bu APK doğrudan 64-bit (arm64-v8a / AArch64) yerel kütüphaneleri içerir ve 64-bit Android 9 ART derleyicisinde yerel makine koduyla çalışır."
                            },
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Technical Spec Table
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131926)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Teknik Mimari Özellikleri", color = TechCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        TechRow("Paket Kimliği", info.packageName)
                        TechRow("Birincil ABI", info.primaryAbi)
                        TechRow("Desteklenen ABI'lar", info.supportedAbis.joinToString(", "))
                        TechRow("DEX Dosya Sayısı", "${info.dexCount} adet")
                        TechRow("Adresleme Modu", if (info.is32Bit) "32-Bit Sanal Adres Alanı (Max 4GB)" else "64-Bit Sanal Adres Alanı (Max 128TB)")
                        TechRow("CPU Yazmaç Yapısı", if (info.is32Bit) "ARM32: R0-R15 (32-bit GPR) • VFPv3" else "ARM64: X0-X30 (64-bit GPR) • NEON/ASIMD")
                        TechRow("Sistem Çağrısı (Syscall)", if (info.is32Bit) "swi / svc 0x0 (EABI 32)" else "svc #0 (AArch64 64-bit ABI)")
                    }
                }

                // Native Libraries breakdown
                if (info.nativeLibraries.isNotEmpty()) {
                    Text("Tespit Edilen ELF Yerel Kütüphaneler", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    info.nativeLibraries.forEach { lib ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(lib.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (lib.is64Bit) TechCyan.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (lib.is64Bit) "64-BIT" else "32-BIT",
                                            color = if (lib.is64Bit) TechCyan else AccentAmber,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("ABI Yolu: lib/${lib.abi}/", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("ELF Başlığı: ${lib.elfMachine}", fontSize = 10.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
            ) {
                Text("Emülatörde Başlat", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = TechCyan)
            }
        }
    )
}

@Composable
fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(
            value,
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

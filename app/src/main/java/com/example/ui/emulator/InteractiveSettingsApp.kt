package com.example.ui.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.Android9VirtualEnvironment
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.TechCyan

@Composable
fun InteractiveSettingsApp(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf("MAIN") } // MAIN, ABOUT, HOUDINI, STORAGE, DISPLAY

    var wifiEnabled by remember { mutableStateOf(true) }
    var houdiniJitEnabled by remember { mutableStateOf(true) }
    var fpsTarget60 by remember { mutableStateOf(true) }
    var devModeEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedSection != "MAIN") {
                    IconButton(onClick = { selectedSection = "MAIN" }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = TechCyan)
                    }
                } else {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = TechCyan, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = when (selectedSection) {
                        "ABOUT" -> "Cihaz Hakkında"
                        "HOUDINI" -> "Houdini İkili Çeviri"
                        "STORAGE" -> "Depolama"
                        "DISPLAY" -> "Ekran & Performans"
                        else -> "Ayarlar (Android 9.0 Pie)"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
        }

        // Settings Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            when (selectedSection) {
                "MAIN" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Wi-Fi
                        SettingsListItem(
                            icon = Icons.Default.Wifi,
                            title = "Ağ ve İnternet",
                            subtitle = if (wifiEnabled) "Bağlı: Virtual-WiFi-64 (Sanal Ağ)" else "Kapalı",
                            trailing = {
                                Switch(
                                    checked = wifiEnabled,
                                    onCheckedChange = { wifiEnabled = it }
                                )
                            }
                        )

                        // Houdini 32-Bit Translation Engine
                        SettingsListItem(
                            icon = Icons.Default.Transform,
                            title = "Houdini 32-Bit İkili Çevirici",
                            subtitle = if (houdiniJitEnabled) "Aktif • ARMv7 -> ARM64 Dinamik JIT" else "Devre Dışı",
                            onClick = { selectedSection = "HOUDINI" }
                        )

                        // Display & FPS
                        SettingsListItem(
                            icon = Icons.Default.Speed,
                            title = "Ekran ve Performans (FPS)",
                            subtitle = if (fpsTarget60) "60 FPS • ART Donanım Hızlandırma Açık" else "30 FPS",
                            onClick = { selectedSection = "DISPLAY" }
                        )

                        // Storage
                        SettingsListItem(
                            icon = Icons.Default.Storage,
                            title = "Depolama",
                            subtitle = "14.2 GB kullanılıyor / 64 GB toplam",
                            onClick = { selectedSection = "STORAGE" }
                        )

                        // About Phone
                        SettingsListItem(
                            icon = Icons.Default.PhoneAndroid,
                            title = "Sanal Cihaz Hakkında",
                            subtitle = "Android 9 (Pie) • API 28 • aarch64",
                            onClick = { selectedSection = "ABOUT" }
                        )
                    }
                }

                "ABOUT" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Sistem Kimlik Bilgileri", color = TechCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                InfoRow("Model", "Android 9 Pie Virtual Machine")
                                InfoRow("Android Sürümü", "9.0 (Pie)")
                                InfoRow("API Seviyesi", "28")
                                InfoRow("Çekirdek", "4.9.148-android9-64-pie-qemu")
                                InfoRow("Yapı Numarası", "PQ3A.190801.002")
                                InfoRow("Birincil ABI", "arm64-v8a (64-Bit)")
                                InfoRow("İkili Çeviri Katmanı", "Houdini ARM32 (armeabi-v7a)")
                                InfoRow("ART Sanal Makinesi", "ART 28.0 64-bit JIT")
                            }
                        }
                    }
                }

                "HOUDINI" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Houdini ARM32 -> ARM64 İkili Çeviri Motoru",
                            color = AccentAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "64-bit Android sistemlerinde 32-bit (armeabi-v7a) yerel .so kütüphanelerini çalıştırmak için talimatları runtime anında AArch64 makine koduna dönüştürür.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        SettingsListItem(
                            icon = Icons.Default.Bolt,
                            title = "Houdini JIT Derleyici",
                            subtitle = "Sık kullanılan döngüleri anında optimize eder",
                            trailing = {
                                Switch(
                                    checked = houdiniJitEnabled,
                                    onCheckedChange = {
                                        houdiniJitEnabled = it
                                        Android9VirtualEnvironment.addLog("I", "Houdini", "JIT Compiler mode toggled: $it")
                                    }
                                )
                            }
                        )

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Çevirici İstatistikleri:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                InfoRow("Yazmaç Eşlemesi", "ARM32 R0-R15 -> ARM64 X0-X15")
                                InfoRow("Bellek Sayfalama", "4KB Sanal Adres Alanı")
                                InfoRow("Syscall Köprüsü", "EABI 32 swi/svc 0x0 Aktif")
                                InfoRow("NEON / VFPv3", "Donanımsal ASIMD Çevirisi")
                            }
                        }
                    }
                }

                "STORAGE" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Depolama Alanı Dağılımı", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        LinearProgressIndicator(
                            progress = { 0.22f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = TechCyan,
                            trackColor = Color(0xFF334155)
                        )
                        Text("14.2 GB / 64 GB kullanılıyor (%22)", color = Color.Gray, fontSize = 11.sp)

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                InfoRow("/system (Sistem İmajı)", "1.8 GB")
                                InfoRow("/data/app (Kurulu APK'lar)", "2.4 GB")
                                InfoRow("/data/data (Uygulama Verileri)", "820 MB")
                                InfoRow("/sdcard/Download", "340 MB")
                            }
                        }
                    }
                }

                "DISPLAY" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsListItem(
                            icon = Icons.Default.Speed,
                            title = "60 FPS Yenileme Hızı",
                            subtitle = if (fpsTarget60) "Akıcı 60 FPS oyun ve arayüz modu" else "Pil tasarrufu modu 30 FPS",
                            trailing = {
                                Switch(
                                    checked = fpsTarget60,
                                    onCheckedChange = { fpsTarget60 = it }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TechCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = TechCyan, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.LightGray, fontSize = 10.sp)
                }
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

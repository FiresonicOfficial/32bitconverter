package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DeviceAbiInfo
import com.example.ui.theme.*

@Composable
fun SystemAuditView(
    deviceInfo: DeviceAbiInfo,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Hero Card
        val is64Only = deviceInfo.isStrict64BitOnly
        val statusColor = if (is64Only) AccentAmber else AccentGreen
        val statusTitle = if (is64Only) "SADECE 64-BİT İŞLETİM SİSTEMİ" else "HİBRİT 32/64-BİT SİSTEM"
        val statusSub = if (is64Only) {
            "Cihazınızda 32-bit (armeabi-v7a) kütüphane desteği devre dışıdır. 32-bit uygulamalar için ikili çeviri (Tango) veya APK dönüştürücü gerekir."
        } else {
            "Cihazınız hem 32-bit hem de 64-bit yerel uygulamaları doğrudan çalıştırabilir."
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().testTag("status_hero_card")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (is64Only) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = "Durum İkonu",
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Text(
                            text = deviceInfo.deviceModel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_audit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = TechCyan
                        )
                    }
                }

                Text(
                    text = statusSub,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Hardware & Architecture Specs
        Text(
            text = "DONANIM & MİMARİ DENETİMİ",
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SpecRow(
                    icon = Icons.Default.Memory,
                    title = "CPU Mimarisi (os.arch)",
                    value = deviceInfo.cpuArch,
                    badge = if (deviceInfo.cpuArch.contains("64")) "64-Bit" else "32-Bit",
                    badgeColor = TechCyan
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SpecRow(
                    icon = Icons.Default.DeveloperBoard,
                    title = "AArch32 Donanım Desteği",
                    value = if (deviceInfo.hardwareAArch32Support) "İşlemcide Mevcut" else "Silikon Düzeyinde Kaldırıldı",
                    badge = if (deviceInfo.hardwareAArch32Support) "AArch32 Var" else "AArch32 Yok",
                    badgeColor = if (deviceInfo.hardwareAArch32Support) AccentGreen else AccentRed
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SpecRow(
                    icon = Icons.Default.Layers,
                    title = "OS 32-Bit Bionic Kütüphaneleri",
                    value = if (deviceInfo.system32BitLibrariesPresent) "/system/lib/libc.so mevcut" else "/system/lib bulunamadı",
                    badge = if (deviceInfo.system32BitLibrariesPresent) "Kütüphaneler Var" else "Yalnızca 64-Bit OS",
                    badgeColor = if (deviceInfo.system32BitLibrariesPresent) AccentGreen else AccentAmber
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SpecRow(
                    icon = Icons.Default.SettingsSystemDaydream,
                    title = "Zygote Modu",
                    value = deviceInfo.zygoteMode,
                    badge = deviceInfo.zygoteMode,
                    badgeColor = TechCyan
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SpecRow(
                    icon = Icons.Default.Transform,
                    title = "Algılanan Çevirici Katman",
                    value = deviceInfo.translationLayerDetected ?: "Aktif Çevirici Yok (Tango / Houdini)",
                    badge = if (deviceInfo.translationLayerDetected != null) "Çevirici Aktif" else "Pasif",
                    badgeColor = if (deviceInfo.translationLayerDetected != null) AccentGreen else MaterialTheme.colorScheme.outline
                )
            }
        }

        // Supported ABIs Table
        Text(
            text = "DESTEKLENEN ABI LİSTESİ",
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
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "64-Bit Yerel ABI'ler (Desteklenen):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (deviceInfo.supported64BitAbis.isEmpty()) {
                        Text("Bulunamadı", color = AccentRed, style = MaterialTheme.typography.bodySmall)
                    } else {
                        deviceInfo.supported64BitAbis.forEach { abi ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(abi, fontWeight = FontWeight.Bold, color = TechCyan) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = TechCyan.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "32-Bit Yerel ABI'ler (armeabi-v7a vb.):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (deviceInfo.supported32BitAbis.isEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("32-Bit ABI Yok (64-bit Only)", color = AccentRed, fontWeight = FontWeight.SemiBold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = AccentRed.copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        deviceInfo.supported32BitAbis.forEach { abi ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(abi, color = AccentGreen) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = AccentGreen.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Summary Note
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Bilgi",
                    tint = TechCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = deviceInfo.diagnosticSummary,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SpecRow(
    icon: ImageVector,
    title: String,
    value: String,
    badge: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = badgeColor.copy(alpha = 0.15f),
            modifier = Modifier.padding(start = 4.dp)
        ) {
            Text(
                text = badge,
                color = badgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

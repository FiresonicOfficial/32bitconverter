package com.example.ui.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.Android9VirtualEnvironment
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.TechCyan

data class VirtualFileEntry(
    val name: String,
    val isDirectory: Boolean,
    val sizeStr: String,
    val path: String
)

@Composable
fun InteractiveFileManagerApp(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf("/data/app") }
    val pathHistory = remember { mutableStateListOf<String>() }

    var newFileName by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filesMap = remember {
        mutableStateMapOf(
            "/data/app" to mutableStateListOf(
                VirtualFileEntry("com.retro.arcade32-1", true, "DIR", "/data/app/com.retro.arcade32-1"),
                VirtualFileEntry("com.benchmark.arm64-1", true, "DIR", "/data/app/com.benchmark.arm64-1"),
                VirtualFileEntry("flappy_armv7_base.apk", false, "1.8 MB", "/data/app/flappy_armv7_base.apk"),
                VirtualFileEntry("retro_arcade_engine.apk", false, "2.4 MB", "/data/app/retro_arcade_engine.apk")
            ),
            "/data/app/com.retro.arcade32-1" to mutableStateListOf(
                VirtualFileEntry("base.apk", false, "2.4 MB", "/data/app/com.retro.arcade32-1/base.apk"),
                VirtualFileEntry("base.odex", false, "840 KB", "/data/app/com.retro.arcade32-1/base.odex"),
                VirtualFileEntry("lib", true, "DIR", "/data/app/com.retro.arcade32-1/lib")
            ),
            "/system" to mutableStateListOf(
                VirtualFileEntry("app", true, "DIR", "/system/app"),
                VirtualFileEntry("priv-app", true, "DIR", "/system/priv-app"),
                VirtualFileEntry("bin", true, "DIR", "/system/bin"),
                VirtualFileEntry("lib64", true, "DIR", "/system/lib64"),
                VirtualFileEntry("build.prop", false, "4.2 KB", "/system/build.prop")
            ),
            "/system/bin" to mutableStateListOf(
                VirtualFileEntry("sh", false, "240 KB", "/system/bin/sh"),
                VirtualFileEntry("dex2oat", false, "1.2 MB", "/system/bin/dex2oat"),
                VirtualFileEntry("app_process64", false, "45 KB", "/system/bin/app_process64"),
                VirtualFileEntry("pm", false, "2.1 KB", "/system/bin/pm")
            ),
            "/sdcard/Download" to mutableStateListOf(
                VirtualFileEntry("retro_game_32bit.apk", false, "3.1 MB", "/sdcard/Download/retro_game_32bit.apk"),
                VirtualFileEntry("benchmark_64bit.apk", false, "4.8 MB", "/sdcard/Download/benchmark_64bit.apk"),
                VirtualFileEntry("notes.txt", false, "120 B", "/sdcard/Download/notes.txt")
            )
        )
    }

    val currentFiles = filesMap[currentPath] ?: mutableStateListOf()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = TechCyan, modifier = Modifier.size(20.dp))
                Column {
                    Text("Dosya Yöneticisi", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    Text(currentPath, color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showCreateDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Yeni Dosya", tint = TechCyan)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.LightGray)
                }
            }
        }

        // Quick Directory Jump Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161E2E))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickPaths = listOf("/data/app", "/system", "/system/bin", "/sdcard/Download")
            quickPaths.forEach { p ->
                val isSelected = currentPath == p
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) TechCyan.copy(alpha = 0.2f) else Color(0xFF1E293B),
                    modifier = Modifier.clickable {
                        pathHistory.add(currentPath)
                        currentPath = p
                    }
                ) {
                    Text(
                        text = p.split("/").last(),
                        fontSize = 11.sp,
                        color = if (isSelected) TechCyan else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // File List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(currentFiles) { item ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (item.isDirectory) {
                                pathHistory.add(currentPath)
                                currentPath = item.path
                                Android9VirtualEnvironment.addLog("D", "Files", "cd ${item.path}")
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = if (item.isDirectory) Icons.Default.Folder else if (item.name.endsWith(".apk")) Icons.Default.Android else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = if (item.isDirectory) TechCyan else if (item.name.endsWith(".apk")) AccentAmber else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(item.name, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                Text(if (item.isDirectory) "Dizin" else item.sizeStr, color = Color.Gray, fontSize = 10.sp)
                            }
                        }

                        if (!item.isDirectory) {
                            IconButton(
                                onClick = {
                                    currentFiles.remove(item)
                                    Android9VirtualEnvironment.addLog("I", "Files", "rm ${item.path}")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Yeni Sanal Dosya Oluştur", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Dosya Adı (Örn: test_app.apk)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            val newPath = "$currentPath/$newFileName"
                            currentFiles.add(VirtualFileEntry(newFileName, newFileName.endsWith("/"), "12 KB", newPath))
                            Android9VirtualEnvironment.addLog("I", "Files", "touch $newPath")
                            newFileName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TechCyan)
                ) {
                    Text("Oluştur", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

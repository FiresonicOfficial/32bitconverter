package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compat.SystemAbiAuditor
import com.example.ui.Android9EmulatorView
import com.example.ui.ApkScannerView
import com.example.ui.GuidesView
import com.example.ui.SystemAuditView
import com.example.ui.TranslationSandboxView
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TechCyan

enum class AppNavTab(val label: String) {
    AUDIT("Denetim"),
    SCAN_AND_PATCH("APK Çevirici"),
    ANDROID9_VM("Android 9"),
    EMULATOR("İkili Çeviri"),
    GUIDES("Rehber")
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            window.colorMode = android.content.pm.ActivityInfo.COLOR_MODE_DEFAULT
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var selectedTab by remember { mutableStateOf(AppNavTab.AUDIT) }
                var deviceInfo by remember { mutableStateOf(SystemAbiAuditor.auditDevice()) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = TechCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "32-Bit Bridge",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (deviceInfo.isStrict64BitOnly) AccentAmber.copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = if (deviceInfo.isStrict64BitOnly) "64-Bit Only" else "Hybrid 32/64",
                                            color = if (deviceInfo.isStrict64BitOnly) AccentAmber else AccentGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            actions = {
                                IconButton(
                                    onClick = { deviceInfo = SystemAbiAuditor.auditDevice() },
                                    modifier = Modifier.testTag("top_refresh_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = TechCyan)
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == AppNavTab.AUDIT,
                                onClick = { selectedTab = AppNavTab.AUDIT },
                                icon = { Icon(Icons.Default.Analytics, contentDescription = "Denetim") },
                                label = { Text("Denetim", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    indicatorColor = TechCyan
                                ),
                                modifier = Modifier.testTag("nav_audit")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppNavTab.SCAN_AND_PATCH,
                                onClick = { selectedTab = AppNavTab.SCAN_AND_PATCH },
                                icon = { Icon(Icons.Default.Build, contentDescription = "APK Çevirici") },
                                label = { Text("APK Çevirici", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    indicatorColor = TechCyan
                                ),
                                modifier = Modifier.testTag("nav_scan_patch")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppNavTab.ANDROID9_VM,
                                onClick = { selectedTab = AppNavTab.ANDROID9_VM },
                                icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "Android 9 VM") },
                                label = { Text("Android 9", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    indicatorColor = TechCyan
                                ),
                                modifier = Modifier.testTag("nav_android9_vm")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppNavTab.EMULATOR,
                                onClick = { selectedTab = AppNavTab.EMULATOR },
                                icon = { Icon(Icons.Default.Transform, contentDescription = "İkili Çeviri") },
                                label = { Text("İkili Çeviri", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    indicatorColor = TechCyan
                                ),
                                modifier = Modifier.testTag("nav_emulator")
                            )

                            NavigationBarItem(
                                selected = selectedTab == AppNavTab.GUIDES,
                                onClick = { selectedTab = AppNavTab.GUIDES },
                                icon = { Icon(Icons.Default.MenuBook, contentDescription = "Rehber") },
                                label = { Text("Rehber", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    indicatorColor = TechCyan
                                ),
                                modifier = Modifier.testTag("nav_guides")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            AppNavTab.AUDIT -> SystemAuditView(
                                deviceInfo = deviceInfo,
                                onRefresh = { deviceInfo = SystemAbiAuditor.auditDevice() }
                            )
                            AppNavTab.SCAN_AND_PATCH -> ApkScannerView(
                                onNavigateToEmulator = { selectedTab = AppNavTab.ANDROID9_VM }
                            )
                            AppNavTab.ANDROID9_VM -> Android9EmulatorView(
                                onNavigateToPatcher = { selectedTab = AppNavTab.SCAN_AND_PATCH }
                            )
                            AppNavTab.EMULATOR -> TranslationSandboxView()
                            AppNavTab.GUIDES -> GuidesView()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "32-Bit Bridge: $name", modifier = modifier)
}

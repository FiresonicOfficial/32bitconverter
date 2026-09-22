package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GuidesView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "64-BİT SİSTEMLERDE 32-BİT ÇALIŞTIRMA REHBERİ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TechCyan,
            letterSpacing = 1.sp
        )

        // Architecture Explanation Hero
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            modifier = Modifier.fillMaxWidth().testTag("architecture_explanation_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = TechCyan)
                    Text("Neden 32-Bit Uygulamalar Açılmıyor?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(
                    text = "Google Pixel 7/8/9, Galaxy S24 ve yeni nesil Snapdragon 8 Gen 3 / Dimensity 9300 işlemcili telefonlar (Cortex-X3, Cortex-X4, Cortex-A520 çekirdekleri) silikon düzeyinde 32-bit (AArch32) komut kümesini tamamen kaldırmıştır.\n\nAyrıca Android 14, 15 ve 16 işletim sistemleri 32-bit Bionic sistem kütüphanelerini (/system/lib/libc.so) içermez. Eski 32-bit uygulamaları açabilmek için 4 ana yöntem mevcuttur:",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Method 1: APK Repackaging
        GuideAccordionItem(
            title = "Yöntem 1: 64-Bit APK Dönüştürme (Bu Uygulama)",
            badge = "En Kolay • PC & Root Gerekmez",
            badgeColor = AccentGreen,
            isInitiallyExpanded = true,
            content = {
                Text(
                    text = "Birçok eski 32-bit uygulama aslında tamamen saf Java/Kotlin bytecode'u (DEX) ile yazılmıştır veya yalnızca eski analitik kütüphaneleri (Crashlytics, Flurry vb.) nedeniyle 32-bit görünür.\n\n" +
                            "1. Uygulamadaki 'APK Çevirici' sekmesinden APK'yı seçin veya yüklü uygulamalardan seçin.\n" +
                            "2. '64-Bit Dönüştür' butonuna basın.\n" +
                            "3. Uygulama eski 32-bit kilitleri ayıklar ve arm64-v8a köprüsü ekler.\n" +
                            "4. Oluşan yeni APK doğrudan telefonunuza kurulabilir ve 64-bit ART sanal makinesinde tam hızda çalışır.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        )

        // Method 2: Tango Dynamic Binary Translator
        GuideAccordionItem(
            title = "Yöntem 2: Tango İkili Çevirici (Binary Translator)",
            badge = "Pixel 7/8/9 & Ağır Oyunlar",
            badgeColor = TechCyan,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tango, tıpkı Apple'ın Rosetta 2 motoru veya Linux'taki Box64 gibi çalışan bir dinamik ikili çeviricidir. 32-bit ARM (ARMv7-A) yerel kütüphanelerini (.so) çalışma zamanında AArch64 komutlarına dönüştürür.",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                    Text("Kurulum Adımları:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TechCyan)
                    Text(
                        "1. Magisk veya KernelSU yüklü cihazınızda 'Tango Translator' modülünü indirin.\n" +
                                "2. Terminal üzerinden çeviriciyi etkinleştirin:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    CodeSnippetCard(
                        code = "su -c '/data/adb/modules/tango/tango_server --install-zygote-hook'",
                        onCopy = {
                            clipboardManager.setText(AnnotatedString("su -c '/data/adb/modules/tango/tango_server --install-zygote-hook'"))
                            Toast.makeText(context, "Kopyalandı!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )

        // Method 3: Shizuku & ADB
        GuideAccordionItem(
            title = "Yöntem 3: Shizuku / ADB ile TargetSDK Engelini Aşma",
            badge = "Kablosuz Hata Ayıklama",
            badgeColor = AccentAmber,
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Android 14+, TargetSDK değeri 23 veya altı olan 32-bit uygulamaların yüklenmesini 'INSTALL_FAILED_DEPRECATED_SDK_VERSION' hatasıyla engeller.\n\n" +
                                "Bilgisayar üzerinden veya telefonunuzda Shizuku/Termux kullanarak bu engeli aşağıdaki komutla kolayca aşabilirsiniz:",
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                    CodeSnippetCard(
                        code = "adb install --bypass-low-target-sdk-block -g uygulama_adi.apk",
                        onCopy = {
                            clipboardManager.setText(AnnotatedString("adb install --bypass-low-target-sdk-block -g uygulama_adi.apk"))
                            Toast.makeText(context, "Kopyalandı!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )

        // Method 4: Virtual 32-Bit Sandboxes (Twoyi / VMOS)
        GuideAccordionItem(
            title = "Yöntem 4: 32-Bit Sanal Alan (Twoyi / VMOS / Kapsayıcı)",
            badge = "Tam İzolasyon",
            badgeColor = AccentPurple,
            content = {
                Text(
                    text = "Ağır 32-bit 3D motoru olan eski oyunlar için cihazınızda hafif bir 32-bit Android kapsayıcısı (sandbox) çalıştırabilirsiniz.\n\n" +
                            "• Twoyi (开源 / Açık Kaynak): 64-bit cihazlarda 32-bit Android 8.1 mikro ortamı çalıştırır.\n" +
                            "• VMOS Pro: Yerleşik 32-bit emülasyon katmanı ile her türlü eski APK'yı açar.\n" +
                            "• VPhoneGaGa: 32-bit çekirdek kitaplıklarını kendi içinde barındırır.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        )
    }
}

@Composable
private fun GuideAccordionItem(
    title: String,
    badge: String,
    badgeColor: Color,
    isInitiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun CodeSnippetCard(code: String, onCopy: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF090F1C),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = AccentGreen,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = TechCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}

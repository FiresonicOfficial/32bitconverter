package com.example.compat

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.model.Android9Process
import com.example.model.Android9VirtualApp
import com.example.model.EmulatorSystemState
import com.example.model.VirtualLogcatEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile
import kotlin.random.Random

object Android9VirtualEnvironment {

    private const val PREFS_NAME = "android9_vm_prefs"
    private const val KEY_INSTALLED_APPS = "installed_virtual_apps"

    val systemState = mutableStateOf(EmulatorSystemState())
    val installedApps = mutableStateListOf<Android9VirtualApp>()
    val runningProcesses = mutableStateListOf<Android9Process>()
    val activeForegroundApp = mutableStateOf<Android9VirtualApp?>(null)
    val logcatEntries = mutableStateListOf<VirtualLogcatEntry>()

    val isPoweredOn = mutableStateOf(true)
    val isQuickSettingsOpen = mutableStateOf(false)
    val isInRecentsOverview = mutableStateOf(false)
    val isTerminalOpen = mutableStateOf(false)
    val selectedAppForDetail = mutableStateOf<Android9VirtualApp?>(null)

    // Interactive in-app execution state for the running app
    val virtualAppCounter = mutableStateOf(0)
    val virtualAppJniResult = mutableStateOf<String?>(null)
    val virtualAppStoredData = mutableStateOf<Map<String, String>>(emptyMap())
    val virtualAppActiveTab = mutableStateOf(0) // 0: App UI, 1: DEX & Libs, 2: Logcat, 3: Virtual FS

    private var isInitialized = false

    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        addLog("I", "init", "Android 9.0 (Pie) 64-Bit Virtual System kernel 4.9.148 booting...")
        addLog("I", "init", "CPU Architecture: aarch64 (ARMv8-A 64-bit)")
        addLog("I", "zygote64", "Zygote64 preloading 4820 classes, 126 shared libraries...")
        addLog("I", "system_server", "ActivityManagerService, PackageManagerService, WindowManagerService ready.")
        addLog("I", "art", "ART 28.0 64-bit JIT compiler initialized.")
        addLog("I", "Houdini64", "ARM32 (armeabi-v7a) -> ARM64 binary translation engine online.")

        // Built-in System Apps
        val systemApps = listOf(
            Android9VirtualApp(
                id = "sys_settings",
                packageName = "com.android.settings",
                appName = "Ayarlar",
                versionName = "9.0-pie",
                versionCode = 28,
                isSystemApp = true,
                is32Bit = false,
                abi = "arm64-v8a",
                targetSdk = 28,
                installPath = "/system/priv-app/Settings/Settings.apk",
                appIconColorHex = 0xFF455A64,
                appIconSymbol = "⚙",
                category = "Sistem",
                activities = listOf("com.android.settings.SettingsActivity", "com.android.settings.DeviceInfoActivity")
            ),
            Android9VirtualApp(
                id = "sys_files",
                packageName = "com.android.documentsui",
                appName = "Dosya Yöneticisi",
                versionName = "9.0-pie",
                versionCode = 28,
                isSystemApp = true,
                is32Bit = false,
                abi = "arm64-v8a",
                targetSdk = 28,
                installPath = "/system/priv-app/DocumentsUI/DocumentsUI.apk",
                appIconColorHex = 0xFF0288D1,
                appIconSymbol = "📁",
                category = "Sistem",
                activities = listOf("com.android.documentsui.FilesActivity")
            ),
            Android9VirtualApp(
                id = "sys_terminal",
                packageName = "com.android.terminal",
                appName = "Terminal (Shell)",
                versionName = "1.0",
                versionCode = 28,
                isSystemApp = true,
                is32Bit = false,
                abi = "arm64-v8a",
                targetSdk = 28,
                installPath = "/system/app/Terminal/Terminal.apk",
                appIconColorHex = 0xFF00C853,
                appIconSymbol = ">_",
                category = "Geliştirici",
                activities = listOf("com.android.terminal.TerminalActivity")
            ),
            Android9VirtualApp(
                id = "sys_installer",
                packageName = "com.android.packageinstaller",
                appName = "APK Aktar & Kur",
                versionName = "9.0-pie",
                versionCode = 28,
                isSystemApp = true,
                is32Bit = false,
                abi = "arm64-v8a",
                targetSdk = 28,
                installPath = "/system/priv-app/PackageInstaller/PackageInstaller.apk",
                appIconColorHex = 0xFF00B0FF,
                appIconSymbol = "📦",
                category = "Sistem",
                activities = listOf("com.android.packageinstaller.PackageInstallerActivity")
            ),
            Android9VirtualApp(
                id = "sys_arch_analyzer",
                packageName = "com.android.archanalyzer",
                appName = "Mimari Analizör",
                versionName = "1.0",
                versionCode = 28,
                isSystemApp = true,
                is32Bit = false,
                abi = "arm64-v8a",
                targetSdk = 28,
                installPath = "/system/priv-app/ArchAnalyzer/ArchAnalyzer.apk",
                appIconColorHex = 0xFF7C4DFF,
                appIconSymbol = "🔬",
                category = "Sistem Analizi",
                activities = listOf("com.android.archanalyzer.MainActivity")
            ),
            // Sample Pre-installed User Apps (Showing 32-bit translation vs 64-bit native execution)
            Android9VirtualApp(
                id = "sample_retro_arcade",
                packageName = "com.retro.arcade32",
                appName = "Retro Arcade 32",
                versionName = "1.2.0",
                versionCode = 120,
                isSystemApp = false,
                is32Bit = true,
                abi = "armeabi-v7a (32-bit Çevrildi)",
                targetSdk = 26,
                installPath = "/data/app/com.retro.arcade32-1/base.apk",
                appIconColorHex = 0xFFFF6D00,
                appIconSymbol = "🎮",
                category = "Oyun (32-Bit)",
                nativeLibs = listOf("libengine32.so", "libsound_armv7.so"),
                activities = listOf("com.retro.arcade32.MainActivity", "com.retro.arcade32.GameScreen")
            ),
            Android9VirtualApp(
                id = "sample_flappy_bird",
                packageName = "com.dotgears.flappy32",
                appName = "Flappy Bird 32",
                versionName = "1.3.0",
                versionCode = 130,
                isSystemApp = false,
                is32Bit = true,
                abi = "armeabi-v7a (32-bit Çevrildi)",
                targetSdk = 26,
                installPath = "/data/app/com.dotgears.flappy32-1/base.apk",
                appIconColorHex = 0xFFFFD600,
                appIconSymbol = "🐥",
                category = "Oyun (32-Bit)",
                nativeLibs = listOf("libflappy_physics.so", "libaudio_armv7.so"),
                activities = listOf("com.dotgears.flappy32.GameActivity")
            ),
            Android9VirtualApp(
                id = "sample_space_shooter",
                packageName = "com.galaxy.space32",
                appName = "Space Combat 32",
                versionName = "2.0.1",
                versionCode = 201,
                isSystemApp = false,
                is32Bit = true,
                abi = "armeabi-v7a (32-bit Çevrildi)",
                targetSdk = 26,
                installPath = "/data/app/com.galaxy.space32-1/base.apk",
                appIconColorHex = 0xFFFF3D00,
                appIconSymbol = "🚀",
                category = "Oyun (32-Bit)",
                nativeLibs = listOf("libspace_engine.so", "libgl2jni.so"),
                activities = listOf("com.galaxy.space32.CombatActivity")
            ),
            Android9VirtualApp(
                id = "sample_2048",
                packageName = "com.puzzle.game2048",
                appName = "2048 Puzzle",
                versionName = "1.0.5",
                versionCode = 105,
                isSystemApp = false,
                is32Bit = true,
                abi = "armeabi-v7a (32-bit Çevrildi)",
                targetSdk = 27,
                installPath = "/data/app/com.puzzle.game2048-1/base.apk",
                appIconColorHex = 0xFFE67E22,
                appIconSymbol = "🧩",
                category = "Bulmaca (32-Bit)",
                nativeLibs = listOf("libpuzzle_core.so"),
                activities = listOf("com.puzzle.game2048.MainActivity")
            ),
            Android9VirtualApp(
                id = "sample_crypto_wallet",
                packageName = "com.wallet.crypto64",
                appName = "Crypto Cüzdan 64",
                versionName = "3.1.0",
                versionCode = 310,
                isSystemApp = false,
                is32Bit = false,
                abi = "arm64-v8a (64-bit Yerel)",
                targetSdk = 28,
                installPath = "/data/app/com.wallet.crypto64-1/base.apk",
                appIconColorHex = 0xFF00B0FF,
                appIconSymbol = "💰",
                category = "Finans (64-Bit)",
                nativeLibs = listOf("libsecp256k1_arm64.so", "libkeystore_native.so"),
                activities = listOf("com.wallet.crypto64.WalletActivity")
            ),
            Android9VirtualApp(
                id = "sample_cpu_bench",
                packageName = "com.benchmark.arm64",
                appName = "ARM64 Benchmark",
                versionName = "2.0.4",
                versionCode = 204,
                isSystemApp = false,
                is32Bit = false,
                abi = "arm64-v8a (64-bit Yerel)",
                targetSdk = 28,
                installPath = "/data/app/com.benchmark.arm64-1/base.apk",
                appIconColorHex = 0xFFAA00FF,
                appIconSymbol = "⚡",
                category = "Performans",
                nativeLibs = listOf("libbenchmark64.so", "libmath_neon.so"),
                activities = listOf("com.benchmark.arm64.BenchmarkActivity")
            )
        )

        installedApps.clear()
        installedApps.addAll(systemApps)

        // Load persisted transferred apps
        loadPersistedApps(context)

        // Default background system processes
        runningProcesses.clear()
        runningProcesses.add(
            Android9Process(
                pid = 1042,
                packageName = "system_server",
                appName = "Android 9 System Server",
                memoryMb = 240,
                cpuPercent = 1.2f,
                isRunning = true,
                startedAt = System.currentTimeMillis() - 180000
            )
        )
        runningProcesses.add(
            Android9Process(
                pid = 1180,
                packageName = "com.android.systemui",
                appName = "System UI (Pie)",
                memoryMb = 95,
                cpuPercent = 0.5f,
                isRunning = true,
                startedAt = System.currentTimeMillis() - 170000
            )
        )
    }

    fun addLog(level: String, tag: String, message: String) {
        val entry = VirtualLogcatEntry(
            timestamp = timeFormat.format(Date()),
            pid = 1000 + Random.nextInt(9000),
            tag = tag,
            level = level,
            message = message
        )
        logcatEntries.add(0, entry)
        if (logcatEntries.size > 200) {
            logcatEntries.removeAt(logcatEntries.lastIndex)
        }
    }

    suspend fun installApkFromUri(context: Context, uri: Uri): Result<Android9VirtualApp> = withContext(Dispatchers.IO) {
        try {
            val displayName = FileSelectionUtility.extractDisplayNameFromUri(context, uri)
            val vmAppDir = File(context.filesDir, "android9_vm/data/app")
            if (!vmAppDir.exists()) vmAppDir.mkdirs()

            val tempApk = File(vmAppDir, "temp_import_${System.currentTimeMillis()}_$displayName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempApk.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Dosya okunamadı."))

            installApkFile(context, tempApk, null, false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installApkFile(
        context: Context,
        file: File,
        customLabel: String? = null,
        isCloned: Boolean = false
    ): Result<Android9VirtualApp> = withContext(Dispatchers.IO) {
        try {
            addLog("I", "PackageManager", "--- APK Yükleme Başlatıldı: ${file.name} ---")
            addLog("D", "PackageManager", "Dosya boyutu: ${file.length() / 1024} KB. İmza ve SHA256 doğrulanıyor...")

            val pm = context.packageManager
            val archiveInfo = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS)

            var packageName = archiveInfo?.packageName ?: "app.transferred.${System.currentTimeMillis() % 10000}"
            var appName = customLabel ?: file.nameWithoutExtension
            var versionName = archiveInfo?.versionName ?: "1.0"
            var versionCode = 1L
            var targetSdk = 28

            if (archiveInfo != null) {
                archiveInfo.applicationInfo?.sourceDir = file.absolutePath
                archiveInfo.applicationInfo?.publicSourceDir = file.absolutePath
                try {
                    val label = pm.getApplicationLabel(archiveInfo.applicationInfo!!).toString()
                    if (label.isNotBlank() && customLabel == null) {
                        appName = label
                    }
                } catch (_: Exception) {}
                targetSdk = archiveInfo.applicationInfo?.targetSdkVersion ?: 28
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    archiveInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    archiveInfo.versionCode.toLong()
                }
            }

            // Inspect ZIP for Native Libraries and DEX
            val nativeLibs = mutableListOf<String>()
            val abisFound = mutableSetOf<String>()
            var dexCount = 0

            try {
                ZipFile(file).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.startsWith("classes") && entry.name.endsWith(".dex")) {
                            dexCount++
                        }
                        if (entry.name.startsWith("lib/") && entry.name.endsWith(".so")) {
                            val parts = entry.name.split("/")
                            if (parts.size >= 3) {
                                abisFound.add(parts[1])
                                nativeLibs.add(parts.last())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                addLog("W", "PackageManager", "Zip analizi uyarısı: ${e.message}")
            }

            val has64Bit = abisFound.any { it.contains("64") }
            val has32BitOnly = abisFound.isNotEmpty() && !has64Bit
            val is32Bit = has32BitOnly

            val abiDescription = when {
                has64Bit -> "arm64-v8a (64-Bit Yerel)"
                has32BitOnly -> "armeabi-v7a (32-Bit -> ARM64 Houdini Çevrildi)"
                else -> "Saf DEX / Any-ABI (64-Bit ART Doğrudan)"
            }

            val finalTargetDir = File(context.filesDir, "android9_vm/data/app/$packageName-1")
            if (!finalTargetDir.exists()) finalTargetDir.mkdirs()
            val targetBaseApk = File(finalTargetDir, "base.apk")
            file.copyTo(targetBaseApk, overwrite = true)

            addLog("I", "dex2oat", "/system/bin/dex2oat --dex-file=${targetBaseApk.absolutePath} --oat-file=${finalTargetDir.absolutePath}/base.odex --instruction-set=arm64")
            addLog("I", "ART", "DEX Optimizasyonu: $dexCount adet DEX dosyası derlendi.")
            if (is32Bit) {
                addLog("W", "Houdini64", "Legacy 32-bit ARMv7 yerel kütüphaneleri tespit edildi (${nativeLibs.joinToString()}). 64-bit ikili çeviri köprüsü bağlandı.")
            } else {
                addLog("I", "ART", "Uygulama 64-bit yerel mimaride optimize edildi.")
            }
            addLog("I", "PackageManager", "Paket $packageName başarıyla Android 9 sistemine kuruldu.")

            // Distinctive pastel colors for app icons
            val iconColors = listOf(
                0xFF00E5FF, 0xFFFF4081, 0xFF7C4DFF, 0xFF00E676,
                0xFFFFAB00, 0xFF00B0FF, 0xFFE040FB, 0xFFFF5252
            )
            val randomColor = iconColors[Math.abs(packageName.hashCode()) % iconColors.size]
            val symbol = appName.firstOrNull()?.uppercase() ?: "A"

            val virtualApp = Android9VirtualApp(
                id = "app_${System.currentTimeMillis()}_${packageName.replace('.', '_')}",
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                isSystemApp = false,
                is32Bit = is32Bit,
                abi = abiDescription,
                targetSdk = targetSdk,
                installPath = targetBaseApk.absolutePath,
                installedAt = System.currentTimeMillis(),
                sourceApkPath = file.absolutePath,
                nativeLibs = nativeLibs.distinct(),
                activities = archiveInfo?.activities?.map { it.name } ?: listOf("$packageName.MainActivity"),
                permissions = archiveInfo?.requestedPermissions?.toList() ?: emptyList(),
                appIconColorHex = randomColor,
                appIconSymbol = symbol,
                category = if (isCloned) "Klonlanan 64-Bit" else "Aktarılan APK"
            )

            // Remove existing version if reinstalled
            installedApps.removeAll { it.packageName == packageName }
            installedApps.add(virtualApp)
            savePersistedApps(context)

            Result.success(virtualApp)
        } catch (e: Exception) {
            addLog("E", "PackageManager", "Kurulum hatası: ${e.message}")
            Result.failure(e)
        }
    }

    fun launchApp(app: Android9VirtualApp) {
        activeForegroundApp.value = app
        isInRecentsOverview.value = false
        isQuickSettingsOpen.value = false
        virtualAppActiveTab.value = 0
        virtualAppCounter.value = 0
        virtualAppJniResult.value = null

        val randomPid = 2800 + Random.nextInt(1200)
        val existingProc = runningProcesses.find { it.packageName == app.packageName }
        if (existingProc == null) {
            runningProcesses.add(
                Android9Process(
                    pid = randomPid,
                    packageName = app.packageName,
                    appName = app.appName,
                    memoryMb = 48 + Random.nextInt(64),
                    cpuPercent = 2.4f,
                    isRunning = true,
                    startedAt = System.currentTimeMillis(),
                    is32BitTranslated = app.is32Bit
                )
            )
        }

        addLog("I", "ActivityManager", "START u0 {act=android.intent.action.MAIN cat=[LAUNCHER] cmp=${app.packageName}/.MainActivity}")
        addLog("I", "ActivityManager", "Start proc ${app.packageName} pid=$randomPid uid=10082")
        if (app.is32Bit) {
            addLog("I", "Houdini64", "Uygulama 32-bit ikili çeviri motorunda çalıştırılıyor. ARMv7 -> ARM64 çevirici devrede.")
        } else {
            addLog("I", "art", "Uygulama 64-bit yerel ART sanal makinesinde yürütülüyor.")
        }
    }

    fun closeActiveApp() {
        val app = activeForegroundApp.value
        if (app != null) {
            addLog("I", "ActivityManager", "App backgrounded: ${app.packageName}")
        }
        activeForegroundApp.value = null
    }

    fun killProcess(packageName: String) {
        runningProcesses.removeAll { it.packageName == packageName }
        if (activeForegroundApp.value?.packageName == packageName) {
            activeForegroundApp.value = null
        }
        addLog("I", "ActivityManager", "Force stop $packageName (app killed)")
    }

    fun clearAllRecentTasks() {
        runningProcesses.removeAll { !it.packageName.startsWith("system_") && !it.packageName.startsWith("com.android.systemui") }
        activeForegroundApp.value = null
        isInRecentsOverview.value = false
        addLog("I", "ActivityManager", "All recent tasks cleared.")
    }

    fun uninstallApp(context: Context, app: Android9VirtualApp) {
        if (app.isSystemApp) {
            addLog("W", "PackageManager", "Sistem uygulamaları silinemez: ${app.packageName}")
            return
        }
        killProcess(app.packageName)
        installedApps.remove(app)
        try {
            File(app.installPath).parentFile?.deleteRecursively()
        } catch (_: Exception) {}
        savePersistedApps(context)
        addLog("I", "PackageManager", "Paket silindi: ${app.packageName}")
    }

    fun runShellCommand(command: String): String {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return ""
        addLog("D", "sh", "$ $trimmed")

        val parts = trimmed.split("\\s+".toRegex())
        val cmd = parts[0]
        val args = parts.drop(1)

        return when (cmd) {
            "help" -> """
                Android 9 Pie (API 28) 64-Bit Shell Komutları:
                  uname -a         : Çekirdek ve mimari bilgisi
                  getprop [prop]   : Sistem build özelliklerini listeler
                  cat /proc/cpuinfo: 8-çekirdek ARM64 CPU donanım detayları
                  pm list packages : Kurulu sanal paketleri listeler
                  pm install <apk> : Belirtilen APK'yı sisteme kurar
                  ps -ef           : Çalışan süreçleri listeler
                  top              : Anlık CPU ve RAM tablosu
                  logcat           : Canlı sistem loglarını görüntüler
                  df -h            : Disk ve depolama alanı durumu
                  free -m          : Sanal RAM bellek durumu
                  clear            : Ekranı temizler
            """.trimIndent()

            "uname" -> {
                if (args.contains("-a")) {
                    "Linux localhost 4.9.148-android9-64-pie-qemu #1 SMP PREEMPT Thu Aug 1 12:00:00 UTC 2019 aarch64 Android"
                } else if (args.contains("-m")) {
                    "aarch64"
                } else {
                    "Linux"
                }
            }

            "getprop" -> {
                val propKey = args.firstOrNull()
                val props = mapOf(
                    "ro.build.version.release" to "9",
                    "ro.build.version.sdk" to "28",
                    "ro.product.model" to "Android 9 Pie Virtual 64-Bit Machine",
                    "ro.product.cpu.abi" to "arm64-v8a",
                    "ro.product.cpu.abilist" to "arm64-v8a,armeabi-v7a,armeabi",
                    "ro.product.cpu.abilist64" to "arm64-v8a",
                    "ro.product.cpu.abilist32" to "armeabi-v7a,armeabi",
                    "persist.sys.translation" to "houdini64_arm32",
                    "ro.dalvik.vm.isa.arm" to "arm64",
                    "ro.build.id" to "PQ3A.190801.002",
                    "ro.boot.hardware" to "qemu_arm64",
                    "gsm.network.type" to "LTE"
                )
                if (propKey != null) {
                    props[propKey] ?: "[bilinmeyen özellik: $propKey]"
                } else {
                    props.entries.joinToString("\n") { "[${it.key}]: [${it.value}]" }
                }
            }

            "cat" -> {
                when (args.firstOrNull()) {
                    "/proc/cpuinfo" -> """
                        Processor       : AArch64 Processor rev 4 (aarch64)
                        processor       : 0
                        BogoMIPS        : 38.40
                        Features        : fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm
                        CPU implementer : 0x41
                        CPU architecture: 8
                        CPU variant     : 0x1
                        CPU part        : 0xd05
                        CPU revision    : 4
                        Hardware        : Android 9 Pie Virtual ARM64 Host
                        Revision        : 0000
                        Serial          : 9PIEVIRT000064BIT
                    """.trimIndent()
                    "/proc/version" -> "Linux version 4.9.148-android9-64-pie (gcc version 4.9.x) #1 SMP PREEMPT 2019"
                    else -> "cat: ${args.firstOrNull() ?: ""}: Böyle bir dosya veya dizin yok"
                }
            }

            "pm" -> {
                if (args.firstOrNull() == "list" && args.getOrNull(1) == "packages") {
                    installedApps.joinToString("\n") {
                        val prefix = if (it.isSystemApp) "package(sys):" else "package:"
                        val abiBadge = if (it.is32Bit) "[32bit-houdini]" else "[64bit-native]"
                        "$prefix${it.packageName}  $abiBadge"
                    }
                } else {
                    "Kullanım: pm list packages | pm install <apk_yolu>"
                }
            }

            "ps" -> {
                buildString {
                    appendLine("USER           PID   PPID  VSIZE  RSS   WCHAN            PC  NAME")
                    appendLine("root             1      0  14120  1840  sys_epoll_wait    0  /init")
                    appendLine("root           450      1  24800  2100  sys_epoll_wait    0  /system/bin/servicemanager")
                    appendLine("system         820      1 128400 14200  binder_ioctl      0  /system/bin/surfaceflinger")
                    appendLine("root          1020      1 245000 32000  sigsuspend        0  zygote64")
                    runningProcesses.forEach { proc ->
                        appendLine("u0_a84        ${proc.pid}   1020 412000 ${proc.memoryMb * 1024}  binder_ioctl      0  ${proc.packageName}")
                    }
                }
            }

            "df" -> """
                Filesystem     1K-blocks      Used Available Use% Mounted on
                /dev/root        3096280   1840200   1256080  60% /system
                /dev/block/vdc  65800000  14200000  51600000  22% /data
                tmpfs            2048000       420   2047580   1% /dev
                /sdcard         65800000  14200000  51600000  22% /storage/emulated/0
            """.trimIndent()

            "free" -> """
                         total        used        free      shared     buffers
                Mem:          4096        1420        2676          24          85
                Swap:         2048           0        2048
            """.trimIndent()

            "clear" -> "__CLEAR__"

            else -> "$cmd: komut bulunamadı. Desteklenen komutlar için 'help' yazın."
        }
    }

    private fun savePersistedApps(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val array = JSONArray()
            installedApps.filter { !it.isSystemApp }.forEach { app ->
                val obj = JSONObject().apply {
                    put("id", app.id)
                    put("packageName", app.packageName)
                    put("appName", app.appName)
                    put("versionName", app.versionName)
                    put("versionCode", app.versionCode)
                    put("is32Bit", app.is32Bit)
                    put("abi", app.abi)
                    put("targetSdk", app.targetSdk)
                    put("installPath", app.installPath)
                    put("installedAt", app.installedAt)
                    put("sourceApkPath", app.sourceApkPath ?: "")
                    put("nativeLibs", JSONArray(app.nativeLibs))
                    put("activities", JSONArray(app.activities))
                    put("permissions", JSONArray(app.permissions))
                    put("appIconColorHex", app.appIconColorHex)
                    put("appIconSymbol", app.appIconSymbol)
                    put("category", app.category)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_INSTALLED_APPS, array.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadPersistedApps(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_INSTALLED_APPS, null) ?: return
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val installPath = obj.getString("installPath")
                if (File(installPath).exists()) {
                    val nativeLibs = mutableListOf<String>()
                    val natArr = obj.optJSONArray("nativeLibs")
                    if (natArr != null) {
                        for (j in 0 until natArr.length()) nativeLibs.add(natArr.getString(j))
                    }
                    val activities = mutableListOf<String>()
                    val actArr = obj.optJSONArray("activities")
                    if (actArr != null) {
                        for (j in 0 until actArr.length()) activities.add(actArr.getString(j))
                    }

                    val app = Android9VirtualApp(
                        id = obj.getString("id"),
                        packageName = obj.getString("packageName"),
                        appName = obj.getString("appName"),
                        versionName = obj.optString("versionName", "1.0"),
                        versionCode = obj.optLong("versionCode", 1L),
                        isSystemApp = false,
                        is32Bit = obj.optBoolean("is32Bit", false),
                        abi = obj.optString("abi", "arm64-v8a"),
                        targetSdk = obj.optInt("targetSdk", 28),
                        installPath = installPath,
                        installedAt = obj.optLong("installedAt", System.currentTimeMillis()),
                        sourceApkPath = obj.optString("sourceApkPath", null),
                        nativeLibs = nativeLibs,
                        activities = activities,
                        appIconColorHex = obj.optLong("appIconColorHex", 0xFF00E5FF),
                        appIconSymbol = obj.optString("appIconSymbol", "A"),
                        category = obj.optString("category", "Aktarılan APK")
                    )
                    installedApps.add(app)
                }
            }
        } catch (_: Exception) {}
    }
}

package com.example.compat

object AdbScriptGenerator {

    data class CommandItem(
        val title: String,
        val category: String,
        val command: String,
        val description: String,
        val isRootRequired: Boolean
    )

    fun getRecommendedCommands(packageName: String = "com.example.app", apkFileName: String = "app.apk"): List<CommandItem> {
        return listOf(
            CommandItem(
                title = "Android 14+ TargetSDK Blokajını Aşarak Yükle",
                category = "ADB / Shizuku",
                command = "adb install --bypass-low-target-sdk-block -g $apkFileName",
                description = "Android 14, 15 ve 16'da eski 32-bit uygulamaların 'INSTALL_FAILED_DEPRECATED_SDK_VERSION' hatasıyla engellenmesini atlar.",
                isRootRequired = false
            ),
            CommandItem(
                title = "32-Bit ABI Zorlamalı Yükleme",
                category = "ADB / Shizuku",
                command = "adb install --abi armeabi-v7a $apkFileName",
                description = "Cihazınızda 32-bit kütüphane desteği varsa, uygulamanın yalnızca armeabi-v7a dilimini yüklemesini zorunlu kılar.",
                isRootRequired = false
            ),
            CommandItem(
                title = "Tango İkili Çevirici (Binary Translator) Servis Başlatma",
                category = "Tango Engine",
                command = "su -c '/data/adb/modules/tango/tango_server --inject-pkg $packageName'",
                description = "Google Pixel 7/8/9 gibi 64-bit donanımlı cihazlarda Tango dinamik ikili çevirici katmanını hedef paket için etkinleştirir.",
                isRootRequired = true
            ),
            CommandItem(
                title = "Shizuku (Kablosuz Hata Ayıklama) Tek Satır Yükleyici",
                category = "PC'siz Kurulum",
                command = "sh /sdcard/Download/$apkFileName --install-via-rish",
                description = "Bilgisayara gerek duymadan Shizuku veya Rish terminali üzerinden telefonunuzda doğrudan kurulum yapar.",
                isRootRequired = false
            ),
            CommandItem(
                title = "32-Bit Uygulamayı Hata Ayıklama Modunda Başlat",
                category = "Çalışma Zamanı",
                command = "adb shell am start -n $packageName/.MainActivity",
                description = "Uyumluluk katmanı uygulandıktan sonra uygulamayı ana etkinlik üzerinden doğrudan çalıştırır.",
                isRootRequired = false
            ),
            CommandItem(
                title = "Dalvik 64-Bit Linker Çökme Korumasını Aç",
                category = "Gelişmiş Ayarlar",
                command = "adb shell setprop debug.ld.all 1 && adb shell setprop persist.sys.dalvik.vm.libcore.compat true",
                description = "Bağlayıcı (linker) hatalarını ayrıntılı logcat'e döker ve eski API uyumluluk bayrağını açar.",
                isRootRequired = true
            )
        )
    }
}

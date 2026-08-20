package com.datasys.cooltrack.core

import platform.Foundation.NSBundle

// Estos valores se leen del Info.plist de la app iOS, poblado desde un
// archivo Config.xcconfig (no versionado) — equivalente a .env.local.
private fun plistValue(key: String): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: ""

actual object AppConfig {
    actual val supabaseUrl: String = plistValue("SUPABASE_URL")
        .ifEmpty { "https://ycblykplwavtrmhggmgf.supabase.co" }
    actual val supabaseAnonKey: String = plistValue("SUPABASE_ANON_KEY")
    actual val cloudinaryApiKey: String = plistValue("CLOUDINARY_API_KEY")
        .ifEmpty { "XMNOpC8RFvJPVCsefOmGAM5kUQU" }
    actual val cloudinaryCloudName: String = plistValue("CLOUDINARY_CLOUD_NAME")
    actual val cloudinaryUploadPreset: String = plistValue("CLOUDINARY_UPLOAD_PRESET")
    actual val apiBaseUrl: String = ApiConfig.BASE_URL_IOS_SIMULATOR
}

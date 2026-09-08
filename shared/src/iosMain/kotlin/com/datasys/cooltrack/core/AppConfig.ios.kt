package com.datasys.cooltrack.core

import platform.Foundation.NSBundle

// Estos valores se leen del Info.plist de la app iOS, poblado desde un
// archivo Config.xcconfig (NO versionado) — equivalente a .env.local.
// No se dejan valores hardcodeados de respaldo: si falta la config, la app
// falla ruidosamente en dev en vez de embeber secretos en el binario.
private fun plistValue(key: String): String =
    NSBundle.mainBundle.objectForInfoDictionaryKey(key) as? String ?: ""

actual object AppConfig {
    actual val supabaseUrl: String = plistValue("SUPABASE_URL")
    actual val supabaseAnonKey: String = plistValue("SUPABASE_ANON_KEY")
    actual val cloudinaryCloudName: String = plistValue("CLOUDINARY_CLOUD_NAME")
    actual val cloudinaryUploadPreset: String = plistValue("CLOUDINARY_UPLOAD_PRESET")
    actual val apiBaseUrl: String = ApiConfig.BASE_URL_IOS_SIMULATOR

    // En iOS se deja en false (no hay logs de red que ocultar en la app
    // publicada); si querés logs en dev, ponelo en true a mano localmente.
    actual val isDebug: Boolean = false
}

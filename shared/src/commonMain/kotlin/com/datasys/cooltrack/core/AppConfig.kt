package com.datasys.cooltrack.core

/**
 * Reemplaza a flutter_dotenv (.env.local). Cada plataforma provee estos
 * valores desde su propio mecanismo seguro de configuración:
 *  - Android: BuildConfig fields generados desde local.properties / CI secrets
 *  - iOS: xcconfig / variables de entorno del esquema de Xcode
 */
expect object AppConfig {
    val supabaseUrl: String
    val supabaseAnonKey: String
    val cloudinaryApiKey: String
    val cloudinaryCloudName: String
    val cloudinaryUploadPreset: String
    val apiBaseUrl: String
}

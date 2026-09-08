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
    // Subida de imágenes a Cloudinary con "unsigned upload preset": el
    // cliente NO necesita (ni debe llevar) la API key / secret de Cloudinary.
    val cloudinaryCloudName: String
    val cloudinaryUploadPreset: String
    val apiBaseUrl: String

    /** true solo en builds de desarrollo — apaga logs de red en release. */
    val isDebug: Boolean
}

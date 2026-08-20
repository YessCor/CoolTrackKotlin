package com.datasys.cooltrack.core

import com.datasys.cooltrack.shared.BuildConfig

actual object AppConfig {
    actual val supabaseUrl: String = BuildConfig.SUPABASE_URL
    actual val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY
    actual val cloudinaryApiKey: String = BuildConfig.CLOUDINARY_API_KEY
    actual val cloudinaryCloudName: String = BuildConfig.CLOUDINARY_CLOUD_NAME
    actual val cloudinaryUploadPreset: String = BuildConfig.CLOUDINARY_UPLOAD_PRESET
    actual val apiBaseUrl: String = ApiConfig.BASE_URL_ANDROID_EMULATOR
}

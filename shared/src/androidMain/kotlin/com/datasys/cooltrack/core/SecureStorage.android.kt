package com.datasys.cooltrack.core

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Debe inicializarse una vez desde Application.onCreate():
 *   SecureStorageInitializer.init(applicationContext)
 * (equivalente a que FlutterSecureStorage no necesite init explícito, pero en
 * Android puro sí necesitamos el Context para crear las EncryptedSharedPreferences).
 */
object SecureStorageInitializer {
    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}

private val prefs by lazy {
    val masterKey = MasterKey.Builder(SecureStorageInitializer.appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    EncryptedSharedPreferences.create(
        SecureStorageInitializer.appContext,
        "cooltrack_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}

actual object SecureStorage {
    actual suspend fun read(key: String): String? = prefs.getString(key, null)

    actual suspend fun write(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual suspend fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }
}

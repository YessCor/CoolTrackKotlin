package com.datasys.cooltrack.core

/**
 * Reemplaza flutter_secure_storage.
 *  - Android: EncryptedSharedPreferences (vía multiplatform-settings + Tink)
 *  - iOS: Keychain (vía multiplatform-settings KeychainSettings)
 */
expect object SecureStorage {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
    suspend fun delete(key: String)
}

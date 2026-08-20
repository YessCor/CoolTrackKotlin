package com.datasys.cooltrack.core

import platform.Security.*
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.dataUsingEncoding
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSString.Companion
import kotlinx.cinterop.*

/**
 * Wrapper simple sobre el Keychain de iOS (equivalente al backend iOS de
 * flutter_secure_storage). Usa kSecClassGenericPassword.
 */
@OptIn(ExperimentalForeignApi::class)
private fun keychainQuery(key: String): CFMutableDictionaryRef {
    val query = CFDictionaryCreateMutable(null, 0, null, null)
    CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
    CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(key as NSString))
    return query!!
}

@OptIn(ExperimentalForeignApi::class)
actual object SecureStorage {
    actual suspend fun read(key: String): String? {
        val query = keychainQuery(key)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) return null
            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            return NSString.create(data, NSUTF8StringEncoding) as String?
        }
    }

    actual suspend fun write(key: String, value: String) {
        delete(key)
        val query = keychainQuery(key)
        val valueData = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(valueData))
        SecItemAdd(query, null)
    }

    actual suspend fun delete(key: String) {
        val query = keychainQuery(key)
        SecItemDelete(query)
    }
}

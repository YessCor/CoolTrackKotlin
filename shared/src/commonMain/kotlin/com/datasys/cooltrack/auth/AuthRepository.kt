package com.datasys.cooltrack.auth

import com.datasys.cooltrack.core.ApiClient
import com.datasys.cooltrack.core.ApiException
import com.datasys.cooltrack.core.SecureStorage
import com.datasys.cooltrack.core.StorageKeys
import com.datasys.cooltrack.core.UserRole
import com.datasys.cooltrack.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Equivalente a AuthState en lib/providers/auth_provider.dart */
data class AuthState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isInitialized: Boolean = false,
) {
    val isAuthenticated: Boolean get() = user != null
    val role: UserRole? get() = user?.role
    val isAdmin: Boolean get() = user?.isAdmin ?: false
    val isTechnician: Boolean get() = user?.isTechnician ?: false
    val isClient: Boolean get() = user?.isClient ?: false
}

/**
 * Equivalente a AuthNotifier (StateNotifier<AuthState> de Riverpod).
 * En KMP no dependemos de un framework de estado específico: exponemos un
 * StateFlow que tanto un ViewModel de Android como un ObservableObject-style
 * wrapper de iOS pueden observar por igual.
 *
 * Se instancia una única vez (equivalente al Provider global de Riverpod) y
 * se inyecta donde haga falta — con Koin, o simplemente como singleton, según
 * prefieras para el resto del proyecto.
 */
class AuthRepository {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    suspend fun init() {
        loadStoredAuth()
        _state.value = _state.value.copy(isInitialized = true)
    }

    private suspend fun loadStoredAuth() {
        try {
            val token = SecureStorage.read(StorageKeys.TOKEN_KEY)
            val userJson = SecureStorage.read(StorageKeys.USER_KEY)

            if (token != null && userJson != null) {
                ApiClient.setAuthToken(token)
                val user = ApiClient.json.decodeFromString(User.serializer(), userJson)
                _state.value = _state.value.copy(user = user)
            }
        } catch (e: Exception) {
            clearStorage()
        }
    }

    private suspend fun clearStorage() {
        SecureStorage.delete(StorageKeys.TOKEN_KEY)
        SecureStorage.delete(StorageKeys.USER_KEY)
        ApiClient.setAuthToken(null)
    }

    suspend fun login(email: String, password: String): Boolean {
        _state.value = _state.value.copy(isLoading = true, error = null)

        return try {
            val response = ApiClient.post(
                "/auth/login",
                buildJsonObject {
                    put("email", email)
                    put("password", password)
                },
            )

            val token = response["token"]?.let { it.toString().trim('"') }
            if (token == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Token no recibido")
                return false
            }

            val userJsonObj = response["user"]?.jsonObject
                ?: run {
                    _state.value = _state.value.copy(isLoading = false, error = "Usuario no recibido")
                    return false
                }
            val user = ApiClient.json.decodeFromJsonElement(User.serializer(), userJsonObj)

            SecureStorage.write(StorageKeys.TOKEN_KEY, token)
            SecureStorage.write(StorageKeys.USER_KEY, userJsonObj.toString())
            ApiClient.setAuthToken(token)

            _state.value = _state.value.copy(user = user, isLoading = false)
            true
        } catch (e: ApiException) {
            _state.value = _state.value.copy(isLoading = false, error = e.message)
            false
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = "Error de conexión")
            false
        }
    }

    suspend fun logout() {
        _state.value = _state.value.copy(isLoading = true)
        runCatching { ApiClient.post("/auth/logout") }
        clearStorage()
        _state.value = AuthState(isInitialized = true)
    }

    suspend fun register(email: String, password: String, name: String, phone: String? = null): Boolean {
        _state.value = _state.value.copy(isLoading = true, error = null)

        return try {
            val response = ApiClient.post(
                "/auth/register",
                buildJsonObject {
                    put("email", email)
                    put("password", password)
                    put("name", name)
                    phone?.let { put("phone", it) }
                },
            )

            val token = response["token"]?.let { it.toString().trim('"') }
            if (token == null) {
                _state.value = _state.value.copy(isLoading = false, error = "Token no recibido")
                return false
            }

            val userJsonObj = response["user"]!!.jsonObject
            val user = ApiClient.json.decodeFromJsonElement(User.serializer(), userJsonObj)

            SecureStorage.write(StorageKeys.TOKEN_KEY, token)
            SecureStorage.write(StorageKeys.USER_KEY, userJsonObj.toString())
            ApiClient.setAuthToken(token)

            _state.value = _state.value.copy(user = user, isLoading = false)
            true
        } catch (e: ApiException) {
            _state.value = _state.value.copy(isLoading = false, error = e.message)
            false
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = "Error de conexión")
            false
        }
    }

    suspend fun refreshUser(): Boolean {
        return try {
            val response = ApiClient.get("/users/me")
            val userJsonObj = response["data"]!!.jsonObject
            val user = ApiClient.json.decodeFromJsonElement(User.serializer(), userJsonObj)
            SecureStorage.write(StorageKeys.USER_KEY, userJsonObj.toString())
            _state.value = _state.value.copy(user = user)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}



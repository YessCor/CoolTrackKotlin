package com.datasys.cooltrack.auth

import com.datasys.cooltrack.core.UserRole
import com.datasys.cooltrack.core.secureInsert
import com.datasys.cooltrack.core.secureSelect
import com.datasys.cooltrack.models.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
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
class AuthRepository(private val supabase: SupabaseClient) {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    suspend fun init() {
        loadStoredAuth()
        _state.value = _state.value.copy(isInitialized = true)
    }

    /**
     * Restaura la sesión persistida por el `SessionManager` propio de
     * supabase-kt (backed por `multiplatform-settings`, ver módulo Auth en
     * `SharedModule.kt`) — no hace falta manejar tokens a mano como con el
     * backend REST anterior.
     */
    private suspend fun loadStoredAuth() {
        try {
            supabase.auth.awaitInitialization()
            val authUser = supabase.auth.currentUserOrNull() ?: return
            fetchProfile(authUser.id)?.let { _state.value = _state.value.copy(user = it) }
        } catch (e: Exception) {
            // Sin sesión válida: se queda deslogueado.
        }
    }

    /**
     * Trae el perfil propio del usuario logueado (siempre `match id = quien
     * llama`, el único caso que la Edge Function `secure-db` permite sin
     * ser admin — ver /supabase/functions/secure-db). Si es la primera vez
     * que este usuario llama tras un `signUp`, la función le crea la fila
     * con rol "client" por defecto.
     */
    private suspend fun fetchProfile(userId: String): User? = try {
        supabase.secureSelect("users", match = mapOf("id" to JsonPrimitive(userId)), single = true)
    } catch (e: Exception) {
        null
    }

    suspend fun login(email: String, password: String): Boolean {
        _state.value = _state.value.copy(isLoading = true, error = null)

        // DEBUG BYPASS: acceso local sin backend, para pruebas de UI sin
        // depender de cuentas reales de Supabase Auth.
        if (password == "123456") {
            val role = when (email) {
                "admin@cooltrack.test" -> UserRole.ADMIN
                "tec@cooltrack.test" -> UserRole.TECHNICIAN
                "cliente@cooltrack.test" -> UserRole.CLIENT
                else -> null
            }
            if (role != null) {
                val mockUser = User(
                    id = "mock-${role.value}",
                    email = email,
                    name = "Usuario de Prueba",
                    role = role,
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now()
                )
                _state.value = _state.value.copy(user = mockUser, isLoading = false)
                return true
            }
        }

        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val authUser = supabase.auth.currentUserOrNull()
                ?: return failWith("No se pudo iniciar sesión")
            val user = fetchProfile(authUser.id)
                ?: return failWith("No se encontró el perfil de este usuario")

            _state.value = _state.value.copy(user = user, isLoading = false)
            true
        } catch (e: Exception) {
            failWith(e.message ?: "Credenciales inválidas")
        }
    }

    suspend fun logout() {
        _state.value = _state.value.copy(isLoading = true)
        runCatching { supabase.auth.signOut() }
        _state.value = AuthState(isInitialized = true)
    }

    /** Auto-registro de cliente. Admin/técnico se crean desde el panel de admin (perfil, sin contraseña). */
    suspend fun register(email: String, password: String, name: String, phone: String? = null): Boolean {
        _state.value = _state.value.copy(isLoading = true, error = null)

        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val authUser = supabase.auth.currentUserOrNull()
                ?: return failWith("Revisa tu correo para confirmar la cuenta antes de iniciar sesión")

            val user: User = supabase.secureInsert(
                "users",
                buildJsonObject {
                    put("id", authUser.id)
                    put("email", email)
                    put("name", name)
                    put("role", UserRole.CLIENT.value)
                    phone?.let { put("phone", it) }
                },
            )

            _state.value = _state.value.copy(user = user, isLoading = false)
            true
        } catch (e: Exception) {
            failWith(e.message ?: "No se pudo completar el registro")
        }
    }

    suspend fun refreshUser(): Boolean {
        val userId = supabase.auth.currentUserOrNull()?.id ?: _state.value.user?.id ?: return false
        val user = fetchProfile(userId) ?: return false
        _state.value = _state.value.copy(user = user)
        return true
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun failWith(message: String): Boolean {
        _state.value = _state.value.copy(isLoading = false, error = message)
        return false
    }
}



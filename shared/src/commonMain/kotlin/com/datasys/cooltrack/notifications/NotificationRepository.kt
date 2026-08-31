package com.datasys.cooltrack.notifications

import com.datasys.cooltrack.core.UserRole
import com.datasys.cooltrack.core.secureInsert
import com.datasys.cooltrack.core.secureSelect
import com.datasys.cooltrack.core.secureUpdate
import com.datasys.cooltrack.models.AppNotification
import com.datasys.cooltrack.models.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Equivalente a notification_provider.dart:
 *  - `notificationProvider` (StreamProvider) -> `notifications: StateFlow<List<AppNotification>>`
 *  - `unreadNotificationsCountProvider` -> `unreadCount: StateFlow<Int>`
 *  - `NotificationService` (markAsRead / sendNotification) -> métodos de esta misma clase
 *
 * Riverpod exponía un Stream reactivo de Supabase Realtime; acá se arma el
 * mismo canal realtime y se colecta en un StateFlow para que cualquier UI lo
 * observe con `collectAsState()`, sin depender de un framework de estado.
 */
class NotificationRepository(
    private val supabase: SupabaseClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    val unreadCount: StateFlow<Int> = MutableStateFlow(0).also { unread ->
        scope.launch {
            notifications.map { list -> list.count { !it.isRead } }
                .collect { unread.value = it }
        }
    }.asStateFlow()

    private var started = false

    /** Arranca la suscripción realtime — llamar una vez al iniciar sesión. */
    fun start() {
        if (started) return
        started = true

        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        scope.launch {
            // Carga inicial (equivalente al primer valor emitido por el Stream de Supabase)
            runCatching { fetchOnce(userId) }

            val channel = supabase.realtime.channel("notifications-$userId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "notifications"
            }
            channel.subscribe()

            changeFlow.collect {
                runCatching { fetchOnce(userId) }
            }
        }
    }

    private suspend fun fetchOnce(userId: String) {
        val rows = supabase.from("notifications")
            .select(Columns.ALL) { filter { eq("user_id", userId) }; order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING) }
            .decodeList<AppNotification>()
        _notifications.value = rows
    }

    suspend fun markAsRead(id: String) {
        supabase.secureUpdate(
            "notifications",
            buildJsonObject { put("is_read", true) },
            match = mapOf("id" to JsonPrimitive(id)),
        )
    }

    suspend fun sendNotification(
        userId: String,
        title: String,
        message: String,
        orderId: String? = null,
        type: String? = null,
    ) {
        supabase.secureInsert<JsonObject>(
            "notifications",
            buildJsonObject {
                put("user_id", userId)
                put("title", title)
                put("message", message)
                orderId?.let { put("order_id", it) }
                type?.let { put("type", it) }
            },
        )
    }

    /** IDs de los usuarios con rol administrador (para enrutar alertas al admin). */
    suspend fun getAdminUserIds(): List<String> = try {
        supabase.secureSelect<List<User>>(
            "users",
            match = mapOf("role" to JsonPrimitive(UserRole.ADMIN.value)),
        ).mapNotNull { it.id }
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Enruta una notificación a todos los usuarios con un rol concreto.
     * Útil para alertar al admin en eventos clave sin saber su user_id de antemano.
     */
    suspend fun notifyRole(
        role: UserRole,
        title: String,
        message: String,
        orderId: String? = null,
        type: String? = null,
    ) {
        val ids: List<String> = when (role) {
            UserRole.ADMIN -> getAdminUserIds()
            UserRole.TECHNICIAN, UserRole.CLIENT -> notifyRoleUserIds(role)
        }
        ids.forEach { userId ->
            runCatching {
                sendNotification(userId, title, message, orderId, type)
            }
        }
    }

    private suspend fun notifyRoleUserIds(role: UserRole): List<String> = try {
        supabase.secureSelect<List<User>>(
            "users",
            match = mapOf("role" to JsonPrimitive(role.value)),
        ).mapNotNull { it.id }
    } catch (e: Exception) {
        emptyList()
    }
}

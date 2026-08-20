package com.datasys.cooltrack.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Equivalente a lib/core/constants.dart
 *
 * Las variables sensibles (SUPABASE_URL, SUPABASE_ANON_KEY, Cloudinary, etc.)
 * ya no se cargan con flutter_dotenv en runtime: en Kotlin Multiplatform se
 * inyectan en tiempo de compilación mediante `expect`/`actual` (ver AppConfig.kt
 * en androidMain / iosMain), leyendo BuildConfig en Android y un plist / xcconfig
 * en iOS. Así evitamos empaquetar el .env dentro del binario final.
 */
object ApiConfig {
    // Para el emulador de Android usa 10.0.2.2; para iOS usa localhost o la IP de tu Mac.
    const val BASE_URL_ANDROID_EMULATOR = "http://10.0.2.2:8080/api"
    const val BASE_URL_IOS_SIMULATOR = "http://localhost:8080/api"
}

// --- User Roles ---------------------------------------------------------

@Serializable
enum class UserRole(val value: String) {
    @SerialName("admin") ADMIN("admin"),
    @SerialName("technician") TECHNICIAN("technician"),
    @SerialName("client") CLIENT("client");

    companion object {
        fun fromValue(value: String): UserRole? = entries.find { it.value == value }
    }
}

// --- Order Status (sincronizado con el enum order_status de la DB) -----

@Serializable
enum class OrderStatus(val value: String, val label: String) {
    @SerialName("pending") PENDING("pending", "Pendiente"),
    @SerialName("assigned") ASSIGNED("assigned", "Asignado"),
    @SerialName("accepted") ACCEPTED("accepted", "Aceptado"),
    @SerialName("in_transit") IN_TRANSIT("in_transit", "En Camino"),
    @SerialName("in_progress") IN_PROGRESS("in_progress", "En Progreso"),
    @SerialName("completed") COMPLETED("completed", "Completado"),
    @SerialName("cancelled") CANCELLED("cancelled", "Cancelado");

    companion object {
        fun fromValue(value: String): OrderStatus? =
            entries.find { it.value == value.lowercase() }
    }
}

/** Transiciones permitidas por rol (equivalente a `allowedTransitions`). */
val allowedTransitions: Map<UserRole, List<OrderStatus>> = mapOf(
    UserRole.ADMIN to listOf(OrderStatus.ASSIGNED, OrderStatus.CANCELLED),
    UserRole.TECHNICIAN to listOf(
        OrderStatus.ACCEPTED,
        OrderStatus.IN_TRANSIT,
        OrderStatus.IN_PROGRESS,
        OrderStatus.COMPLETED,
    ),
    UserRole.CLIENT to emptyList(),
)

/** Siguiente estado del técnico en el flujo lineal (equivalente a `technicianNextStatus`). */
val technicianNextStatus: Map<OrderStatus, OrderStatus> = mapOf(
    OrderStatus.ASSIGNED to OrderStatus.ACCEPTED,
    OrderStatus.ACCEPTED to OrderStatus.IN_TRANSIT,
    OrderStatus.IN_TRANSIT to OrderStatus.IN_PROGRESS,
    OrderStatus.IN_PROGRESS to OrderStatus.COMPLETED,
)

// --- Equipment Types ------------------------------------------------------

@Serializable
enum class EquipmentType(val value: String, val label: String) {
    @SerialName("split") SPLIT("split", "Split"),
    @SerialName("central") CENTRAL("central", "Central"),
    @SerialName("mini_split") MINI_SPLIT("mini_split", "Mini Split"),
    @SerialName("chiller") CHILLER("chiller", "Chiller"),
    @SerialName("fan_coil") FAN_COIL("fan_coil", "Fan Coil"),
    @SerialName("other") OTHER("other", "Otro");

    companion object {
        fun fromValue(value: String): EquipmentType? =
            entries.find { it.value == value.lowercase() } ?: OTHER
    }
}

// --- Quote Status -----------------------------------------------------

@Serializable
enum class QuoteStatus(val value: String, val label: String) {
    @SerialName("draft") DRAFT("draft", "Borrador"),
    @SerialName("sent") SENT("sent", "Enviado"),
    @SerialName("approved") APPROVED("approved", "Aprobado"),
    @SerialName("rejected") REJECTED("rejected", "Rechazado"),
    @SerialName("expired") EXPIRED("expired", "Expirado");

    companion object {
        fun fromValue(value: String): QuoteStatus? =
            entries.find { it.value == value.lowercase() }
    }
}

// --- Notification Types ------------------------------------------------

@Serializable
enum class NotificationType(val value: String) {
    @SerialName("order") ORDER("order"),
    @SerialName("quote") QUOTE("quote"),
    @SerialName("info") INFO("info"),
    @SerialName("alert") ALERT("alert");

    companion object {
        fun fromValue(value: String): NotificationType =
            entries.find { it.value == value } ?: INFO
    }
}

// --- Storage keys --------------------------------------------------------

object StorageKeys {
    const val TOKEN_KEY = "auth_token"
    const val USER_KEY = "user_data"
}

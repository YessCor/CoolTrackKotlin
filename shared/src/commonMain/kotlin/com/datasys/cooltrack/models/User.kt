package com.datasys.cooltrack.models

import com.datasys.cooltrack.core.UserRole
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Equivalente a lib/models/user.dart */
@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val phone: String? = null,
    val role: UserRole = UserRole.CLIENT,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
    val isTechnician: Boolean get() = role == UserRole.TECHNICIAN
    val isClient: Boolean get() = role == UserRole.CLIENT
}

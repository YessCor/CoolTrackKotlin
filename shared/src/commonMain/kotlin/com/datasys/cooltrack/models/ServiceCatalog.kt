package com.datasys.cooltrack.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Equivalente a lib/models/service_catalog.dart */
@Serializable
data class ServiceCatalog(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    @SerialName("base_price") val basePrice: Double,
    val unit: String = "servicio",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Instant,
)

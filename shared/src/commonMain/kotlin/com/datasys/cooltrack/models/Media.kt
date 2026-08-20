package com.datasys.cooltrack.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Equivalente a lib/models/media.dart */
@Serializable
data class Media(
    val id: String,
    val url: String,
    @SerialName("public_id") val publicId: String,
    @SerialName("resource_type") val resourceType: String,
    val format: String? = null,
    val bytes: Int? = null,
    @SerialName("uploaded_by") val uploadedBy: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("equipment_id") val equipmentId: String? = null,
    val context: String? = null,
    val caption: String? = null,
    @SerialName("created_at") val createdAt: Instant,
)

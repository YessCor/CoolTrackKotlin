package com.datasys.cooltrack.models

import com.datasys.cooltrack.core.OrderStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Equivalente a lib/models/service_order.dart */
@Serializable
data class ServiceOrder(
    val id: String,
    @SerialName("order_number") val orderNumber: Int,
    @SerialName("client_id") val clientId: String,
    @SerialName("technician_id") val technicianId: String? = null,
    @SerialName("equipment_id") val equipmentId: String? = null,
    val status: OrderStatus = OrderStatus.PENDING,
    val priority: String = "normal",
    @SerialName("service_type") val serviceType: String = "maintenance",
    val description: String = "",
    @SerialName("scheduled_date") val scheduledDate: Instant? = null,
    @SerialName("started_at") val startedAt: Instant? = null,
    @SerialName("completed_at") val completedAt: Instant? = null,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("client_signature_url") val clientSignatureUrl: String? = null,
    @SerialName("technician_notes") val technicianNotes: String? = null,
    @SerialName("client_rating") val clientRating: Int? = null,
    @SerialName("client_feedback") val clientFeedback: String? = null,
    @SerialName("total_amount") val totalAmount: Double? = null,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
) {
    val statusLabel: String get() = status.label

    val isPending: Boolean get() = status == OrderStatus.PENDING
    val isAssigned: Boolean get() = status == OrderStatus.ASSIGNED
    val isInProgress: Boolean get() = status == OrderStatus.IN_PROGRESS || status == OrderStatus.IN_TRANSIT
    val isCompleted: Boolean get() = status == OrderStatus.COMPLETED
    val isCancelled: Boolean get() = status == OrderStatus.CANCELLED
}

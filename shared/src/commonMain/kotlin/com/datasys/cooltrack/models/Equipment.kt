package com.datasys.cooltrack.models

import com.datasys.cooltrack.core.EquipmentType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Equivalente a lib/models/equipment.dart */
@Serializable
data class Equipment(
    val id: String,
    @SerialName("client_id") val clientId: String,
    val name: String,
    val type: EquipmentType = EquipmentType.OTHER,
    val brand: String? = null,
    val model: String? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
    @SerialName("capacity_tons") val capacityTons: Double? = null,
    @SerialName("installation_date") val installationDate: LocalDate? = null,
    @SerialName("last_service_date") val lastServiceDate: LocalDate? = null,
    @SerialName("location_description") val locationDescription: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
) {
    val typeLabel: String get() = type.label
}

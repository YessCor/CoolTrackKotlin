package com.datasys.cooltrack.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Equivalente a lib/models/technician_location.dart */
@Serializable
data class TechnicianLocation(
    val id: String,
    @SerialName("technician_id") val technicianId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    @SerialName("recorded_at") val recordedAt: Instant,
)

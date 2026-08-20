package com.datasys.cooltrack.services

import kotlinx.coroutines.flow.Flow

/** Equivalente a una posición de Geolocator.Position */
data class GeoPosition(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val heading: Double?,
    val speed: Double?,
)

/**
 * Reemplaza a geolocator. Cada plataforma implementa el flujo nativo de
 * ubicación: FusedLocationProviderClient en Android, CLLocationManager en iOS.
 */
expect class LocationProvider() {
    suspend fun hasPermission(): Boolean
    suspend fun requestPermission(): Boolean
    suspend fun isLocationServiceEnabled(): Boolean

    /** Stream de posiciones, actualiza cada ~50 metros (mismo distanceFilter que el original). */
    fun positionUpdates(distanceFilterMeters: Float = 50f): Flow<GeoPosition>
}

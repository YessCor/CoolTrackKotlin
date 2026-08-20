package com.datasys.cooltrack.location

import com.datasys.cooltrack.services.GeoPosition
import com.datasys.cooltrack.services.LocationProvider
import com.datasys.cooltrack.services.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Equivalente a LocationStatus (enum) en lib/providers/location_provider.dart */
enum class LocationStatus { UNKNOWN, ENABLED, DISABLED, DENIED }

/** Equivalente a LocationData.fromPosition(position) — mismo shape, ya en Kotlin. */
data class LocationData(val latitude: Double, val longitude: Double, val accuracy: Double?) {
    companion object {
        fun fromPosition(position: GeoPosition) =
            LocationData(position.latitude, position.longitude, position.accuracy)
    }
}

/** Equivalente a LocationState en lib/providers/location_provider.dart */
data class LocationUiState(
    val currentLocation: LocationData? = null,
    val status: LocationStatus = LocationStatus.UNKNOWN,
    val isTracking: Boolean = false,
    val error: String? = null,
)

/**
 * Equivalente a LocationNotifier (StateNotifier) en
 * lib/providers/location_provider.dart. Es la capa de "estado de UI" sobre
 * services/LocationService (que ya hace el trabajo pesado de permisos +
 * stream nativo + escritura a Supabase para el rol técnico).
 */
class LocationRepository(
    private val locationProvider: LocationProvider,
    private val locationService: LocationService,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    private var trackingJob: Job? = null

    suspend fun init() {
        val hasPermission = locationProvider.hasPermission()
        if (hasPermission) {
            _state.value = _state.value.copy(status = LocationStatus.ENABLED)
            getCurrentLocation()
        } else {
            _state.value = _state.value.copy(status = LocationStatus.DENIED)
        }
    }

    suspend fun getCurrentLocation() {
        try {
            // Toma la primera emisión del stream nativo como "posición actual",
            // equivalente a getCurrentPosition() de geolocator.
            var received = false
            locationProvider.positionUpdates().collect { position ->
                if (!received) {
                    received = true
                    _state.value = _state.value.copy(
                        currentLocation = LocationData.fromPosition(position),
                        status = LocationStatus.ENABLED,
                        error = null,
                    )
                }
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e.message)
        }
    }

    fun startTracking() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            val hasPermission = locationProvider.hasPermission()
            if (!hasPermission) {
                _state.value = _state.value.copy(status = LocationStatus.DENIED)
                return@launch
            }

            locationService.startTrackingTechnician()

            locationProvider.positionUpdates().collect { position ->
                _state.value = _state.value.copy(currentLocation = LocationData.fromPosition(position))
            }

            _state.value = _state.value.copy(isTracking = true, status = LocationStatus.ENABLED)
        }
    }

    fun stopTracking() {
        locationService.stopTracking()
        trackingJob?.cancel()
        trackingJob = null
        _state.value = _state.value.copy(isTracking = false)
    }

    /** Equivalente a isWithinRadius del original — cálculo de distancia con fórmula de Haversine. */
    fun isWithinRadius(lat: Double, lon: Double, radiusMeters: Double): Boolean {
        val current = _state.value.currentLocation ?: return false
        return haversineMeters(current.latitude, current.longitude, lat, lon) <= radiusMeters
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(toRadians(lat1)) * cos(toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun toRadians(degrees: Double): Double = degrees * kotlin.math.PI / 180.0

    fun dispose() {
        stopTracking()
    }
}

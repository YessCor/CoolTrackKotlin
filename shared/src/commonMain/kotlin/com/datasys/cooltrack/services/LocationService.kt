package com.datasys.cooltrack.services

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/** Fila a insertar en la tabla `technician_locations` de Supabase. */
@Serializable
private data class TechnicianLocationInsert(
    val technician_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double?,
    val heading: Double?,
    val speed: Double?,
    val recorded_at: String,
)

/**
 * Equivalente a lib/services/location_service.dart. Orquesta el
 * LocationProvider (expect/actual nativo) con la escritura en Supabase.
 */
class LocationService(
    private val locationProvider: LocationProvider,
    private val supabase: SupabaseClient,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    private var trackingJob: Job? = null

    suspend fun handlePermission(): Boolean {
        if (!locationProvider.isLocationServiceEnabled()) return false
        if (locationProvider.hasPermission()) return true
        return locationProvider.requestPermission()
    }

    fun startTrackingTechnician() {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            if (!handlePermission()) return@launch

            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch

            locationProvider.positionUpdates(distanceFilterMeters = 50f).collect { position ->
                runCatching {
                    supabase.from("technician_locations").insert(
                        TechnicianLocationInsert(
                            technician_id = userId,
                            latitude = position.latitude,
                            longitude = position.longitude,
                            accuracy = position.accuracy,
                            heading = position.heading,
                            speed = position.speed,
                            recorded_at = Clock.System.now().toString(),
                        )
                    )
                }
                // Los errores se ignoran silenciosamente, igual que el catch
                // vacío + print de debug del original en Dart.
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }
}

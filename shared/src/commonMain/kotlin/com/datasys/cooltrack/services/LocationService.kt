package com.datasys.cooltrack.services

import com.datasys.cooltrack.core.secureInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
                    supabase.secureInsert<JsonObject>(
                        "technician_locations",
                        buildJsonObject {
                            put("technician_id", userId)
                            put("latitude", position.latitude)
                            put("longitude", position.longitude)
                            position.accuracy?.let { put("accuracy", it) }
                            position.heading?.let { put("heading", it) }
                            position.speed?.let { put("speed", it) }
                            put("recorded_at", Clock.System.now().toString())
                        },
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

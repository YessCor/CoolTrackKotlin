package com.datasys.cooltrack.services

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.datasys.cooltrack.core.SecureStorageInitializer
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class LocationProvider actual constructor() {

    private val context get() = SecureStorageInitializer.appContext
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    actual suspend fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    // La solicitud real del permiso (diálogo del sistema) requiere una Activity;
    // se dispara desde la UI con ActivityResultContracts.RequestPermission()
    // y esta función queda como contrato/no-op documentado, igual que el
    // patrón usado en ImagePickerService.
    actual suspend fun requestPermission(): Boolean = hasPermission()

    actual suspend fun isLocationServiceEnabled(): Boolean {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    actual fun positionUpdates(distanceFilterMeters: Float): Flow<GeoPosition> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateDistanceMeters(distanceFilterMeters)
            .build()

        val callback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(
                        GeoPosition(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy.toDouble(),
                            heading = loc.bearing.toDouble(),
                            speed = loc.speed.toDouble(),
                        )
                    )
                }
            }
        }

        if (hasPermission()) {
            fusedClient.requestLocationUpdates(request, callback, null)
        }

        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }
}

package com.datasys.cooltrack.services

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject

actual class LocationProvider actual constructor() {

    private val manager = CLLocationManager()

    actual suspend fun hasPermission(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedAlways ||
            status == kCLAuthorizationStatusAuthorizedWhenInUse
    }

    actual suspend fun requestPermission(): Boolean {
        manager.requestWhenInUseAuthorization()
        return hasPermission()
    }

    actual suspend fun isLocationServiceEnabled(): Boolean =
        CLLocationManager.locationServicesEnabled()

    actual fun positionUpdates(distanceFilterMeters: Float): Flow<GeoPosition> = callbackFlow {
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = distanceFilterMeters.toDouble()

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val loc = didUpdateLocations.lastOrNull() as? platform.CoreLocation.CLLocation ?: return
                trySend(
                    GeoPosition(
                        latitude = loc.coordinate.useContents { latitude },
                        longitude = loc.coordinate.useContents { longitude },
                        accuracy = loc.horizontalAccuracy,
                        heading = loc.course,
                        speed = loc.speed,
                    )
                )
            }
        }

        manager.delegate = delegate
        manager.startUpdatingLocation()

        awaitClose { manager.stopUpdatingLocation() }
    }
}

package com.rescue.mesh.android.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Qu\u1ea3n l\u00fd GPS V\u1ec7 tinh s\u1eed d\u1ee5ng Google Fused Location Provider.
 *
 * C\u1eadp nh\u1eadt v\u1ecb tr\u00ed th\u1ef1c t\u1ebf m\u1ed7i 3 gi\u00e2y v\u1edbi \u0111\u1ed9 ch\u00ednh x\u00e1c cao nh\u1ea5t.
 */
class GpsLocationManager(private val context: Context) {

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    )

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(1f)
            .setWaitForAccurateLocation(true)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    trySend(LocationData(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy
                    ))
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }
}
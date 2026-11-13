package com.rescue.mesh.android.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Qu\u1ea3n l\u00fd c\u1ea3m bi\u1ebfn la b\u00e0n t\u1eeb t\u00ednh (Compass) v\u00e0 t\u00ednh G\u00f3c h\u01b0\u1edbng t\u1edbi Tr\u1ea1m C\u1ee9u H\u1ed9.
 *
 * D\u00f9ng k\u1ebft h\u1ee3p 2 c\u1ea3m bi\u1ebfn:
 *   - Accelerometer: \u0110o gia t\u1ed1c tr\u1ecdng l\u1ef1c.
 *   - MagneticField: \u0110o t\u1eeb tr\u01b0\u1eddng Tr\u00e1i \u0110\u1ea5t.
 *
 * T\u00ednh to\u00e1n:
 *   - Azimuth (\u0111\u1ed9 t\u1eeb h\u01b0\u1edbng B\u1eafc): Qua SensorManager.getOrientation().
 *   - Bearing (g\u00f3c h\u01b0\u1edbng t\u1edbi m\u1ee5c ti\u00eau): Qua Location.distanceBetween().
 */
class CompassSensorManager(context: Context) {

    data class CompassState(
        val azimuthDegrees: Float,
        val bearingToBaseStation: Float,
        val distanceMeters: Float
    )

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    fun getCompassUpdates(
        baseLat: Double, baseLng: Double,
        currentLat: Double, currentLng: Double
    ): Flow<CompassState> = callbackFlow {
        val gravity = FloatArray(3)
        val magnetic = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetic, 0, 3)
                    }
                }

                val rotationMatrix = FloatArray(9)
                val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, magnetic)
                if (success) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val normalizedAzimuth = (azimuth + 360) % 360

                    val results = FloatArray(2)
                    Location.distanceBetween(currentLat, currentLng, baseLat, baseLng, results)

                    trySend(CompassState(
                        azimuthDegrees = normalizedAzimuth,
                        bearingToBaseStation = results[1].let { (it + 360) % 360 },
                        distanceMeters = results[0]
                    ))
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
package com.rescue.mesh.android.hardware

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.*

/**
 * Qu\u1ea3n l\u00fd \u0110\u00e8n Flash LED ph\u00e1t t\u00edn hi\u1ec7u Morse SOS.
 *
 * M\u00e3 Morse SOS theo chu\u1ea9n qu\u1ed1c t\u1ebf:
 *   S = \u2022 \u2022 \u2022 (3 nh\u1ecbp ng\u1eafn)
 *   O = \u2013 \u2013 \u2013 (3 nh\u1ecbp d\u00e0i)
 *   S = \u2022 \u2022 \u2022 (3 nh\u1ecbp ng\u1eafn)
 */
class FlashlightManager(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (_: Exception) { null }

    private var morseJob: Job? = null
    private var isRunning = false

    fun toggleMorseSos(onStateChanged: (Boolean) -> Unit) {
        if (isRunning) {
            stopMorse()
            onStateChanged(false)
        } else {
            startMorse(onStateChanged)
        }
    }

    private fun startMorse(onStateChanged: (Boolean) -> Unit) {
        if (cameraId == null) return
        isRunning = true
        onStateChanged(true)

        morseJob = CoroutineScope(Dispatchers.IO).launch {
            val dotMs = 150L
            val dashMs = 450L
            val gapMs = 150L
            val letterGap = 450L
            val wordGap = 1050L

            while (isActive && isRunning) {
                // S: \u2022 \u2022 \u2022
                repeat(3) { flash(dotMs); delay(gapMs) }
                delay(letterGap)
                // O: \u2013 \u2013 \u2013
                repeat(3) { flash(dashMs); delay(gapMs) }
                delay(letterGap)
                // S: \u2022 \u2022 \u2022
                repeat(3) { flash(dotMs); delay(gapMs) }
                delay(wordGap)
            }
        }
    }

    private suspend fun flash(duration: Long) {
        try {
            cameraId?.let {
                cameraManager.setTorchMode(it, true)
                delay(duration)
                cameraManager.setTorchMode(it, false)
            }
        } catch (_: Exception) {}
    }

    private fun stopMorse() {
        isRunning = false
        morseJob?.cancel()
        morseJob = null
        try { cameraId?.let { cameraManager.setTorchMode(it, false) } } catch (_: Exception) {}
    }

    fun shutdown() { stopMorse() }
}
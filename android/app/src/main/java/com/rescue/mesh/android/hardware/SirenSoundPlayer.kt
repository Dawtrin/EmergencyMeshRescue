package com.rescue.mesh.android.hardware

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ph\u00e1t \u00e2m thanh c\u00f2i h\u00fa c\u1ee9u n\u1ea1n kh\u1ea9n c\u1ea5p.
 *
 * T\u1ea1o s\u00f3ng \u00e2m Sine wave v\u1edbi t\u1ea7n s\u1ed1 xoay v\u00f2ng 800Hz - 1500Hz (ki\u1ec3u c\u00f2i h\u00fa)
 * s\u1eed d\u1ee5ng AudioTrack API tr\u1ef1c ti\u1ebfp (kh\u00f4ng c\u1ea7n file \u00e2m thanh).
 */
class SirenSoundPlayer(private val context: Context) {

    private var sirenJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FREQ_LOW = 800.0
        private const val FREQ_HIGH = 1500.0
        private const val SWEEP_DURATION_MS = 1000L
    }

    fun toggleSiren(onStateChanged: (Boolean) -> Unit) {
        if (isPlaying) {
            stop()
            onStateChanged(false)
        } else {
            start(onStateChanged)
        }
    }

    private fun start(onStateChanged: (Boolean) -> Unit) {
        isPlaying = true
        onStateChanged(true)

        sirenJob = CoroutineScope(Dispatchers.IO).launch {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            val buffer = ShortArray(bufferSize / 2)

            var sampleIndex = 0L
            while (isActive && isPlaying) {
                for (i in buffer.indices) {
                    val totalSamples = (SWEEP_DURATION_MS * SAMPLE_RATE / 1000).toLong()
                    val phase = (sampleIndex % totalSamples).toDouble() / totalSamples
                    val freq = FREQ_LOW + (FREQ_HIGH - FREQ_LOW) * phase
                    buffer[i] = (sin(2 * PI * freq * sampleIndex / SAMPLE_RATE) * Short.MAX_VALUE).toInt().toShort()
                    sampleIndex++
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        }
    }

    private fun stop() {
        isPlaying = false
        sirenJob?.cancel()
        sirenJob = null
    }

    fun release() { stop() }
}
package com.rescue.mesh.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rescue.mesh.android.data.repository.MeshRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground Service chạy ngầm tiếp sóng Mesh khi màn hình tắt.
 *
 * Tích hợp:
 *   - Lắng nghe Socket Server nhận gói tin trên port relay.
 *   - Tự động phát Heartbeat định kỳ 30s/lần về Base Station.
 *   - Duy trì Wi-Fi Direct Peer Discovery.
 */
class MeshRelayService : Service() {

    companion object {
        const val CHANNEL_ID = "mesh_relay_channel"
        const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var repository: MeshRepository

    override fun onCreate() {
        super.onCreate()
        repository = MeshRepository.getInstance(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val profile = repository.userPreferences.userProfileFlow.first()
            repository.startListeningServer(profile.relayPort)
            repository.registerP2p()
            // Kích hoạt Heartbeat 30 giây/lần cho Relay Node
            repository.startHeartbeatLoop(30_000L)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        repository.stopHeartbeatLoop()
        repository.stopListeningServer()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ti\u1ebfp s\u00f3ng Mesh Rescue",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "D\u1ecbch v\u1ee5 ti\u1ebfp s\u00f3ng c\u1ee9u n\u1ea1n \u0111ang ho\u1ea1t \u0111\u1ed9ng ng\u1ea7m"
            }
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mesh Relay \u0111ang ti\u1ebfp s\u00f3ng")
            .setContentText("Tr\u1ea1m ti\u1ebfp s\u00f3ng c\u1ee9u n\u1ea1n \u0111ang ho\u1ea1t \u0111\u1ed9ng & g\u1eedi Heartbeat...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
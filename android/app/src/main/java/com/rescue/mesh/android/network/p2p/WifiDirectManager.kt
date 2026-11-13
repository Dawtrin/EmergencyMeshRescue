package com.rescue.mesh.android.network.p2p

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Trình quản lý mạng Tùy biến Không dây Wi-Fi Direct (P2P Mesh Network).
 *
 * Nhiệm vụ:
 *   - Khởi tạo Wi-Fi P2P Channel và đăng ký BroadcastReceiver.
 *   - Tự động Discover Peers (quét tìm thiết bị xung quanh trong vùng lũ).
 *   - Báo cáo danh sách các Node láng giềng thời gian thực.
 *   - Rung xúc giác double-bounce khi phát hiện thiết bị mới (P2P Handshake Haptic).
 */
class

WifiDirectManager(private val context: Context) {

    private val p2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

    data class P2pState(
        val isWifiP2pEnabled: Boolean = false,
        val discoveredPeers: List<WifiP2pDevice> = emptyList(),
        val isConnected: Boolean = false,
        val groupOwnerAddress: String? = null
    )

    private val _p2pState = MutableStateFlow(P2pState())
    val p2pState: StateFlow<P2pState> = _p2pState.asStateFlow()

    init {
        channel = p2pManager?.initialize(context, context.mainLooper, null)
    }

    /**
     * Rung xúc giác nhịp đôi nẩy nhẹ khi phát hiện peer mới (P2P Handshake).
     */
    private fun triggerPeerDiscoveryHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                // Double-bounce: ngắn - nghỉ - ngắn (P2P Handshake pattern)
                val timings = longArrayOf(0, 60, 80, 60)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        } catch (_: Exception) {}
    }

    /**
     * Đăng ký Receiver lắng nghe sự kiện Wi-Fi Direct.
     */
    @SuppressLint("MissingPermission")
    fun register() {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                        _p2pState.value = _p2pState.value.copy(isWifiP2pEnabled = isEnabled)
                    }

                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        p2pManager?.requestPeers(channel) { peerList ->
                            val newPeers = peerList.deviceList.toList()
                            val oldPeers = _p2pState.value.discoveredPeers

                            // Phát hiện peer mới -> rung haptic double-bounce
                            val newCount = newPeers.size - oldPeers.size
                            if (newCount > 0) {
                                triggerPeerDiscoveryHaptic()
                            }

                            _p2pState.value = _p2pState.value.copy(discoveredPeers = newPeers)
                        }
                    }

                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        p2pManager?.requestConnectionInfo(channel) { info ->
                            if (info != null && info.groupFormed) {
                                val groupOwnerIp = info.groupOwnerAddress?.hostAddress
                                _p2pState.value = _p2pState.value.copy(
                                    isConnected = true,
                                    groupOwnerAddress = groupOwnerIp
                                )
                            } else {
                                _p2pState.value = _p2pState.value.copy(
                                    isConnected = false,
                                    groupOwnerAddress = null
                                )
                            }
                        }
                    }
                }
            }
        }

        context.registerReceiver(receiver, intentFilter)
    }

    /**
     * Hủy đăng ký Receiver.
     */
    fun unregister() {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
        }
        receiver = null
    }

    /**
     * Bắt đầu quét tìm các thiết bị Mesh xung quanh.
     */
    @SuppressLint("MissingPermission")
    fun discoverPeers(onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
        p2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { onSuccess() }
            override fun onFailure(reason: Int) { onFailure(reason) }
        })
    }
}
package com.rescue.mesh.android.data.repository

import android.content.Context
import android.os.Build
import com.rescue.mesh.android.data.local.IncidentDatabase
import com.rescue.mesh.android.data.local.IncidentEntity
import com.rescue.mesh.android.data.local.UserPreferences
import com.rescue.mesh.android.data.model.MeshPacket
import com.rescue.mesh.android.domain.routing.RoutingEngine
import com.rescue.mesh.android.domain.util.PacketFactory
import com.rescue.mesh.android.hardware.GpsLocationManager
import com.rescue.mesh.android.network.p2p.WifiDirectManager
import com.rescue.mesh.android.network.socket.MeshSocketClient
import com.rescue.mesh.android.network.socket.MeshSocketServer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Repository Singleton điều phối toàn bộ hoạt động Mạng Mesh và Dữ liệu Cứu nạn.
 *
 * Pattern: Application-scoped Singleton đảm bảo chỉ có 1 instance
 * duy nhất quản lý kết nối Socket, RoutingEngine, IncidentDatabase
 * và trạng thái mạng — tránh xung đột giữa Activity và Foreground Service.
 */
class MeshRepository private constructor(
    private val context: Context,
    val userPreferences: UserPreferences,
) {
    companion object {
        @Volatile
        private var INSTANCE: MeshRepository? = null

        fun getInstance(context: Context): MeshRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeshRepository(
                    context.applicationContext,
                    UserPreferences(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }

    val deviceNodeId: String = "NODE_ANDROID_${Build.MODEL.replace(" ", "_")}"

    val gpsManager = GpsLocationManager(context)
    val wifiDirectManager = WifiDirectManager(context)

    private val routingEngine = RoutingEngine(deviceNodeId)
    private val socketClient = MeshSocketClient()
    private var socketServer: MeshSocketServer? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    // Room Database
    val incidentDatabase = IncidentDatabase.getInstance(context)
    val incidentDao = incidentDatabase.incidentDao()

    data class NetworkStats(
        val packetsSent: Int = 0,
        val packetsForwarded: Int = 0,
        val packetsReceived: Int = 0,
        val packetsDropped: Int = 0,
        val heartbeatsSent: Int = 0,
        val lastReceivedPacket: MeshPacket? = null,
        val lastSendResult: MeshSocketClient.SendResult? = null,
        val retryAttempts: Int = 0
    )

    private val _networkStats = MutableStateFlow(NetworkStats())
    val networkStats: StateFlow<NetworkStats> = _networkStats.asStateFlow()

    private val _activeDispatchCmd = MutableStateFlow<MeshPacket?>(null)
    val activeDispatchCmd: StateFlow<MeshPacket?> = _activeDispatchCmd.asStateFlow()

    private val _isSosBroadcasting = MutableStateFlow(false)
    val isSosBroadcasting: StateFlow<Boolean> = _isSosBroadcasting.asStateFlow()

    private val _sosHistory = MutableStateFlow<List<MeshPacket>>(emptyList())
    val sosHistory: StateFlow<List<MeshPacket>> = _sosHistory.asStateFlow()

    private var isP2pRegistered = false

    fun registerP2p() {
        if (!isP2pRegistered) {
            wifiDirectManager.register()
            isP2pRegistered = true
        }
    }

    fun startListeningServer(port: Int = 8889) {
        stopListeningServer()
        socketServer = MeshSocketServer(port) { packet ->
            handleIncomingPacket(packet)
        }
        socketServer?.start()
    }

    fun stopListeningServer() {
        socketServer?.stop()
        socketServer = null
    }

    /**
     * Khởi động vòng lặp gửi Heartbeat định kỳ giữ kết nối sống (30 giây/lần).
     */
    fun startHeartbeatLoop(intervalMs: Long = 30_000L) {
        stopHeartbeatLoop()
        heartbeatJob = repositoryScope.launch {
            while (isActive) {
                try {
                    val heartbeatPacket = PacketFactory.createHeartbeat(deviceNodeId)
                    val prepared = routingEngine.prepareOutgoingPacket(heartbeatPacket)
                    val profile = userPreferences.userProfileFlow.first()

                    val result = socketClient.sendPacket(
                        host = profile.baseStationIp,
                        port = profile.baseStationPort,
                        packet = prepared
                    )
                    if (result.success) {
                        _networkStats.update { it.copy(heartbeatsSent = it.heartbeatsSent + 1) }
                    }
                } catch (_: Exception) {}
                delay(intervalMs)
            }
        }
    }

    fun stopHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Xử lý gói tin đến qua RoutingEngine.
     * Hỗ trợ DeliverAndForward cho gói tin BROADCAST.
     */
    private fun handleIncomingPacket(packet: MeshPacket) {
        when (val decision = routingEngine.processIncomingPacket(packet)) {
            is RoutingEngine.RoutingDecision.DeliverLocal -> {
                deliverLocally(decision.packet)
            }
            is RoutingEngine.RoutingDecision.Forward -> {
                forwardPacket(decision.packet)
            }
            is RoutingEngine.RoutingDecision.DeliverAndForward -> {
                deliverLocally(decision.packet)
                forwardPacket(decision.packet)
            }
            is RoutingEngine.RoutingDecision.Drop -> {
                _networkStats.update { it.copy(packetsDropped = it.packetsDropped + 1) }
            }
        }
    }

    private fun deliverLocally(packet: MeshPacket) {
        _networkStats.update {
            it.copy(packetsReceived = it.packetsReceived + 1, lastReceivedPacket = packet)
        }
        if (packet.packetType == MeshPacket.TYPE_DISPATCH_CMD) {
            _activeDispatchCmd.value = packet
            // Lưu vào Room Database
            repositoryScope.launch {
                incidentDao.insertIncident(packet.toIncidentEntity(isOutgoing = false, sendSuccess = true))
            }
        }
    }

    private fun forwardPacket(packet: MeshPacket) {
        _networkStats.update { it.copy(packetsForwarded = it.packetsForwarded + 1) }
        repositoryScope.launch {
            val profile = userPreferences.userProfileFlow.first()
            socketClient.sendPacket(
                host = profile.baseStationIp,
                port = profile.baseStationPort,
                packet = packet
            )
        }
    }

    /**
     * Phát tín hiệu SOS khẩn cấp tới Base Station (Auto-Retry 3 lần).
     */
    suspend fun broadcastSos(
        alertType: String,
        severity: String,
        message: String,
        victimCount: Int,
        location: GpsLocationManager.LocationData?
    ): MeshSocketClient.SendResult {
        _isSosBroadcasting.value = true
        val profile = userPreferences.userProfileFlow.first()

        val sosPacket = PacketFactory.createSosPacket(
            sourceNodeId = deviceNodeId,
            senderName = profile.fullName,
            alertType = alertType,
            severity = severity,
            message = message.ifBlank { "Y\u00eau c\u1ea7u \u1ee9ng c\u1ee9u kh\u1ea9n c\u1ea5p!" },
            victimCount = victimCount,
            latitude = location?.latitude ?: 16.054407,
            longitude = location?.longitude ?: 108.202167
        )

        val preparedPacket = routingEngine.prepareOutgoingPacket(sosPacket)

        // Lưu vào Room trước khi gửi (để retry sau nếu thất bại)
        incidentDao.insertIncident(preparedPacket.toIncidentEntity(isOutgoing = true, sendSuccess = false))

        val result = socketClient.sendPacket(
            host = profile.baseStationIp,
            port = profile.baseStationPort,
            packet = preparedPacket
        )

        if (result.success) {
            _networkStats.update {
                it.copy(
                    packetsSent = it.packetsSent + 1,
                    retryAttempts = it.retryAttempts + result.attemptCount - 1
                )
            }
            _sosHistory.update { it + preparedPacket }
            incidentDao.markAsSent(preparedPacket.packetId)
        }
        _networkStats.update { it.copy(lastSendResult = result) }
        _isSosBroadcasting.value = false
        return result
    }

    /**
     * Gửi phản hồi ACK xác nhận đã nhận lệnh chỉ huy.
     */
    suspend fun sendAckForDispatch(dispatchPacket: MeshPacket): MeshSocketClient.SendResult {
        val ackPacket = PacketFactory.createAck(
            sourceNodeId = deviceNodeId,
            originalPacketId = dispatchPacket.packetId,
            destinationNodeId = dispatchPacket.sourceNodeId
        )
        val preparedAck = routingEngine.prepareOutgoingPacket(ackPacket)
        val profile = userPreferences.userProfileFlow.first()

        val result = socketClient.sendPacket(
            host = profile.baseStationIp,
            port = profile.baseStationPort,
            packet = preparedAck
        )

        _activeDispatchCmd.value = null
        return result
    }

    /**
     * Retry gửi lại một gói tin SOS thất bại cụ thể từ IncidentDatabase.
     */
    suspend fun retrySingleIncident(incident: IncidentEntity): MeshSocketClient.SendResult {
        val profile = userPreferences.userProfileFlow.first()
        val packet = MeshPacket(
            packetId = incident.packetId,
            packetType = incident.packetType,
            sourceNodeId = incident.sourceNodeId,
            destinationNodeId = incident.destinationNodeId,
            senderHopId = deviceNodeId,
            ttl = MeshPacket.DEFAULT_TTL,
            hopCount = 0,
            timestamp = incident.timestamp,
            payload = MeshPacket.Payload(
                senderName = incident.senderName,
                alertType = incident.alertType,
                message = incident.message,
                victimCount = incident.victimCount,
                severity = incident.severity,
                location = MeshPacket.Location(incident.latitude, incident.longitude)
            ),
            routeHistory = mutableListOf(deviceNodeId)
        ).signChecksum()

        val result = socketClient.sendPacket(profile.baseStationIp, profile.baseStationPort, packet)
        if (result.success) {
            incidentDao.markAsSent(incident.packetId)
            _networkStats.update { it.copy(packetsSent = it.packetsSent + 1) }
        }
        return result
    }

    /**
     * Retry gửi lại tất cả gói tin SOS thất bại (Store-Carry-Forward).
     */
    suspend fun retryFailedPackets() {
        val failed = incidentDao.getFailedOutgoing()
        for (incident in failed) {
            retrySingleIncident(incident)
        }
    }

    fun dismissDispatchAlert() {
        _activeDispatchCmd.value = null
    }

    fun cleanup() {
        stopHeartbeatLoop()
        stopListeningServer()
        if (isP2pRegistered) {
            wifiDirectManager.unregister()
            isP2pRegistered = false
        }
        routingEngine.shutdown()
    }

    private fun MeshPacket.toIncidentEntity(isOutgoing: Boolean, sendSuccess: Boolean): IncidentEntity {
        return IncidentEntity(
            packetId = packetId,
            packetType = packetType,
            sourceNodeId = sourceNodeId,
            destinationNodeId = destinationNodeId,
            senderName = payload?.senderName ?: "",
            alertType = payload?.alertType ?: "",
            message = payload?.message ?: "",
            severity = payload?.severity ?: "",
            victimCount = payload?.victimCount ?: 1,
            latitude = payload?.location?.latitude ?: 0.0,
            longitude = payload?.location?.longitude ?: 0.0,
            hopCount = hopCount,
            ttl = ttl,
            timestamp = timestamp,
            routeHistory = routeHistory.joinToString(","),
            isOutgoing = isOutgoing,
            sendSuccess = sendSuccess
        )
    }
}
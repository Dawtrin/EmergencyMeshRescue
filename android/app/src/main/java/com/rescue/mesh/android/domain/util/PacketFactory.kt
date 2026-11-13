package com.rescue.mesh.android.domain.util

import com.rescue.mesh.android.data.model.MeshPacket
import java.util.UUID

/**
 * Factory tạo các loại gói tin chuẩn của hệ thống.
 *
 * Áp dụng mô hình 3 loại thông điệp phân tán (Đề số 54):
 *   - SOS_DATA  (REQ): Nạn nhân gửi yêu cầu cứu nạn.
 *   - DISPATCH_CMD (ACQ): Trạm chỉ huy phản hồi lệnh điều phối.
 *   - ACK (REL): Xác nhận đã nhận và hoàn thành lệnh.
 *   - HEARTBEAT: Gói tin giữ kết nối sống, báo còn hoạt động.
 */
object PacketFactory {

    fun createSosPacket(
        sourceNodeId: String,
        senderName: String,
        alertType: String,
        severity: String,
        message: String,
        victimCount: Int,
        latitude: Double,
        longitude: Double
    ): MeshPacket {
        return MeshPacket(
            packetId = generateShortId(),
            packetType = MeshPacket.TYPE_SOS_DATA,
            sourceNodeId = sourceNodeId,
            destinationNodeId = MeshPacket.NODE_BASE_STATION,
            senderHopId = sourceNodeId,
            ttl = MeshPacket.DEFAULT_TTL,
            hopCount = 0,
            timestamp = System.currentTimeMillis(),
            payload = MeshPacket.Payload(
                senderName = senderName,
                alertType = alertType,
                message = message,
                victimCount = victimCount,
                severity = severity,
                location = MeshPacket.Location(latitude, longitude)
            ),
            routeHistory = mutableListOf(sourceNodeId)
        ).signChecksum()
    }

    fun createDispatchCmd(
        sourceNodeId: String,
        targetNodeId: String,
        commandMessage: String,
        senderName: String = "Base Station Commander"
    ): MeshPacket {
        return MeshPacket(
            packetId = generateShortId(),
            packetType = MeshPacket.TYPE_DISPATCH_CMD,
            sourceNodeId = sourceNodeId,
            destinationNodeId = targetNodeId,
            senderHopId = sourceNodeId,
            ttl = MeshPacket.DEFAULT_TTL,
            hopCount = 0,
            timestamp = System.currentTimeMillis(),
            payload = MeshPacket.Payload(
                senderName = senderName,
                message = commandMessage,
                severity = MeshPacket.SEVERITY_HIGH
            ),
            routeHistory = mutableListOf(sourceNodeId)
        ).signChecksum()
    }

    fun createAck(
        sourceNodeId: String,
        originalPacketId: String,
        destinationNodeId: String
    ): MeshPacket {
        return MeshPacket(
            packetId = generateShortId(),
            packetType = MeshPacket.TYPE_ACK,
            sourceNodeId = sourceNodeId,
            destinationNodeId = destinationNodeId,
            senderHopId = sourceNodeId,
            ttl = MeshPacket.DEFAULT_TTL,
            hopCount = 0,
            timestamp = System.currentTimeMillis(),
            payload = MeshPacket.Payload(
                message = "ACK for packet: $originalPacketId"
            ),
            routeHistory = mutableListOf(sourceNodeId)
        ).signChecksum()
    }

    fun createHeartbeat(sourceNodeId: String): MeshPacket {
        return MeshPacket(
            packetId = generateShortId(),
            packetType = MeshPacket.TYPE_HEARTBEAT,
            sourceNodeId = sourceNodeId,
            destinationNodeId = "BROADCAST",
            senderHopId = sourceNodeId,
            ttl = 2,
            hopCount = 0,
            timestamp = System.currentTimeMillis(),
            payload = MeshPacket.Payload(
                message = "Heartbeat from $sourceNodeId"
            ),
            routeHistory = mutableListOf(sourceNodeId)
        ).signChecksum()
    }

    private fun generateShortId(): String =
        UUID.randomUUID().toString().substring(0, 8)
}
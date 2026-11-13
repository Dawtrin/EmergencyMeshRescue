package com.rescue.mesh.android.data.model

import com.google.gson.Gson
import com.rescue.mesh.android.domain.util.ChecksumUtil
import java.util.UUID

/**
 * Cấu trúc gói tin chuẩn của hệ thống Emergency Mesh Rescue.
 *
 * TƯƠNG THÍCH 100% JSON với Phase 1 Desktop (MeshPacket.java).
 * Mọi field name giống hệt để Gson serialize/deserialize xuyên suốt.
 *
 * Cơ sở lý thuyết:
 *   - TTL (Time-To-Live): Lấy ý tưởng từ IPv4 Header (RFC 791).
 *   - routeHistory: Tương đương Source Route trong IPv6 (RFC 2460).
 *   - packetId (UUID): Định danh duy nhất để SeenPacketCache phát hiện duplicate.
 *   - checksum (SHA-256): Đảm bảo Data Integrity theo RFC 6234.
 */
data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString().substring(0, 8),
    val packetType: String = TYPE_SOS_DATA,
    val sourceNodeId: String = "",
    val destinationNodeId: String = "",
    var senderHopId: String = "",
    var ttl: Int = DEFAULT_TTL,
    var hopCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val payload: Payload? = null,
    val routeHistory: MutableList<String> = mutableListOf(),
    var checksum: String = ""
) {
    /** Nội dung chính của gói tin SOS/DISPATCH. */
    data class Payload(
        val senderName: String = "",
        val alertType: String = "",
        val message: String = "",
        val victimCount: Int = 1,
        val severity: String = SEVERITY_CRITICAL,
        val location: Location? = null
    )

    /** Tọa độ địa lý GPS của nạn nhân. */
    data class Location(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    )

    fun toJson(): String = GSON.toJson(this)

    fun computeChecksum(): String {
        val payloadJson = GSON.toJson(payload)
        return ChecksumUtil.sha256(payloadJson)
    }

    fun signChecksum(): MeshPacket {
        checksum = computeChecksum()
        return this
    }

    fun verifyChecksum(): Boolean {
        if (checksum.isEmpty()) return true
        return checksum == computeChecksum()
    }

    companion object {
        const val TYPE_SOS_DATA     = "SOS_DATA"
        const val TYPE_DISPATCH_CMD = "DISPATCH_CMD"
        const val TYPE_ACK          = "ACK"
        const val TYPE_HEARTBEAT    = "HEARTBEAT"

        const val SEVERITY_CRITICAL = "CRITICAL"
        const val SEVERITY_HIGH     = "HIGH"
        const val SEVERITY_MEDIUM   = "MEDIUM"

        const val ALERT_MEDICAL    = "MEDICAL"
        const val ALERT_FLOOD      = "FLOOD_TRAPPED"
        const val ALERT_LANDSLIDE  = "LANDSLIDE"
        const val ALERT_EARTHQUAKE = "EARTHQUAKE"

        const val NODE_BASE_STATION = "BASE_STATION"
        const val NODE_A_VICTIM     = "NODE_A_VICTIM"
        const val NODE_B_RELAY      = "NODE_B_RELAY"

        const val DEFAULT_TTL = 5

        private val GSON = Gson()

        fun fromJson(json: String): MeshPacket? {
            return try {
                GSON.fromJson(json, MeshPacket::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
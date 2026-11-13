package com.rescue.mesh.android.domain.routing

import com.rescue.mesh.android.data.model.MeshPacket

/**
 * Động cơ định tuyến luồng gói tin (Controlled Flooding Mesh Routing Engine).
 *
 * Cơ sở lý thuyết (Đề số 53 & 54):
 *   - Kiểm tra TTL (Time-To-Live, RFC 791): Nếu TTL <= 1 -> Drop gói tin.
 *   - Duplicate Filtering (SeenPacketCache): Nếu packetId đã từng thấy -> Drop.
 *   - Checksum Verification (SHA-256, RFC 6234): Nếu toàn vẹn dữ liệu bị hỏng -> Drop.
 *   - Hop Management & Source Routing (RFC 2460):
 *       + Giảm TTL đi 1, tăng hopCount lên 1.
 *       + Thêm nodeId vào routeHistory (Source Route).
 *       + Cập nhật senderHopId = nodeId hiện tại.
 *   - Broadcast Forwarding: Gói tin BROADCAST và HEARTBEAT luôn được forward
 *     ngoài việc deliver local, đảm bảo mọi node trong mạng đều nhận được.
 */
class RoutingEngine(
    val currentNodeId: String,
    private val packetCache: SeenPacketCache = SeenPacketCache()
) {

    sealed class RoutingDecision {
        /** Giao cho tầng ứng dụng xử lý nội bộ. */
        data class DeliverLocal(val packet: MeshPacket) : RoutingDecision()
        /** Chuyển tiếp tới các node láng giềng trong mạng Mesh. */
        data class Forward(val packet: MeshPacket) : RoutingDecision()
        /** Giao nội bộ VÀ đồng thời chuyển tiếp (dùng cho Broadcast). */
        data class DeliverAndForward(val packet: MeshPacket) : RoutingDecision()
        /** Hủy gói tin vì lý do hợp lệ. */
        data class Drop(val reason: String, val packet: MeshPacket) : RoutingDecision()
    }

    /**
     * Xử lý một gói tin Mesh đến và đưa ra quyết định định tuyến.
     *
     * Luồng xử lý tuần tự:
     *   1. Kiểm tra Checksum -> Drop nếu bị sửa đổi trên đường truyền.
     *   2. Kiểm tra Cache trùng lặp -> Drop nếu đã nhận gói này trước đó.
     *   3. Kiểm tra tự phát (Self-Originated) -> Drop tránh vòng lặp.
     *   4. Xác định gói tin có phải dành cho node này không.
     *   5. Kiểm tra TTL còn đủ để chuyển tiếp không.
     *   6. Biến đổi gói tin (giảm TTL, tăng hop, ghi route) và ra quyết định.
     */
    fun processIncomingPacket(rawPacket: MeshPacket): RoutingDecision {
        // 1. Kiểm tra tính toàn vẹn Checksum SHA-256
        if (!rawPacket.verifyChecksum()) {
            return RoutingDecision.Drop("CHECKSUM_MISMATCH", rawPacket)
        }

        // 2. Kiểm tra trùng lặp bằng Cache (chống Broadcast Storm)
        if (packetCache.isDuplicate(rawPacket.packetId)) {
            return RoutingDecision.Drop("DUPLICATE_PACKET", rawPacket)
        }

        // 3. Kiểm tra xem có phải gói tin do chính mình tạo ra không
        if (rawPacket.sourceNodeId == currentNodeId) {
            return RoutingDecision.Drop("SELF_ORIGINATED", rawPacket)
        }

        // 4. Xác định gói tin có dành cho node này không
        val isBroadcast = rawPacket.destinationNodeId == "BROADCAST"
        val isDirectlyForMe = rawPacket.destinationNodeId == currentNodeId
        val isBaseStationForMe = rawPacket.destinationNodeId == MeshPacket.NODE_BASE_STATION
                && currentNodeId == MeshPacket.NODE_BASE_STATION

        val isForMe = isDirectlyForMe || isBaseStationForMe

        // 5. Kiểm tra TTL còn khả năng chuyển tiếp không
        if (rawPacket.ttl <= 1) {
            // TTL hết nhưng gói tin dành cho mình thì vẫn nhận
            return if (isForMe || isBroadcast) {
                RoutingDecision.DeliverLocal(rawPacket)
            } else {
                RoutingDecision.Drop("TTL_EXPIRED", rawPacket)
            }
        }

        // 6. Biến đổi gói tin để chuyển tiếp (Hop Increment & Source Route)
        val forwardedPacket = rawPacket.copy(
            ttl = rawPacket.ttl - 1,
            hopCount = rawPacket.hopCount + 1,
            senderHopId = currentNodeId,
            routeHistory = ArrayList(rawPacket.routeHistory).apply {
                if (!contains(currentNodeId)) add(currentNodeId)
            }
        )

        // 7. Ra quyết định cuối cùng
        return when {
            // Broadcast: Vừa nhận nội bộ VÀ chuyển tiếp cho node khác
            isBroadcast -> RoutingDecision.DeliverAndForward(forwardedPacket)
            // Unicast dành cho mình: Chỉ nhận nội bộ, không chuyển tiếp
            isForMe -> RoutingDecision.DeliverLocal(forwardedPacket)
            // Unicast dành cho node khác: Chuyển tiếp tiếp tục
            else -> RoutingDecision.Forward(forwardedPacket)
        }
    }

    /**
     * Chuẩn bị gói tin mới do chính node này phát đi (Originating packet).
     */
    fun prepareOutgoingPacket(packet: MeshPacket): MeshPacket {
        packetCache.isDuplicate(packet.packetId) // Đánh dấu vào cache
        return packet.apply {
            senderHopId = currentNodeId
            if (!routeHistory.contains(currentNodeId)) {
                routeHistory.add(currentNodeId)
            }
            signChecksum()
        }
    }

    /** Số lượng gói tin đã xử lý qua cache. */
    fun getCacheSize(): Int = packetCache.size()

    /** Giải phóng tài nguyên. */
    fun shutdown() {
        packetCache.shutdown()
    }
}
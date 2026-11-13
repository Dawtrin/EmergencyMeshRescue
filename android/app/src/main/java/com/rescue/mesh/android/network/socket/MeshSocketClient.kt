package com.rescue.mesh.android.network.socket

import com.rescue.mesh.android.data.model.MeshPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Mesh Socket Client gửi gói tin đến node/trạm cứu hộ.
 *
 * Gửi MeshPacket dưới dạng JSON qua TCP Socket với:
 *   - Timeout kết nối: 5 giây.
 *   - Auto-Retry: Tối đa 3 lần, mỗi lần chờ 1 giây (theo SUPER_PROMPT spec).
 *   - Backoff tuyến tính: 1s -> 2s -> 3s giữa các lần retry.
 */
class MeshSocketClient {

    data class SendResult(
        val success: Boolean,
        val errorMessage: String = "",
        val attemptCount: Int = 1
    )

    companion object {
        private const val MAX_RETRIES = 3
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val BASE_RETRY_DELAY_MS = 1000L
    }

    /**
     * Gửi gói tin với cơ chế Auto-Retry 3 lần.
     *
     * Nếu gửi thất bại (IOException, timeout), tự động thử lại tối đa 3 lần
     * với backoff delay tăng dần (1s, 2s, 3s).
     */
    suspend fun sendPacket(host: String, port: Int, packet: MeshPacket): SendResult =
        withContext(Dispatchers.IO) {
            var lastError = ""
            for (attempt in 1..MAX_RETRIES) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                        socket.soTimeout = CONNECT_TIMEOUT_MS
                        val writer = PrintWriter(
                            socket.getOutputStream().bufferedWriter(Charsets.UTF_8),
                            true
                        )
                        writer.println(packet.toJson())
                        writer.flush()
                    }
                    return@withContext SendResult(
                        success = true,
                        attemptCount = attempt
                    )
                } catch (e: Exception) {
                    lastError = e.message ?: "K\u1ebft n\u1ed1i th\u1ea5t b\u1ea1i"
                    if (attempt < MAX_RETRIES) {
                        delay(BASE_RETRY_DELAY_MS * attempt)
                    }
                }
            }
            SendResult(
                success = false,
                errorMessage = "Th\u1ea5t b\u1ea1i sau $MAX_RETRIES l\u1ea7n th\u1eed: $lastError",
                attemptCount = MAX_RETRIES
            )
        }
}
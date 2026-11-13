package com.rescue.mesh.android.network.socket

import com.rescue.mesh.android.data.model.MeshPacket
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket

/**
 * Mesh Socket Server l\u1eafng nghe g\u00f3i tin t\u1eeb m\u1ea1ng l\u01b0\u1edbi Mesh.
 *
 * M\u1ed7i k\u1ebft n\u1ed1i \u0111\u1ebfn s\u1ebd \u0111\u01b0\u1ee3c \u0111\u1ecdc JSON, parse th\u00e0nh MeshPacket v\u00e0 chuy\u1ec3n
 * cho callback x\u1eed l\u00fd qua RoutingEngine.
 */
class MeshSocketServer(
    private val port: Int,
    private val onPacketReceived: (MeshPacket) -> Unit
) {
    private var serverThread: Thread? = null
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        serverThread = Thread {
            try {
                serverSocket = ServerSocket(port)
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    Thread {
                        try {
                            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8))
                            val json = reader.readLine()
                            if (!json.isNullOrEmpty()) {
                                MeshPacket.fromJson(json)?.let { onPacketReceived(it) }
                            }
                        } catch (_: Exception) {
                        } finally {
                            try { clientSocket.close() } catch (_: Exception) {}
                        }
                    }.start()
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverThread = null
    }
}
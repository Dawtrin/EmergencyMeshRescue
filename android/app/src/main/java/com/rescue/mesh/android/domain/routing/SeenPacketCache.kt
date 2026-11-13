package com.rescue.mesh.android.domain.routing

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * B\u1ed9 \u0111\u1ec7m Packet Cache ch\u1ed1ng l\u1eb7p tr\u00f9ng (Duplicate Prevention).
 *
 * D\u00f9ng ConcurrentHashMap thread-safe \u0111\u1ec3 theo d\u00f5i packetId \u0111\u00e3 th\u1ea5y.
 * T\u1ef1 \u0111\u1ed9ng xo\u00e1 entry c\u0169 h\u01a1n 30 gi\u00e2y \u0111\u1ec3 tr\u00e1nh r\u00f2 r\u1ec9 b\u1ed9 nh\u1edb.
 */
class SeenPacketCache(
    private val expirationMs: Long = 30_000L
) {
    private val cache = ConcurrentHashMap<String, Long>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    init {
        scheduler.scheduleAtFixedRate({
            val now = System.currentTimeMillis()
            cache.entries.removeIf { now - it.value > expirationMs }
        }, expirationMs, expirationMs, TimeUnit.MILLISECONDS)
    }

    /**
     * Ki\u1ec3m tra xem packetId \u0111\u00e3 x\u1eed l\u00fd ch\u01b0a.
     * N\u1ebfu ch\u01b0a t\u1eebng th\u1ea5y -> ghi v\u00e0o cache v\u00e0 tr\u1ea3 v\u1ec1 false.
     * N\u1ebfu \u0111\u00e3 th\u1ea5y r\u1ed3i -> tr\u1ea3 v\u1ec1 true (DUPLICATE).
     */
    fun isDuplicate(packetId: String): Boolean {
        val now = System.currentTimeMillis()
        return cache.putIfAbsent(packetId, now) != null
    }

    fun size(): Int = cache.size

    fun shutdown() {
        scheduler.shutdownNow()
        cache.clear()
    }
}
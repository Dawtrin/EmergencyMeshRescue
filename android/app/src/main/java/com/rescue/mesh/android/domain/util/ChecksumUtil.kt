package com.rescue.mesh.android.domain.util

import java.security.MessageDigest

/**
 * Ti\u1ec7n \u00edch t\u00ednh Checksum SHA-256 \u0111\u1ea3m b\u1ea3o to\u00e0n v\u1eb9n d\u1eef li\u1ec7u theo RFC 6234.
 */
object ChecksumUtil {
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
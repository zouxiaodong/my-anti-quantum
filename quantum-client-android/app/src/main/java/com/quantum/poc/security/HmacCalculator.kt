package com.quantum.poc.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacCalculator {

    private const val ALGORITHM = "HmacSHA256"

    fun calculate(sessionId: String, nonce: String, timestamp: Long, sessionKeyHex: String): String {
        val data = "$sessionId$nonce$timestamp"
        val keyBytes = hexToBytes(sessionKeyHex)
        val keySpec = SecretKeySpec(keyBytes, ALGORITHM)

        val mac = Mac.getInstance(ALGORITHM)
        mac.init(keySpec)
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))

        return hmacBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex string" }
        return ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) +
                    Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
    }
}

package com.quantum.poc.security

import java.security.SecureRandom

object NonceGenerator {
    private val secureRandom = SecureRandom()

    fun generate(byteLength: Int = 16): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateTimestamp(): Long = System.currentTimeMillis()
}

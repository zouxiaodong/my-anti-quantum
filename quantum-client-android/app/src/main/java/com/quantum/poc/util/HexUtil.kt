package com.quantum.poc.util

import java.nio.charset.StandardCharsets

object HexUtil {
    
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
    
    fun stringToHex(str: String): String {
        return bytesToHex(str.toByteArray(StandardCharsets.UTF_8))
    }
    
    fun hexToString(hex: String): String {
        return String(hexToBytes(hex), StandardCharsets.UTF_8)
    }
}

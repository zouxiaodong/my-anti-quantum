package com.quantum.poc.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Sm4Cipher {

    private const val ALGORITHM = "SM4"
    private const val TRANSFORMATION = "SM4/CBC/PKCS7Padding"

    fun encrypt(plainTextHex: String, keyHex: String, ivHex: String): String {
        val key = hexToBytes(keyHex)
        val iv = hexToBytes(ivHex)
        val plainText = hexToBytes(plainTextHex)

        val keySpec = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val cipherText = cipher.doFinal(plainText)
        return bytesToHex(cipherText)
    }

    fun decrypt(cipherTextHex: String, keyHex: String, ivHex: String): String {
        val key = hexToBytes(keyHex)
        val iv = hexToBytes(ivHex)
        val cipherText = hexToBytes(cipherTextHex)

        val keySpec = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val plainText = cipher.doFinal(cipherText)
        return bytesToHex(plainText)
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex string" }
        return ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) +
                    Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

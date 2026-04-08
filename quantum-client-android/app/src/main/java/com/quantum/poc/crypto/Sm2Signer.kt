package com.quantum.poc.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

object Sm2Signer {

    private const val ALGORITHM = "SM2"
    private const val SIGNATURE_ALGORITHM = "SM3withSM2"

    fun sign(dataHex: String, privateKeyHex: String): String {
        val privateKeyBytes = hexToBytes(privateKeyHex)
        val data = hexToBytes(dataHex)

        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val keyFactory = KeyFactory.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        val privateKey = keyFactory.generatePrivate(keySpec)

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        signature.initSign(privateKey)
        signature.update(data)

        return bytesToHex(signature.sign())
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

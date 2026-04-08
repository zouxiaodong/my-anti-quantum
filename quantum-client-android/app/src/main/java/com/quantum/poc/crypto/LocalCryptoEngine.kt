package com.quantum.poc.crypto

data class EncryptedPayload(
    val cipherText: String,
    val iv: String,
    val dilithiumSignature: String,
    val sm2Signature: String
)

class LocalCryptoEngine {

    private var sm4KeyHex: String = ""
    private var ivHex: String = ""
    private var dilithiumPrivateKeyHex: String = ""
    private var sm2PrivateKeyHex: String = ""

    fun setSessionKeys(
        sm4Key: String,
        iv: String,
        dilithiumPrivateKey: String,
        sm2PrivateKey: String
    ) {
        this.sm4KeyHex = sm4Key
        this.ivHex = iv
        this.dilithiumPrivateKeyHex = dilithiumPrivateKey
        this.sm2PrivateKeyHex = sm2PrivateKey
    }

    fun decapsulateSessionKey(encapsulatedKeyHex: String, kyberPrivateKeyHex: String) {
        sm4KeyHex = PqcOperations.kyberDecapsulate(encapsulatedKeyHex, kyberPrivateKeyHex)
        ivHex = generateRandomHex(16)
    }

    fun encryptAndSign(plainTextHex: String): EncryptedPayload {
        val cipherText = Sm4Cipher.encrypt(plainTextHex, sm4KeyHex, ivHex)
        val dilithiumSig = PqcOperations.dilithiumSign(cipherText, dilithiumPrivateKeyHex)
        val sm2Sig = Sm2Signer.sign(cipherText, sm2PrivateKeyHex)

        return EncryptedPayload(
            cipherText = cipherText,
            iv = ivHex,
            dilithiumSignature = dilithiumSig,
            sm2Signature = sm2Sig
        )
    }

    private fun generateRandomHex(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

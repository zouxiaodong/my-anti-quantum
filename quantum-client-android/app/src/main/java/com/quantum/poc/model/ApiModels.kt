package com.quantum.poc.model

import com.google.gson.annotations.SerializedName

data class ApiResult<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: T?,
    @SerializedName("msg") val msg: String
)

data class EncryptRequest(
    @SerializedName("data") val data: String,
    @SerializedName("keyData") val keyData: String,
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("iv") val iv: String? = null
)

data class HashRequest(
    @SerializedName("data") val data: String,
    @SerializedName("algorithm") val algorithm: String
)

data class HMacRequest(
    @SerializedName("data") val data: String,
    @SerializedName("key") val key: String
)

data class Sm2Request(
    @SerializedName("data") val data: String,
    @SerializedName("privateKey") val privateKey: String? = null,
    @SerializedName("publicKey") val publicKey: String? = null
)

data class Sm2VerifyRequest(
    @SerializedName("data") val data: String,
    @SerializedName("signature") val signature: String,
    @SerializedName("publicKey") val publicKey: String
)

data class KeyPairRequest(
    @SerializedName("algorithm") val algorithm: String
)

data class PqcKeyWrapRequest(
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("pqcPubkey") val pqcPubkey: String,
    @SerializedName("symmetricKey") val symmetricKey: String
)

data class PqcKeyUnwrapRequest(
    @SerializedName("algorithm") val algorithm: String,
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("pqcPrikey") val pqcPrikey: String
)

data class HybridEncryptRequest(
    @SerializedName("data") val data: String,
    @SerializedName("sm4Key") val sm4Key: String,
    @SerializedName("sm4Algorithm") val sm4Algorithm: String = "SM4/CBC/NoPadding",
    @SerializedName("signAlgorithm") val signAlgorithm: String?,
    @SerializedName("signPrivateKey") val signPrivateKey: String
)

data class HybridDecryptRequest(
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("signature") val signature: String,
    @SerializedName("sm4Key") val sm4Key: String,
    @SerializedName("sm4Algorithm") val sm4Algorithm: String = "SM4/CBC/NoPadding",
    @SerializedName("signAlgorithm") val signAlgorithm: String?,
    @SerializedName("signPublicKey") val signPublicKey: String
)

data class PqcKeyPairResponse(
    @SerializedName("publicKey") val publicKey: String,
    @SerializedName("privateKey") val privateKey: String
)

data class PqcKeyWrapResponse(
    @SerializedName("keyCipher") val keyCipher: String,
    @SerializedName("keyId") val keyId: String
)

data class HybridEncryptResponse(
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("signature") val signature: String
)

data class HybridDecryptResponse(
    @SerializedName("plainText") val plainText: String?,
    @SerializedName("verifyResult") val verifyResult: Boolean
)


data class SessionEncryptRequest(
    @SerializedName("data") val data: String,
    @SerializedName("sm4Algorithm") val sm4Algorithm: String = "SM4/CBC/NoPadding",
    @SerializedName("iv") val iv: String? = null
)

data class SessionEncryptResponse(
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("signature") val signature: String
)

data class SessionDecryptRequest(
    @SerializedName("cipherText") val cipherText: String,
    @SerializedName("signature") val signature: String,
    @SerializedName("sm4Algorithm") val sm4Algorithm: String = "SM4/CBC/NoPadding",
    @SerializedName("iv") val iv: String? = null
)

data class SessionDecryptResponse(
    @SerializedName("plainText") val plainText: String?,
    @SerializedName("sm2VerifyResult") val sm2VerifyResult: Boolean
)

data class SessionWrapKeyRequest(
    @SerializedName("algorithm") val algorithm: String = "Kyber512",
    @SerializedName("publicKey") val publicKey: String,
    @SerializedName("sessionKey") val sessionKey: String
)

data class SessionSaveKeyRequest(
    @SerializedName("keyCipher") val keyCipher: String
)

data class SessionKyberKeyResponse(
    @SerializedName("publicKey") val publicKey: String
)

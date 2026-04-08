package com.quantum.poc.model

data class SessionInitRequest(
    val clientNonce: String,
    val kyberAlgorithm: String = "Kyber768",
    val dilithiumAlgorithm: String = "Dilithium2"
)

data class SessionInitResponse(
    val sessionId: String,
    val kyberPublicKey: String,
    val serverNonce: String,
    val expiresAt: Long
)

data class SessionKeyResponse(
    val sm2PublicKey: String,
    val sm2PrivateKey: String,
    val dilithiumPublicKey: String,
    val dilithiumPrivateKey: String
)

data class UploadRequest(
    val cipherText: String,
    val iv: String,
    val dilithiumSignature: String,
    val sm2Signature: String
)

data class UploadResponse(
    val success: Boolean,
    val dataId: String
)

data class ResumeRequest(
    val sessionId: String,
    val clientNonce: String,
    val pskHint: String
)

data class ResumeResponse(
    val sessionId: String,
    val resumed: Boolean,
    val expiresAt: Long
)

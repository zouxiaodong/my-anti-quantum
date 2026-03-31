package com.quantum.poc.api

import com.quantum.poc.model.*
import retrofit2.Call
import retrofit2.http.*

interface CryptoApiService {
    
    @POST("api/crypto/genRandom")
    fun genRandom(@Query("length") length: Int): Call<ApiResult<String>>
    
    @POST("api/crypto/sm4/encrypt")
    fun sm4Encrypt(@Body request: EncryptRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/sm4/decrypt")
    fun sm4Decrypt(@Body request: EncryptRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/hash")
    fun hash(@Body request: HashRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/hmac")
    fun hmac(@Body request: HMacRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/ecc/genKeyPair")
    fun genEccKeyPair(): Call<ApiResult<Map<String, String>>>
    
    @POST("api/crypto/sm2/encrypt")
    fun sm2Encrypt(@Body request: Sm2Request): Call<ApiResult<String>>
    
    @POST("api/crypto/sm2/decrypt")
    fun sm2Decrypt(@Body request: Sm2Request): Call<ApiResult<String>>
    
    @POST("api/crypto/sm2/sign")
    fun sm2Sign(@Body request: Sm2Request): Call<ApiResult<String>>
    
    @POST("api/crypto/sm2/verify")
    fun sm2Verify(@Body request: Sm2VerifyRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/pqc/genKeyPair")
    fun genPqcKeyPair(@Body request: KeyPairRequest): Call<ApiResult<PqcKeyPairResponse>>
    
    @POST("api/crypto/pqc/wrapKey")
    fun pqcKeyWrap(@Body request: PqcKeyWrapRequest): Call<ApiResult<PqcKeyWrapResponse>>
    
    @POST("api/crypto/pqc/unwrapKey")
    fun pqcKeyUnwrap(@Body request: PqcKeyUnwrapRequest): Call<ApiResult<String>>
    
    @POST("api/crypto/encrypt")
    fun hybridEncrypt(@Body request: HybridEncryptRequest): Call<ApiResult<HybridEncryptResponse>>
    
    @POST("api/crypto/decrypt")
    fun hybridDecrypt(@Body request: HybridDecryptRequest): Call<ApiResult<HybridDecryptResponse>>
    
    @POST("api/crypto/session/init")
    fun sessionInit(@Body request: SessionInitRequest): Call<ApiResult<SessionInitResponse>>
    
    @GET("api/crypto/session")
    fun sessionGet(@Header("X-Session-Id") sessionId: String): Call<ApiResult<SessionInitResponse>>
    
    @DELETE("api/crypto/session")
    fun sessionDelete(@Header("X-Session-Id") sessionId: String): Call<ApiResult<String>>
    
    @POST("api/crypto/session/wrapKey")
    fun sessionWrapKey(@Header("X-Session-Id") sessionId: String): Call<ApiResult<Map<String, String>>>
    
    @POST("api/crypto/session/genKeys")
    fun sessionGenKeys(@Header("X-Session-Id") sessionId: String): Call<ApiResult<Map<String, String>>>
    
    @POST("api/crypto/session/encrypt")
    fun sessionEncrypt(
        @Header("X-Session-Id") sessionId: String,
        @Body request: SessionEncryptRequest
    ): Call<ApiResult<SessionEncryptResponse>>
    
    @POST("api/crypto/session/decrypt")
    fun sessionDecrypt(
        @Header("X-Session-Id") sessionId: String,
        @Body request: SessionDecryptRequest
    ): Call<ApiResult<SessionDecryptResponse>>
}

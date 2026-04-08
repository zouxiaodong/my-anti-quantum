package com.quantum.poc.api

import com.quantum.poc.model.*
import retrofit2.Call
import retrofit2.http.*

interface GatewayApiService {

    @POST("alsp/v1/session/init")
    fun sessionInit(@Body request: SessionInitRequest): Call<ApiResult<SessionInitResponse>>

    @POST("alsp/v1/session/genKeys")
    fun sessionGenKeys(
        @Header("X-Session-Id") sessionId: String
    ): Call<ApiResult<SessionKeyResponse>>

    @POST("alsp/v1/session/resume")
    fun sessionResume(@Body request: ResumeRequest): Call<ApiResult<ResumeResponse>>

    @POST("alsp/v1/data/upload")
    fun uploadData(
        @Header("X-Session-Id") sessionId: String,
        @Header("X-Nonce") nonce: String,
        @Header("X-Timestamp") timestamp: Long,
        @Header("X-HMAC") hmac: String,
        @Body request: UploadRequest
    ): Call<ApiResult<UploadResponse>>
}

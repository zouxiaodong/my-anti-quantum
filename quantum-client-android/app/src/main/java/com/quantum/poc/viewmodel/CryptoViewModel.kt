package com.quantum.poc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum.poc.api.ApiClient
import com.quantum.poc.crypto.EncryptedPayload
import com.quantum.poc.crypto.LocalCryptoEngine
import com.quantum.poc.model.*
import com.quantum.poc.security.HmacCalculator
import com.quantum.poc.security.NonceGenerator
import com.quantum.poc.util.HexUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SessionData(
    val sessionId: String = "",
    val kyberPublicKey: String = "",
    val sm2PublicKey: String = "",
    val sm2PrivateKey: String = "",
    val dilithiumPublicKey: String = "",
    val dilithiumPrivateKey: String = "",
    val serverNonce: String = "",
    val plainText: String = "12345678",
    val cipherText: String = "",
    val iv: String = "",
    val dilithiumSignature: String = "",
    val sm2Signature: String = "",
    val uploadResult: String = "",
    val logMessages: List<String> = emptyList()
)

sealed class CryptoUiState {
    object Idle : CryptoUiState()
    object SessionCreated : CryptoUiState()
    object KeysGenerated : CryptoUiState()
    object Encrypted : CryptoUiState()
    object Uploaded : CryptoUiState()
    data class Error(val message: String) : CryptoUiState()
}

class CryptoViewModel : ViewModel() {

    private val cryptoEngine = LocalCryptoEngine()

    private val _sessionData = MutableLiveData(SessionData())
    val sessionData: LiveData<SessionData> = _sessionData

    private val _uiState = MutableLiveData<CryptoUiState>(CryptoUiState.Idle)
    val uiState: LiveData<CryptoUiState> = _uiState

    fun newSession() {
        viewModelScope.launch {
            try {
                appendLog("=== Starting new session ===")
                val clientNonce = NonceGenerator.generate(16)
                val request = SessionInitRequest(clientNonce = clientNonce)

                val response = ApiClient.gatewayApiService.sessionInit(request).execute()
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()!!.data!!
                    _sessionData.value = _sessionData.value?.copy(
                        sessionId = data.sessionId,
                        kyberPublicKey = data.kyberPublicKey,
                        serverNonce = data.serverNonce
                    )
                    appendLog("✅ Session created: ${data.sessionId}")
                    _uiState.value = CryptoUiState.SessionCreated
                } else {
                    val errorMsg = response.body()?.msg ?: "Session init failed"
                    appendLog("❌ $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                appendLog("❌ Session init error: ${e.message}")
                _uiState.value = CryptoUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun generateKeys() {
        viewModelScope.launch {
            try {
                val sessionId = _sessionData.value?.sessionId ?: return@launch
                appendLog("Generating SM2 + Dilithium keys...")

                val response = ApiClient.gatewayApiService.sessionGenKeys(sessionId).execute()
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()!!.data!!
                    _sessionData.value = _sessionData.value?.copy(
                        sm2PublicKey = data.sm2PublicKey,
                        sm2PrivateKey = data.sm2PrivateKey,
                        dilithiumPublicKey = data.dilithiumPublicKey,
                        dilithiumPrivateKey = data.dilithiumPrivateKey
                    )
                    appendLog("✅ Keys generated")
                    _uiState.value = CryptoUiState.KeysGenerated
                } else {
                    val errorMsg = response.body()?.msg ?: "Key generation failed"
                    appendLog("❌ $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                appendLog("❌ Key generation error: ${e.message}")
                _uiState.value = CryptoUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun encryptAndUpload() {
        viewModelScope.launch {
            try {
                val session = _sessionData.value ?: return@launch
                if (session.dilithiumPrivateKey.isEmpty()) {
                    appendLog("⚠️ Generate keys first")
                    _uiState.value = CryptoUiState.Error("Generate keys first")
                    return@launch
                }

                appendLog("=== Encrypting locally ===")
                val plainTextHex = HexUtil.stringToHex(session.plainText)

                cryptoEngine.setSessionKeys(
                    sm4Key = session.kyberPublicKey,
                    iv = NonceGenerator.generate(16),
                    dilithiumPrivateKey = session.dilithiumPrivateKey,
                    sm2PrivateKey = session.sm2PrivateKey
                )

                val payload: EncryptedPayload = cryptoEngine.encryptAndSign(plainTextHex)

                _sessionData.value = session.copy(
                    cipherText = payload.cipherText,
                    iv = payload.iv,
                    dilithiumSignature = payload.dilithiumSignature,
                    sm2Signature = payload.sm2Signature
                )

                appendLog("✅ Local encryption complete")
                appendLog("  CipherText: ${payload.cipherText.take(32)}...")
                _uiState.value = CryptoUiState.Encrypted

                appendLog("=== Uploading to Gateway ===")
                uploadToGateway(payload)

            } catch (e: Exception) {
                appendLog("❌ Encrypt/upload error: ${e.message}")
                _uiState.value = CryptoUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun uploadToGateway(payload: EncryptedPayload) {
        val session = _sessionData.value ?: return
        val nonce = NonceGenerator.generate(16)
        val timestamp = NonceGenerator.generateTimestamp()
        val hmac = HmacCalculator.calculate(
            sessionId = session.sessionId,
            nonce = nonce,
            timestamp = timestamp,
            sessionKeyHex = session.kyberPublicKey
        )

        val uploadRequest = UploadRequest(
            cipherText = payload.cipherText,
            iv = payload.iv,
            dilithiumSignature = payload.dilithiumSignature,
            sm2Signature = payload.sm2Signature
        )

        val response = ApiClient.gatewayApiService.uploadData(
            sessionId = session.sessionId,
            nonce = nonce,
            timestamp = timestamp,
            hmac = hmac,
            request = uploadRequest
        ).execute()

        if (response.isSuccessful && response.body()?.code == 0) {
            val data = response.body()!!.data!!
            _sessionData.value = session.copy(
                uploadResult = "✅ Uploaded: ${data.dataId}"
            )
            appendLog("✅ Upload success: ${data.dataId}")
            _uiState.value = CryptoUiState.Uploaded
        } else {
            val errorMsg = response.body()?.msg ?: "Upload failed"
            appendLog("❌ $errorMsg")
            _uiState.value = CryptoUiState.Error(errorMsg)
        }
    }

    fun setPlainText(text: String) {
        _sessionData.value = _sessionData.value?.copy(plainText = text)
    }

    private fun appendLog(message: String) {
        val currentLogs = _sessionData.value?.logMessages ?: emptyList()
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _sessionData.value = _sessionData.value?.copy(
            logMessages = currentLogs + "[$timestamp] $message"
        )
    }

    fun reset() {
        _sessionData.value = SessionData()
        _uiState.value = CryptoUiState.Idle
    }
}

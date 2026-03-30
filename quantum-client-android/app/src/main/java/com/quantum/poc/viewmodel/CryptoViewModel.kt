package com.quantum.poc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.quantum.poc.api.ApiClient
import com.quantum.poc.model.*
import retrofit2.Call
import retrofit2.Callback

enum class SessionState {
    IDLE,
    KEY_READY,
    SESSION_KEY,
    ENCRYPTED,
    SIGNED
}

data class SessionData(
    val state: SessionState = SessionState.IDLE,
    val kyberAlgorithm: String = "Kyber512",
    val dilithiumAlgorithm: String = "Dilithium2",
    val publicKey: String = "",
    val privateKey: String = "",
    val random: String = "",
    val sessionKey: String = "",
    val plainText: String = "12345678",
    val cipherText: String = "",
    val signature: String = "",
    val verifyResult: String = ""
)

class CryptoViewModel : ViewModel() {
    
    private val apiService = ApiClient.cryptoApiService
    
    private val _uiState = MutableLiveData<CryptoUiState>()
    val uiState: LiveData<CryptoUiState> = _uiState
    
    private val _sessionData = MutableLiveData<SessionData>()
    val sessionData: LiveData<SessionData> = _sessionData
    
    private val _logMessage = MutableLiveData<String>("")
    val logMessage: LiveData<String> = _logMessage
    
    init {
        reset()
    }
    
    fun newSession() {
        reset()
        appendLog("🆕 新建会话 - 算法: Kyber512 + SM4-CBC + Dilithium2")
    }
    
    fun setKyberAlgorithm(algorithm: String) {
        _sessionData.value = _sessionData.value?.copy(kyberAlgorithm = algorithm)
        appendLog("🔄 切换密钥协商算法: $algorithm")
    }
    
    fun setDilithiumAlgorithm(algorithm: String) {
        _sessionData.value = _sessionData.value?.copy(dilithiumAlgorithm = algorithm)
        appendLog("🔄 切换签名算法: $algorithm")
    }
    
    fun genKeyPair() {
        _uiState.value = CryptoUiState.Loading
        val algorithm = _sessionData.value?.kyberAlgorithm ?: "Kyber512"
        appendLog("📌 步骤1: 生成${algorithm}密钥对...")
        
        val request = KeyPairRequest(algorithm)
        apiService.genPqcKeyPair(request).enqueue(object : Callback<ApiResult<PqcKeyPairResponse>> {
            override fun onResponse(call: Call<ApiResult<PqcKeyPairResponse>>, response: retrofit2.Response<ApiResult<PqcKeyPairResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    response.body()?.data?.let { data ->
                        _sessionData.value = _sessionData.value?.copy(
                            state = SessionState.KEY_READY,
                            publicKey = data.publicKey,
                            privateKey = data.privateKey
                        )
                    }
                    appendLog("✅ 步骤1完成: 密钥对已生成 (公钥/私钥)")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "请求失败"
                    appendLog("❌ 步骤1失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<PqcKeyPairResponse>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤1失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun genRandom() {
        _uiState.value = CryptoUiState.Loading
        appendLog("📌 步骤2: 生成随机数...")
        
        apiService.genRandom(32).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val randomData = response.body()?.data ?: ""
                    _sessionData.value = _sessionData.value?.copy(
                        state = SessionState.SESSION_KEY,
                        random = randomData,
                        sessionKey = randomData
                    )
                    appendLog("✅ 步骤2完成: 随机数/会话密钥已生成 (32字节)")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "请求失败"
                    appendLog("❌ 步骤2失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤2失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun encrypt(sm4Mode: String = "SM4/CBC/NoPadding") {
        val currentState = _sessionData.value?.state
        if (currentState != SessionState.KEY_READY && currentState != SessionState.SESSION_KEY) {
            appendLog("⚠️ 请先完成密钥协商和随机数生成")
            _uiState.value = CryptoUiState.Error("请先完成密钥协商和随机数生成")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        val sm4ModeName = if (sm4Mode.contains("CBC")) "SM4-CBC" else "SM4-ECB"
        appendLog("📌 步骤3: $sm4ModeName 加密...")
        
        val request = EncryptRequest(
            data = _sessionData.value?.plainText ?: "",
            keyData = _sessionData.value?.sessionKey?.take(32)?.padEnd(32, '0') ?: "0123456789abcdef",
            algorithm = sm4Mode
        )
        
        apiService.sm4Encrypt(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _sessionData.value = _sessionData.value?.copy(
                        state = SessionState.ENCRYPTED,
                        cipherText = response.body()?.data ?: ""
                    )
                    appendLog("✅ 步骤3完成: $sm4ModeName 加密成功")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "请求失败"
                    appendLog("❌ 步骤3失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤3失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun decrypt(sm4Mode: String = "SM4/CBC/NoPadding") {
        val currentState = _sessionData.value?.state
        if (currentState != SessionState.ENCRYPTED) {
            appendLog("⚠️ 请先完成加密操作")
            _uiState.value = CryptoUiState.Error("请先完成加密操作")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        val sm4ModeName = if (sm4Mode.contains("CBC")) "SM4-CBC" else "SM4-ECB"
        appendLog("📌 步骤3b: $sm4ModeName 解密...")
        
        val request = EncryptRequest(
            data = _sessionData.value?.cipherText ?: "",
            keyData = _sessionData.value?.sessionKey?.take(32)?.padEnd(32, '0') ?: "0123456789abcdef",
            algorithm = sm4Mode
        )
        
        apiService.sm4Decrypt(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _sessionData.value = _sessionData.value?.copy(
                        plainText = response.body()?.data ?: ""
                    )
                    appendLog("✅ 步骤3b完成: $sm4ModeName 解密成功")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "请求失败"
                    appendLog("❌ 步骤3b失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤3b失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sign() {
        val currentState = _sessionData.value?.state
        if (currentState != SessionState.KEY_READY && currentState != SessionState.SESSION_KEY) {
            appendLog("⚠️ 请先完成密钥协商")
            _uiState.value = CryptoUiState.Error("请先完成密钥协商")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        val algorithm = _sessionData.value?.dilithiumAlgorithm ?: "Dilithium2"
        appendLog("📌 步骤4: $algorithm 签名...")
        
        val keyPairRequest = KeyPairRequest(algorithm.lowercase())
        apiService.genPqcKeyPair(keyPairRequest).enqueue(object : Callback<ApiResult<PqcKeyPairResponse>> {
            override fun onResponse(call: Call<ApiResult<PqcKeyPairResponse>>, response: retrofit2.Response<ApiResult<PqcKeyPairResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val signPrivateKey = response.body()?.data?.privateKey ?: ""
                    val hmacRequest = HMacRequest(
                        data = _sessionData.value?.plainText ?: "",
                        key = signPrivateKey.take(32).padEnd(32, '0')
                    )
                    
                    apiService.hmac(hmacRequest).enqueue(object : Callback<ApiResult<String>> {
                        override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                            if (response.isSuccessful && response.body()?.code == 0) {
                                _sessionData.value = _sessionData.value?.copy(
                                    state = SessionState.SIGNED,
                                    signature = response.body()?.data ?: ""
                                )
                                appendLog("✅ 步骤4完成: $algorithm 签名成功")
                                _uiState.value = CryptoUiState.Success
                            } else {
                                val errorMsg = response.body()?.msg ?: "签名请求失败"
                                appendLog("❌ 步骤4失败: $errorMsg")
                                _uiState.value = CryptoUiState.Error(errorMsg)
                            }
                        }
                        
                        override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                            val errorMsg = t.message ?: "网络错误"
                            appendLog("❌ 步骤4失败: $errorMsg")
                            _uiState.value = CryptoUiState.Error(errorMsg)
                        }
                    })
                } else {
                    val errorMsg = response.body()?.msg ?: "密钥对生成失败"
                    appendLog("❌ 步骤4失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<PqcKeyPairResponse>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤4失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun verify() {
        val currentState = _sessionData.value?.state
        if (currentState != SessionState.SIGNED) {
            appendLog("⚠️ 请先完成签名操作")
            _uiState.value = CryptoUiState.Error("请先完成签名操作")
            return
        }
        
        val isValid = _sessionData.value?.signature?.isNotEmpty() == true
        val verifyResult = if (isValid) "✅ 验签成功" else "❌ 验签失败"
        _sessionData.value = _sessionData.value?.copy(verifyResult = verifyResult)
        appendLog("📌 步骤4b: $verifyResult")
    }
    
    fun fullFlow() {
        appendLog("🚀 开始执行完整流程...")
        appendLog("组合: ${_sessionData.value?.kyberAlgorithm} + SM4-CBC + ${_sessionData.value?.dilithiumAlgorithm}")
        genKeyPair()
    }
    
    fun setPlainText(text: String) {
        _sessionData.value = _sessionData.value?.copy(plainText = text)
    }
    
    private fun appendLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val newLog = "[$timestamp] $message"
        _logMessage.value = newLog + "\n" + (_logMessage.value ?: "")
    }
    
    fun reset() {
        _sessionData.value = SessionData()
        _logMessage.value = "✅ 会话已重置"
        _uiState.value = CryptoUiState.Idle
    }
}

sealed class CryptoUiState {
    object Idle : CryptoUiState()
    object Loading : CryptoUiState()
    object Success : CryptoUiState()
    data class Error(val message: String) : CryptoUiState()
}

package com.quantum.poc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum.poc.api.ApiClient
import com.quantum.poc.model.*
import kotlinx.coroutines.launch
import retrofit2.Response

class CryptoViewModel : ViewModel() {
    
    private val apiService = ApiClient.cryptoApiService
    
    private val _uiState = MutableLiveData<CryptoUiState>()
    val uiState: LiveData<CryptoUiState> = _uiState
    
    private val _plainText = MutableLiveData<String>("12345678")
    val plainText: LiveData<String> = _plainText
    
    private val _cipherText = MutableLiveData<String>("")
    val cipherText: LiveData<String> = _cipherText
    
    private val _publicKey = MutableLiveData<String>("")
    val publicKey: LiveData<String> = _publicKey
    
    private val _privateKey = MutableLiveData<String>("")
    val privateKey: LiveData<String> = _privateKey
    
    private val _signature = MutableLiveData<String>("")
    val signature: LiveData<String> = _signature
    
    private val _verifyResult = MutableLiveData<String>("")
    val verifyResult: LiveData<String> = _verifyResult
    
    private val _logMessage = MutableLiveData<String>("")
    val logMessage: LiveData<String> = _logMessage
    
    init {
        _uiState.value = CryptoUiState.Idle
    }
    
    fun setPlainText(text: String) {
        _plainText.value = text
    }
    
    fun genRandom(length: Int = 32) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val response = apiService.genRandom(length)
                handleResult(response) { data ->
                    _logMessage.value = "随机数生成: $data"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun genPqcKeyPair(algorithm: String) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = KeyPairRequest(algorithm)
                val response = apiService.genPqcKeyPair(request)
                handleResult(response) { data ->
                    _publicKey.value = data.publicKey
                    _privateKey.value = data.privateKey
                    _logMessage.value = "PQC密钥对生成成功: $algorithm"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun sm4Encrypt(algorithm: String = "SM4/CBC/NoPadding") {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = EncryptRequest(
                    data = _plainText.value ?: "",
                    keyData = "0123456789abcdef",
                    algorithm = algorithm
                )
                val response = apiService.sm4Encrypt(request)
                handleResult(response) { data ->
                    _cipherText.value = data
                    _logMessage.value = "SM4加密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun sm4Decrypt(algorithm: String = "SM4/CBC/NoPadding") {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = EncryptRequest(
                    data = _cipherText.value ?: "",
                    keyData = "0123456789abcdef",
                    algorithm = algorithm
                )
                val response = apiService.sm4Decrypt(request)
                handleResult(response) { data ->
                    _plainText.value = data
                    _logMessage.value = "SM4解密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun sm2Encrypt() {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = Sm2Request(
                    data = _plainText.value ?: "",
                    privateKey = _privateKey.value ?: ""
                )
                val response = apiService.sm2Encrypt(request)
                handleResult(response) { data ->
                    _cipherText.value = data
                    _logMessage.value = "SM2加密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun sm2Decrypt() {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = Sm2Request(
                    data = _cipherText.value ?: "",
                    privateKey = _privateKey.value ?: ""
                )
                val response = apiService.sm2Decrypt(request)
                handleResult(response) { data ->
                    _plainText.value = data
                    _logMessage.value = "SM2解密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun hmacSign() {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = HMacRequest(
                    data = _plainText.value ?: "",
                    key = _privateKey.value ?: ""
                )
                val response = apiService.hmac(request)
                handleResult(response) { data ->
                    _signature.value = data
                    _logMessage.value = "HMAC签名成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun hybridEncrypt(signAlgorithm: String) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = HybridEncryptRequest(
                    data = _plainText.value ?: "",
                    sm4Key = "0123456789abcdef",
                    signAlgorithm = signAlgorithm,
                    signPrivateKey = _privateKey.value ?: ""
                )
                val response = apiService.hybridEncrypt(request)
                handleResult(response) { data ->
                    _cipherText.value = data.cipherText
                    _signature.value = data.signature
                    _logMessage.value = "混合加密成功"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    fun hybridDecrypt(signAlgorithm: String) {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading
            try {
                val request = HybridDecryptRequest(
                    cipherText = _cipherText.value ?: "",
                    signature = _signature.value ?: "",
                    sm4Key = "0123456789abcdef",
                    signAlgorithm = signAlgorithm,
                    signPublicKey = _publicKey.value ?: ""
                )
                val response = apiService.hybridDecrypt(request)
                handleResult(response) { data ->
                    _plainText.value = data.plainText ?: ""
                    _verifyResult.value = if (data.verifyResult) "验签成功" else "验签失败"
                    _logMessage.value = "混合解密成功, 验签: ${data.verifyResult}"
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    private fun <T> handleResult(response: Response<ApiResult<T>>, onSuccess: (T) -> Unit) {
        if (response.isSuccessful && response.body()?.code == 0) {
            response.body()?.data?.let { onSuccess(it) }
            _uiState.value = CryptoUiState.Success
        } else {
            _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
        }
    }
}

sealed class CryptoUiState {
    object Idle : CryptoUiState()
    object Loading : CryptoUiState()
    object Success : CryptoUiState()
    data class Error(val message: String) : CryptoUiState()
}

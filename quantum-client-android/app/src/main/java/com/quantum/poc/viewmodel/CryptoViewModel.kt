package com.quantum.poc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantum.poc.api.ApiClient
import com.quantum.poc.model.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback

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
        _uiState.value = CryptoUiState.Loading
        apiService.genRandom(length).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _logMessage.value = "随机数生成: ${response.body()?.data}"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
    
    fun genPqcKeyPair(algorithm: String) {
        _uiState.value = CryptoUiState.Loading
        val request = KeyPairRequest(algorithm)
        apiService.genPqcKeyPair(request).enqueue(object : Callback<ApiResult<PqcKeyPairResponse>> {
            override fun onResponse(call: Call<ApiResult<PqcKeyPairResponse>>, response: retrofit2.Response<ApiResult<PqcKeyPairResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    response.body()?.data?.let { data ->
                        _publicKey.value = data.publicKey
                        _privateKey.value = data.privateKey
                    }
                    _logMessage.value = "PQC密钥对生成成功: $algorithm"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<PqcKeyPairResponse>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
    
    fun sm4Encrypt(algorithm: String = "SM4/CBC/NoPadding") {
        _uiState.value = CryptoUiState.Loading
        val request = EncryptRequest(
            data = _plainText.value ?: "",
            keyData = "0123456789abcdef",
            algorithm = algorithm
        )
        apiService.sm4Encrypt(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _cipherText.value = response.body()?.data ?: ""
                    _logMessage.value = "SM4加密成功"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
    
    fun sm4Decrypt(algorithm: String = "SM4/CBC/NoPadding") {
        _uiState.value = CryptoUiState.Loading
        val request = EncryptRequest(
            data = _cipherText.value ?: "",
            keyData = "0123456789abcdef",
            algorithm = algorithm
        )
        apiService.sm4Decrypt(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _plainText.value = response.body()?.data ?: ""
                    _logMessage.value = "SM4解密成功"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
    
    fun sm2Encrypt() {
        _uiState.value = CryptoUiState.Loading
        val request = Sm2Request(
            data = _plainText.value ?: "",
            privateKey = _privateKey.value ?: ""
        )
        apiService.sm2Encrypt(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _cipherText.value = response.body()?.data ?: ""
                    _logMessage.value = "SM2加密成功"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
    
    fun sm2Decrypt() {
        _uiState.value = CryptoUiState.Loading
        val request = Sm2Request(
            data = _cipherText.value ?: "",
            privateKey = _privateKey.value ?: ""
        )
        apiService.sm2Decrypt(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _plainText.value = response.body()?.data ?: ""
                    _logMessage.value = "SM2解密成功"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
    
    fun hmacSign() {
        _uiState.value = CryptoUiState.Loading
        val request = HMacRequest(
            data = _plainText.value ?: "",
            key = _privateKey.value ?: ""
        )
        apiService.hmac(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _signature.value = response.body()?.data ?: ""
                    _logMessage.value = "HMAC签名成功"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
    
    fun hybridEncrypt(signAlgorithm: String) {
        _uiState.value = CryptoUiState.Loading
        val request = HybridEncryptRequest(
            data = _plainText.value ?: "",
            sm4Key = "0123456789abcdef",
            signAlgorithm = signAlgorithm,
            signPrivateKey = _privateKey.value ?: ""
        )
        apiService.hybridEncrypt(request).enqueue(object : Callback<ApiResult<HybridEncryptResponse>> {
            override fun onResponse(call: Call<ApiResult<HybridEncryptResponse>>, response: retrofit2.Response<ApiResult<HybridEncryptResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    response.body()?.data?.let { data ->
                        _cipherText.value = data.cipherText
                        _signature.value = data.signature
                    }
                    _logMessage.value = "混合加密成功"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<HybridEncryptResponse>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
    
    fun hybridDecrypt(signAlgorithm: String) {
        _uiState.value = CryptoUiState.Loading
        val request = HybridDecryptRequest(
            cipherText = _cipherText.value ?: "",
            signature = _signature.value ?: "",
            sm4Key = "0123456789abcdef",
            signAlgorithm = signAlgorithm,
            signPublicKey = _publicKey.value ?: ""
        )
        apiService.hybridDecrypt(request).enqueue(object : Callback<ApiResult<HybridDecryptResponse>> {
            override fun onResponse(call: Call<ApiResult<HybridDecryptResponse>>, response: retrofit2.Response<ApiResult<HybridDecryptResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    response.body()?.data?.let { data ->
                        _plainText.value = data.plainText ?: ""
                        _verifyResult.value = if (data.verifyResult) "验签成功" else "验签失败"
                    }
                    _logMessage.value = "混合解密成功"
                    _uiState.value = CryptoUiState.Success
                } else {
                    _uiState.value = CryptoUiState.Error(response.body()?.msg ?: "请求失败")
                }
            }
            override fun onFailure(call: Call<ApiResult<HybridDecryptResponse>>, t: Throwable) {
                _uiState.value = CryptoUiState.Error(t.message ?: "未知错误")
            }
        })
    }
}

sealed class CryptoUiState {
    object Idle : CryptoUiState()
    object Loading : CryptoUiState()
    object Success : CryptoUiState()
    data class Error(val message: String) : CryptoUiState()
}

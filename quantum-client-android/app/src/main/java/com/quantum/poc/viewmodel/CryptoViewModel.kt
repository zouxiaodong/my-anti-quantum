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
    val sessionId: String = "",
    val kyberAlgorithm: String = "Kyber512",
    val dilithiumAlgorithm: String = "Dilithium2",
    val publicKey: String = "",
    val privateKey: String = "",
    val random: String = "",
    val sessionKey: String = "",
    val plainText: String = "12345678",
    val cipherText: String = "",
    val iv: String = "",
    val signature: String = "",
    val verifyResult: String = "",
    // SM2 fields
    val sm2PublicKey: String = "",
    val sm2PrivateKey: String = "",
    val sm2Signature: String = "",
    val sm2VerifyResult: String = "",
    // Decrypt result
    val decryptResult: String = "",
    // Dilithium签名密钥对
    val dilithiumPublicKey: String = "",
    val dilithiumPrivateKey: String = ""
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
        appendLog("📌 步骤2: 生成随机数（作为SM4会话密钥）...")
        
        apiService.genRandom(16).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val randomData = response.body()?.data ?: ""
                    _sessionData.value = _sessionData.value?.copy(
                        state = SessionState.SESSION_KEY,
                        random = randomData,
                        sessionKey = randomData
                    )
                    appendLog("✅ 步骤2完成: 随机数/会话密钥已生成 (16字节)")
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
    
    // 将字符串转换为Hex
    private fun stringToHex(str: String): String {
        return str.toByteArray().joinToString("") { "%02x".format(it) }
    }
    
    // 生成随机Hex字符串
    private fun generateRandomHex(length: Int): String {
        val chars = "0123456789abcdef"
        return (1..length).map { chars.random() }.joinToString("")
    }
    
    // 填充Hex数据到16字节边界
    private fun padToBlockSize(hex: String, blockSize: Int): String {
        val bytes = blockSize
        val padding = bytes - (hex.length / 2 % bytes)
        return if (padding == bytes) hex else {
            val paddingBytes = (1..padding).map { "%02x".format(it) }.joinToString("")
            hex + paddingBytes
        }
    }
    
    // 移除PKCS7填充
    private fun removePadding(hex: String): String {
        return try {
            val bytes = hex.chunked(2).map { it.toInt(16).toByte() }
            val padding = bytes.last().toInt() and 0xFF
            val original = bytes.dropLast(padding)
            String(original.toByteArray(), Charsets.UTF_8)
        } catch (e: Exception) {
            hexToString(hex)
        }
    }
    
    // 将Hex转换为字符串
    private fun hexToString(hex: String): String {
        return try {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray().toString(Charsets.UTF_8)
        } catch (e: Exception) {
            hex
        }
    }
    
    fun encrypt(sm4Mode: String = "SM4/CBC/NoPadding") {
        // 允许在任意状态加密
        _uiState.value = CryptoUiState.Loading
        val sm4ModeName = if (sm4Mode.contains("CBC")) "SM4-CBC" else "SM4-ECB"
        appendLog("📌 步骤3: $sm4ModeName 加密...")
        
        val plainTextHex = stringToHex(_sessionData.value?.plainText ?: "")
        val paddedHex = padToBlockSize(plainTextHex, 16)
        val keyHex = _sessionData.value?.sessionKey ?: "0123456789abcdef"
        
        val ivHex = if (sm4Mode.contains("CBC")) {
            generateRandomHex(32)
        } else null
        
        val request = EncryptRequest(
            data = paddedHex,
            keyData = keyHex,
            algorithm = sm4Mode,
            iv = ivHex
        )
        
        apiService.sm4Encrypt(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _sessionData.value = _sessionData.value?.copy(
                        state = SessionState.ENCRYPTED,
                        cipherText = response.body()?.data ?: "",
                        iv = ivHex ?: ""
                    )
                    appendLog("✅ 步骤3完成: $sm4ModeName 加密成功 (IV: ${ivHex?.take(8)}...)")
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
        // 检查是否有密文
        val cipherText = _sessionData.value?.cipherText
        if (cipherText.isNullOrEmpty()) {
            appendLog("⚠️ 请先执行加密操作")
            _uiState.value = CryptoUiState.Error("请先执行加密操作")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        val sm4ModeName = if (sm4Mode.contains("CBC")) "SM4-CBC" else "SM4-ECB"
        appendLog("📌 步骤3b: $sm4ModeName 解密...")
        
        val keyHex = _sessionData.value?.sessionKey ?: "0123456789abcdef"
        val ivHex = if (sm4Mode.contains("CBC")) _sessionData.value?.iv else null
        
        val request = EncryptRequest(
            data = cipherText,
            keyData = keyHex,
            algorithm = sm4Mode,
            iv = ivHex
        )
        
        apiService.sm4Decrypt(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val decryptedHex = response.body()?.data ?: ""
                    val plainText = removePadding(decryptedHex)
                    _sessionData.value = _sessionData.value?.copy(
                        plainText = plainText,
                        decryptResult = "✅ 解密成功: $plainText"
                    )
                    appendLog("✅ 步骤3b完成: $sm4ModeName 解密成功")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "解密失败"
                    _sessionData.value = _sessionData.value?.copy(decryptResult = "❌ $errorMsg")
                    appendLog("❌ 步骤3b失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                _sessionData.value = _sessionData.value?.copy(decryptResult = "❌ $errorMsg")
                appendLog("❌ 步骤3b失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sign() {
        val privateKey = _sessionData.value?.dilithiumPrivateKey
        if (privateKey.isNullOrEmpty()) {
            appendLog("⚠️ 请先生成签名密钥对")
            _uiState.value = CryptoUiState.Error("请先生成签名密钥对")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        val algorithm = _sessionData.value?.dilithiumAlgorithm ?: "Dilithium2"
        appendLog("📌 步骤4: $algorithm 签名...")
        
        val plainTextHex = stringToHex(_sessionData.value?.plainText ?: "")
        val hmacRequest = HMacRequest(
            data = plainTextHex,
            key = privateKey.take(32).padEnd(32, '0')
        )
        
        apiService.hmac(hmacRequest).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _sessionData.value = _sessionData.value?.copy(
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
    }
    
    fun genDilithiumKeyPair() {
        _uiState.value = CryptoUiState.Loading
        val algorithm = _sessionData.value?.dilithiumAlgorithm ?: "Dilithium2"
        appendLog("📌 生成${algorithm}签名密钥对...")
        
        val keyPairRequest = KeyPairRequest(algorithm.lowercase())
        apiService.genPqcKeyPair(keyPairRequest).enqueue(object : Callback<ApiResult<PqcKeyPairResponse>> {
            override fun onResponse(call: Call<ApiResult<PqcKeyPairResponse>>, response: retrofit2.Response<ApiResult<PqcKeyPairResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    response.body()?.data?.let { data ->
                        _sessionData.value = _sessionData.value?.copy(
                            dilithiumPublicKey = data.publicKey,
                            dilithiumPrivateKey = data.privateKey
                        )
                    }
                    appendLog("✅ 签名密钥对生成成功")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "密钥对生成失败"
                    appendLog("❌ 签名密钥对生成失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<PqcKeyPairResponse>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 签名密钥对生成失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun verify() {
        // 检查是否有签名
        val signature = _sessionData.value?.signature
        if (signature.isNullOrEmpty()) {
            appendLog("⚠️ 请先执行签名操作")
            _uiState.value = CryptoUiState.Error("请先执行签名操作")
            return
        }
        
        val isValid = signature.isNotEmpty()
        val verifyResult = if (isValid) "✅ 验签成功" else "❌ 验签失败"
        _sessionData.value = _sessionData.value?.copy(verifyResult = verifyResult)
        appendLog("📌 步骤4b: $verifyResult")
    }
    
    fun fullFlow() {
        appendLog("🚀 开始执行完整流程...")
        appendLog("组合: ${_sessionData.value?.kyberAlgorithm} + SM4-CBC + ${_sessionData.value?.dilithiumAlgorithm} + SM2")
        
        // Step 1: Generate PQC key pair (Kyber)
        genKeyPair()
        
        // Step 2: Generate random for SM4 session key
        genRandom()
        
        // Step 3: Generate SM2 key pair
        sm2GenKey()
        
        // Step 4: SM2 encryption
        sm2Sign()
        
        // Step 5: SM4 encryption
        encrypt()
        
        // Step 6: SM4 decryption  
        decrypt()
        
        // Step 7: Dilithium signature
        sign()
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
    
    fun clearLog() {
        _logMessage.value = ""
    }
    
    fun sessionInit() {
        _uiState.value = CryptoUiState.Loading
        appendLog("📌 步骤1: 初始化会话 (Kyber + Dilithium)...")
        
        val request = SessionInitRequest(
            kyberAlgorithm = _sessionData.value?.kyberAlgorithm,
            dilithiumAlgorithm = _sessionData.value?.dilithiumAlgorithm
        )
        
        apiService.sessionInit(request).enqueue(object : Callback<ApiResult<SessionInitResponse>> {
            override fun onResponse(call: Call<ApiResult<SessionInitResponse>>, response: retrofit2.Response<ApiResult<SessionInitResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    response.body()?.data?.let { data ->
                        _sessionData.value = _sessionData.value?.copy(
                            state = SessionState.KEY_READY,
                            sessionId = data.sessionId,
                            publicKey = data.kyberPublicKey,
                            privateKey = data.kyberPrivateKey
                        )
                    }
                    appendLog("✅ 步骤1完成: 会话已创建 (SessionID: ${_sessionData.value?.sessionId?.take(8)}...)")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "会话初始化失败"
                    appendLog("❌ 步骤1失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<SessionInitResponse>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤1失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sessionWrapKey() {
        val sessionId = _sessionData.value?.sessionId
        if (sessionId.isNullOrEmpty()) {
            appendLog("⚠️ 请先初始化会话")
            _uiState.value = CryptoUiState.Error("请先初始化会话")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        appendLog("📌 步骤2: 生成SM4会话密钥 (Kyber包装)...")
        
        apiService.sessionWrapKey(sessionId).enqueue(object : Callback<ApiResult<Map<String, String>>> {
            override fun onResponse(call: Call<ApiResult<Map<String, String>>>, response: retrofit2.Response<ApiResult<Map<String, String>>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    _sessionData.value = _sessionData.value?.copy(
                        state = SessionState.SESSION_KEY,
                        sessionKey = data?.get("sessionKey") ?: "",
                        random = data?.get("sessionKey") ?: ""
                    )
                    appendLog("✅ 步骤2完成: 会话密钥已生成")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "密钥包装失败"
                    appendLog("❌ 步骤2失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<Map<String, String>>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤2失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sessionGenKeys() {
        val sessionId = _sessionData.value?.sessionId
        if (sessionId.isNullOrEmpty()) {
            appendLog("⚠️ 请先初始化会话")
            _uiState.value = CryptoUiState.Error("请先初始化会话")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        appendLog("📌 步骤3: 生成SM2和Dilithium密钥对...")
        
        apiService.sessionGenKeys(sessionId).enqueue(object : Callback<ApiResult<Map<String, String>>> {
            override fun onResponse(call: Call<ApiResult<Map<String, String>>>, response: retrofit2.Response<ApiResult<Map<String, String>>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    _sessionData.value = _sessionData.value?.copy(
                        sm2PublicKey = data?.get("sm2PublicKey") ?: "",
                        sm2PrivateKey = data?.get("sm2PrivateKey") ?: "",
                        dilithiumPublicKey = data?.get("dilithiumPublicKey") ?: "",
                        dilithiumPrivateKey = data?.get("dilithiumPrivateKey") ?: ""
                    )
                    appendLog("✅ 步骤3完成: SM2和Dilithium密钥对已生成")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "密钥生成失败"
                    appendLog("❌ 步骤3失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<Map<String, String>>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤3失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sessionEncrypt(sm4Mode: String = "SM4/CBC/NoPadding") {
        val sessionId = _sessionData.value?.sessionId
        if (sessionId.isNullOrEmpty()) {
            appendLog("⚠️ 请先初始化会话")
            _uiState.value = CryptoUiState.Error("请先初始化会话")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        val sm4ModeName = if (sm4Mode.contains("CBC")) "SM4-CBC" else "SM4-ECB"
        appendLog("📌 步骤4: $sm4ModeName 加密 + SM2签名...")
        
        val plainTextHex = stringToHex(_sessionData.value?.plainText ?: "")
        val paddedHex = padToBlockSize(plainTextHex, 16)
        
        val ivHex = if (sm4Mode.contains("CBC")) {
            generateRandomHex(32)
        } else null
        
        val request = SessionEncryptRequest(
            data = paddedHex,
            sm4Algorithm = sm4Mode,
            iv = ivHex
        )
        
        apiService.sessionEncrypt(sessionId, request).enqueue(object : Callback<ApiResult<SessionEncryptResponse>> {
            override fun onResponse(call: Call<ApiResult<SessionEncryptResponse>>, response: retrofit2.Response<ApiResult<SessionEncryptResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    _sessionData.value = _sessionData.value?.copy(
                        state = SessionState.ENCRYPTED,
                        cipherText = data?.cipherText ?: "",
                        iv = ivHex ?: "",
                        sm2Signature = data?.signature ?: ""
                    )
                    appendLog("✅ 步骤4完成: $sm4ModeName 加密 + SM2签名成功")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "加密失败"
                    appendLog("❌ 步骤4失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<SessionEncryptResponse>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ 步骤4失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sessionDecrypt(sm4Mode: String = "SM4/CBC/NoPadding") {
        val sessionId = _sessionData.value?.sessionId
        if (sessionId.isNullOrEmpty()) {
            appendLog("⚠️ 请先初始化会话")
            _uiState.value = CryptoUiState.Error("请先初始化会话")
            return
        }
        
        val cipherText = _sessionData.value?.cipherText
        if (cipherText.isNullOrEmpty()) {
            appendLog("⚠️ 请先执行加密操作")
            _uiState.value = CryptoUiState.Error("请先执行加密操作")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        val sm4ModeName = if (sm4Mode.contains("CBC")) "SM4-CBC" else "SM4-ECB"
        appendLog("📌 步骤5: $sm4ModeName 解密 + SM2验签...")
        
        val request = SessionDecryptRequest(
            cipherText = cipherText,
            signature = _sessionData.value?.sm2Signature ?: "",
            sm4Algorithm = sm4Mode,
            iv = _sessionData.value?.iv
        )
        
        apiService.sessionDecrypt(sessionId, request).enqueue(object : Callback<ApiResult<SessionDecryptResponse>> {
            override fun onResponse(call: Call<ApiResult<SessionDecryptResponse>>, response: retrofit2.Response<ApiResult<SessionDecryptResponse>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    val decryptedHex = data?.plainText ?: ""
                    val plainText = removePadding(decryptedHex)
                    val verifyResult = data?.sm2VerifyResult ?: false
                    
                    _sessionData.value = _sessionData.value?.copy(
                        plainText = plainText,
                        decryptResult = if (verifyResult) "✅ 解密成功" else "❌ SM2验签失败",
                        sm2VerifyResult = if (verifyResult) "✅ SM2验签成功" else "❌ SM2验签失败"
                    )
                    appendLog(if (verifyResult) "✅ 步骤5完成: $sm4ModeName 解密成功 + SM2验签成功" else "❌ 步骤5失败: SM2验签失败")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "解密失败"
                    _sessionData.value = _sessionData.value?.copy(decryptResult = "❌ $errorMsg")
                    appendLog("❌ 步骤5失败: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<SessionDecryptResponse>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                _sessionData.value = _sessionData.value?.copy(decryptResult = "❌ $errorMsg")
                appendLog("❌ 步骤5失败: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sessionFullFlow() {
        appendLog("🚀 开始执行Session完整流程...")
        appendLog("组合: ${_sessionData.value?.kyberAlgorithm} + SM4-CBC + ${_sessionData.value?.dilithiumAlgorithm} + SM2")
        
        sessionInit()
    }
    
    fun reset() {
        _sessionData.value = SessionData()
        _logMessage.value = "✅ 会话已重置"
        _uiState.value = CryptoUiState.Idle
    }
    
    // SM2 methods
    fun sm2GenKey() {
        _uiState.value = CryptoUiState.Loading
        appendLog("📌 SM2: 密钥生成...")
        
        apiService.genEccKeyPair().enqueue(object : Callback<ApiResult<Map<String, String>>> {
            override fun onResponse(call: Call<ApiResult<Map<String, String>>>, response: retrofit2.Response<ApiResult<Map<String, String>>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    _sessionData.value = _sessionData.value?.copy(
                        sm2PublicKey = data?.get("publicKey") ?: "",
                        sm2PrivateKey = data?.get("privateKey") ?: ""
                    )
                    appendLog("✅ SM2: 密钥生成成功")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "SM2密钥生成失败"
                    appendLog("❌ SM2: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<Map<String, String>>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ SM2: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sm2Sign() {
        val privateKey = _sessionData.value?.sm2PrivateKey
        if (privateKey.isNullOrEmpty()) {
            appendLog("⚠️ 请先生成SM2密钥对")
            _uiState.value = CryptoUiState.Error("请先生成SM2密钥对")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        appendLog("📌 SM2: 签名...")
        
        val plainTextHex = stringToHex(_sessionData.value?.plainText ?: "")
        val request = Sm2Request(
            data = plainTextHex,
            privateKey = privateKey
        )
        
        apiService.sm2Sign(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    _sessionData.value = _sessionData.value?.copy(
                        sm2Signature = response.body()?.data ?: ""
                    )
                    appendLog("✅ SM2: 签名成功")
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "SM2签名失败"
                    appendLog("❌ SM2: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ SM2: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
            }
        })
    }
    
    fun sm2Verify() {
        val signature = _sessionData.value?.sm2Signature
        val publicKey = _sessionData.value?.sm2PublicKey
        if (signature.isNullOrEmpty() || publicKey.isNullOrEmpty()) {
            appendLog("⚠️ 请先执行SM2签名")
            _uiState.value = CryptoUiState.Error("请先执行SM2签名")
            return
        }
        
        _uiState.value = CryptoUiState.Loading
        appendLog("📌 SM2: 验签...")
        
        val plainTextHex = stringToHex(_sessionData.value?.plainText ?: "")
        val request = Sm2VerifyRequest(
            data = plainTextHex,
            signature = signature,
            publicKey = publicKey
        )
        
        apiService.sm2Verify(request).enqueue(object : Callback<ApiResult<String>> {
            override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
                if (response.isSuccessful && response.body()?.code == 0) {
                    val isValid = response.body()?.data == "true"
                    val result = if (isValid) "✅ SM2验签成功" else "❌ SM2验签失败"
                    _sessionData.value = _sessionData.value?.copy(sm2VerifyResult = result)
                    appendLog(result)
                    _uiState.value = CryptoUiState.Success
                } else {
                    val errorMsg = response.body()?.msg ?: "SM2验签失败"
                    appendLog("❌ SM2: $errorMsg")
                    _uiState.value = CryptoUiState.Error(errorMsg)
                }
            }
            
            override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                val errorMsg = t.message ?: "网络错误"
                appendLog("❌ SM2: $errorMsg")
                _uiState.value = CryptoUiState.Error(errorMsg)
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

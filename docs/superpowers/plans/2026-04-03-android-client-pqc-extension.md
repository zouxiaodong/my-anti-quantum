# Android Client PQC Extension Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the Android client to perform LOCAL crypto operations (SM4 encryption, Dilithium+SM2 signing, Kyber decapsulation) and communicate with the new Gateway via ALSP protocol — ensuring plaintext never leaves the device.

**Architecture:** Add local crypto engine using BouncyCastle (SM4/SM2) and a PQC library (Kyber/Dilithium). Replace direct encryptor calls with Gateway ALSP API calls. The flow: receive encapsulated session key from Gateway → locally decapsulate → locally encrypt plaintext → locally sign → upload ciphertext to Gateway.

**Tech Stack:** Kotlin 1.9, Android minSdk 24, BouncyCastle 1.83 (SM4/SM2), Retrofit 2.9, OkHttp 4.12, Coroutines

**Design Spec:** `docs/superpowers/specs/2026-04-03-pqc-secure-communication-design.md`

**Existing Codebase:**
- `quantum-client-android/app/` — existing Android project
- Current: calls encryptor directly for ALL crypto operations
- Target: do crypto LOCALLY, only call Gateway for session management and data upload

**Key Constraint:** Android minSdk 24. BouncyCastle works on Android. For Kyber/Dilithium, we'll use a pure-Java PQC implementation to avoid NDK complexity.

---

## File Map

### New Files

| File | Responsibility |
|------|---------------|
| `app/src/main/java/com/quantum/poc/crypto/LocalCryptoEngine.kt` | Local crypto: SM4 encrypt, Dilithium sign, SM2 sign, Kyber decapsulate |
| `app/src/main/java/com/quantum/poc/crypto/Sm4Cipher.kt` | SM4-CBC encryption wrapper |
| `app/src/main/java/com/quantum/poc/crypto/Sm2Signer.kt` | SM2 signing wrapper |
| `app/src/main/java/com/quantum/poc/crypto/PqcOperations.kt` | Kyber decapsulation + Dilithium signing |
| `app/src/main/java/com/quantum/poc/api/GatewayApiService.kt` | New Retrofit interface for Gateway ALSP endpoints |
| `app/src/main/java/com/quantum/poc/model/GatewayModels.kt` | New data models for Gateway API |
| `app/src/main/java/com/quantum/poc/security/HmacCalculator.kt` | HMAC-SHA256 for request integrity |
| `app/src/main/java/com/quantum/poc/security/NonceGenerator.kt` | Secure random nonce generation |

### Modified Files

| File | Changes |
|------|---------|
| `app/build.gradle` | Add BouncyCastle dependency |
| `app/src/main/java/com/quantum/poc/api/ApiClient.kt` | Add Gateway base URL + GatewayApiService instance |
| `app/src/main/java/com/quantum/poc/viewmodel/CryptoViewModel.kt` | Replace remote crypto calls with local crypto + Gateway API calls |
| `app/src/main/java/com/quantum/poc/ui/MainActivity.kt` | Update UI to reflect new flow |

---

## Chunk 1: Dependencies + Crypto Engine

### Task 1: Add BouncyCastle Dependency

**Files:**
- Modify: `app/build.gradle`

- [ ] **Step 1: Add BouncyCastle to build.gradle**

Add to the `dependencies` block in `app/build.gradle`:

```gradle
    // BouncyCastle for SM4 + SM2
    implementation 'org.bouncycastle:bcprov-jdk18on:1.83'
```

- [ ] **Step 2: Register BouncyCastle provider**

Create `app/src/main/java/com/quantum/poc/QuantumApp.kt`:

```kotlin
package com.quantum.poc

import android.app.Application
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class QuantumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Security.addProvider(BouncyCastleProvider())
    }
}
```

- [ ] **Step 3: Register Application in AndroidManifest.xml**

Add `android:name=".QuantumApp"` to the `<application>` tag in `app/src/main/AndroidManifest.xml`:

```xml
<application
    android:name=".QuantumApp"
    ...
```

- [ ] **Step 4: Verify project syncs**

Run: `cd quantum-client-android && ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add quantum-client-android/app/build.gradle quantum-client-android/app/src/main/java/com/quantum/poc/QuantumApp.kt quantum-client-android/app/src/main/AndroidManifest.xml
git commit -m "feat: add BouncyCastle dependency for SM4/SM2 crypto"
```

---

### Task 2: Implement Local Crypto Engine

**Files:**
- Create: `app/src/main/java/com/quantum/poc/crypto/Sm4Cipher.kt`
- Create: `app/src/main/java/com/quantum/poc/crypto/Sm2Signer.kt`
- Create: `app/src/main/java/com/quantum/poc/crypto/PqcOperations.kt`
- Create: `app/src/main/java/com/quantum/poc/crypto/LocalCryptoEngine.kt`

- [ ] **Step 1: Create Sm4Cipher.kt**

```kotlin
package com.quantum.poc.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Sm4Cipher {

    private const val ALGORITHM = "SM4"
    private const val TRANSFORMATION = "SM4/CBC/PKCS7Padding"

    fun encrypt(plainTextHex: String, keyHex: String, ivHex: String): String {
        val key = hexToBytes(keyHex)
        val iv = hexToBytes(ivHex)
        val plainText = hexToBytes(plainTextHex)

        val keySpec = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val cipherText = cipher.doFinal(plainText)
        return bytesToHex(cipherText)
    }

    fun decrypt(cipherTextHex: String, keyHex: String, ivHex: String): String {
        val key = hexToBytes(keyHex)
        val iv = hexToBytes(ivHex)
        val cipherText = hexToBytes(cipherTextHex)

        val keySpec = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)

        val plainText = cipher.doFinal(cipherText)
        return bytesToHex(plainText)
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex string" }
        return ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) +
                    Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 2: Create Sm2Signer.kt**

```kotlin
package com.quantum.poc.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

object Sm2Signer {

    private const val ALGORITHM = "SM2"
    private const val SIGNATURE_ALGORITHM = "SM3withSM2"

    fun sign(dataHex: String, privateKeyHex: String): String {
        val privateKeyBytes = hexToBytes(privateKeyHex)
        val data = hexToBytes(dataHex)

        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val keyFactory = KeyFactory.getInstance(ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        val privateKey = keyFactory.generatePrivate(keySpec)

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        signature.initSign(privateKey)
        signature.update(data)

        return bytesToHex(signature.sign())
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex string" }
        return ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) +
                    Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 3: Create PqcOperations.kt**

```kotlin
package com.quantum.poc.crypto

object PqcOperations {

    /**
     * Kyber768 decapsulation: use private key to extract shared secret from encapsulated key.
     *
     * NOTE: For POC, this is a placeholder. In production, integrate liboqs-android
     * or a pure-Java Kyber implementation.
     *
     * The actual decapsulation logic depends on the Kyber library used.
     * This stub returns the encapsulated key as-is for compilation.
     */
    fun kyberDecapsulate(encapsulatedKeyHex: String, privateKeyHex: String): String {
        // TODO: Replace with actual Kyber768 decapsulation
        // Using liboqs-android or a pure-Java Kyber implementation
        // For now, return a derived key as placeholder
        return deriveKey(encapsulatedKeyHex, privateKeyHex)
    }

    /**
     * Dilithium2 signing: sign data with Dilithium private key.
     *
     * NOTE: For POC, this is a placeholder. In production, integrate liboqs-android
     * or a pure-Java Dilithium implementation.
     */
    fun dilithiumSign(dataHex: String, privateKeyHex: String): String {
        // TODO: Replace with actual Dilithium2 signing
        // Using liboqs-android or a pure-Java Dilithium implementation
        // For now, return a derived signature as placeholder
        return deriveKey(dataHex, privateKeyHex)
    }

    private fun deriveKey(data: String, key: String): String {
        val combined = (data + key).toByteArray(Charsets.UTF_8)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(combined).joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 4: Create LocalCryptoEngine.kt**

```kotlin
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
```

- [ ] **Step 5: Verify project compiles**

Run: `cd quantum-client-android && ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add quantum-client-android/app/src/main/java/com/quantum/poc/crypto/
git commit -m "feat: implement local crypto engine (SM4, SM2, PQC placeholders)"
```

---

## Chunk 2: Gateway API + Security Helpers

### Task 3: Add Gateway API Service and Models

**Files:**
- Create: `app/src/main/java/com/quantum/poc/api/GatewayApiService.kt`
- Create: `app/src/main/java/com/quantum/poc/model/GatewayModels.kt`
- Modify: `app/src/main/java/com/quantum/poc/api/ApiClient.kt`

- [ ] **Step 1: Create GatewayModels.kt**

```kotlin
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
```

- [ ] **Step 2: Create GatewayApiService.kt**

```kotlin
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
```

- [ ] **Step 3: Update ApiClient.kt**

Replace the existing `ApiClient.kt` content:

```kotlin
package com.quantum.poc.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val GATEWAY_URL = "http://192.168.31.5:8443/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(GATEWAY_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val gatewayApiService: GatewayApiService = retrofit.create(GatewayApiService::class.java)
}
```

- [ ] **Step 4: Verify project compiles**

Run: `cd quantum-client-android && ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add quantum-client-android/app/src/main/java/com/quantum/poc/api/GatewayApiService.kt quantum-client-android/app/src/main/java/com/quantum/poc/model/GatewayModels.kt quantum-client-android/app/src/main/java/com/quantum/poc/api/ApiClient.kt
git commit -m "feat: add Gateway API service and models, update ApiClient to point to Gateway"
```

---

### Task 4: Add Security Helpers

**Files:**
- Create: `app/src/main/java/com/quantum/poc/security/HmacCalculator.kt`
- Create: `app/src/main/java/com/quantum/poc/security/NonceGenerator.kt`

- [ ] **Step 1: Create NonceGenerator.kt**

```kotlin
package com.quantum.poc.security

import java.security.SecureRandom

object NonceGenerator {
    private val secureRandom = SecureRandom()

    fun generate(byteLength: Int = 16): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateTimestamp(): Long = System.currentTimeMillis()
}
```

- [ ] **Step 2: Create HmacCalculator.kt**

```kotlin
package com.quantum.poc.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacCalculator {

    private const val ALGORITHM = "HmacSHA256"

    fun calculate(sessionId: String, nonce: String, timestamp: Long, sessionKeyHex: String): String {
        val data = "$sessionId$nonce$timestamp"
        val keyBytes = hexToBytes(sessionKeyHex)
        val keySpec = SecretKeySpec(keyBytes, ALGORITHM)

        val mac = Mac.getInstance(ALGORITHM)
        mac.init(keySpec)
        val hmacBytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))

        return hmacBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Invalid hex string" }
        return ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) +
                    Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
    }
}
```

- [ ] **Step 3: Verify project compiles**

Run: `cd quantum-client-android && ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add quantum-client-android/app/src/main/java/com/quantum/poc/security/
git commit -m "feat: add NonceGenerator and HmacCalculator security helpers"
```

---

## Chunk 3: ViewModel Rewrite + UI Update

### Task 5: Rewrite CryptoViewModel for Gateway Flow

**Files:**
- Modify: `app/src/main/java/com/quantum/poc/viewmodel/CryptoViewModel.kt`

- [ ] **Step 1: Rewrite CryptoViewModel.kt**

Replace the entire content of `CryptoViewModel.kt`:

```kotlin
package com.quantum.poc.viewmodel

import android.util.Log
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

data class SessionData(
    val sessionId: String = "",
    val kyberPublicKey: String = "",
    val kyberPrivateKey: String = "",
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
        _sessionData.value = _sessionData.value?.copy(
            logMessages = currentLogs + message
        )
        Log.d("CryptoViewModel", message)
    }

    fun reset() {
        _sessionData.value = SessionData()
        _uiState.value = CryptoUiState.Idle
    }
}
```

- [ ] **Step 2: Verify project compiles**

Run: `cd quantum-client-android && ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESS (may have warnings about unused imports from old code — fix if errors)

- [ ] **Step 3: Commit**

```bash
git add quantum-client-android/app/src/main/java/com/quantum/poc/viewmodel/CryptoViewModel.kt
git commit -m "feat: rewrite CryptoViewModel for local encryption + Gateway upload flow"
```

---

### Task 6: Update MainActivity UI

**Files:**
- Modify: `app/src/main/java/com/quantum/poc/ui/MainActivity.kt`

- [ ] **Step 1: Read existing MainActivity.kt**

Read the current file at `app/src/main/java/com/quantum/poc/ui/MainActivity.kt` to understand the existing UI structure.

- [ ] **Step 2: Update MainActivity.kt**

Update button click handlers and observers to match the new ViewModel API. Key changes:
- Remove references to old encryptor API calls
- Update button labels to reflect new flow: "New Session" → "Generate Keys" → "Encrypt & Upload"
- Update log display to show new log format

```kotlin
package com.quantum.poc.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.quantum.poc.R
import com.quantum.poc.viewmodel.CryptoUiState
import com.quantum.poc.viewmodel.CryptoViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: CryptoViewModel by viewModels()

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var plainTextInput: EditText
    private lateinit var btnNewSession: Button
    private lateinit var btnGenerateKeys: Button
    private lateinit var btnEncryptUpload: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        plainTextInput = findViewById(R.id.plainTextInput)
        btnNewSession = findViewById(R.id.btnNewSession)
        btnGenerateKeys = findViewById(R.id.btnGenerateKeys)
        btnEncryptUpload = findViewById(R.id.btnEncryptUpload)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.sessionData.observe(this) { data ->
            plainTextInput.setText(data.plainText)
            logText.text = data.logMessages.joinToString("\n")
        }

        viewModel.uiState.observe(this) { state ->
            when (state) {
                is CryptoUiState.Idle -> {
                    statusText.text = "Ready"
                    btnGenerateKeys.isEnabled = false
                    btnEncryptUpload.isEnabled = false
                }
                is CryptoUiState.SessionCreated -> {
                    statusText.text = "Session Created"
                    btnGenerateKeys.isEnabled = true
                    btnEncryptUpload.isEnabled = false
                }
                is CryptoUiState.KeysGenerated -> {
                    statusText.text = "Keys Generated"
                    btnEncryptUpload.isEnabled = true
                }
                is CryptoUiState.Encrypted -> {
                    statusText.text = "Encrypted Locally"
                }
                is CryptoUiState.Uploaded -> {
                    statusText.text = "Uploaded Successfully"
                }
                is CryptoUiState.Error -> {
                    statusText.text = "Error: ${state.message}"
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnNewSession.setOnClickListener {
            viewModel.newSession()
        }

        btnGenerateKeys.setOnClickListener {
            viewModel.generateKeys()
        }

        btnEncryptUpload.setOnClickListener {
            val text = plainTextInput.text.toString()
            viewModel.setPlainText(text)
            viewModel.encryptAndUpload()
        }
    }
}
```

- [ ] **Step 3: Update layout if needed**

Check `app/src/main/res/layout/activity_main.xml` — ensure button IDs match:
- `btnNewSession`
- `btnGenerateKeys`
- `btnEncryptUpload`
- `statusText`
- `logText`
- `plainTextInput`

If IDs differ, update either the layout XML or the Kotlin code to match.

- [ ] **Step 4: Verify project compiles**

Run: `cd quantum-client-android && ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add quantum-client-android/app/src/main/java/com/quantum/poc/ui/MainActivity.kt
git commit -m "feat: update MainActivity for new encrypt-and-upload flow"
```

---

## Chunk 4: Full Build + Integration Test

### Task 7: Full Build Verification

- [ ] **Step 1: Run full debug build**

Run: `cd quantum-client-android && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESS, APK generated at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 2: Run lint check**

Run: `cd quantum-client-android && ./gradlew :app:lintDebug`
Expected: No critical errors (warnings OK)

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: Android client PQC extension complete — local encryption + Gateway upload"
```

---

## Summary

| Chunk | Tasks | Key Deliverable |
|-------|-------|----------------|
| 1 | 1-2 | BouncyCastle + Local Crypto Engine (SM4, SM2, PQC placeholders) |
| 2 | 3-4 | Gateway API + Security Helpers (HMAC, Nonce) |
| 3 | 5-6 | ViewModel rewrite + UI update |
| 4 | 7 | Full build verification |

**Total steps:** ~25 steps across 7 tasks
**Estimated time:** 2-3 hours

**Important Notes:**
1. **PQC Placeholder**: `PqcOperations.kt` uses SHA-256 as placeholder for Kyber/Dilithium. For production POC, integrate `liboqs-android` or a pure-Java PQC library.
2. **Gateway URL**: Update `GATEWAY_URL` in `ApiClient.kt` to match actual Gateway address.
3. **SM2 Key Format**: The Sm2Signer expects PKCS8-encoded private keys. Ensure the Gateway returns keys in this format, or add key conversion logic.

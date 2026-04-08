# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a quantum-safe encryption system that implements post-quantum cryptography (PQC) using hybrid algorithms including:

- **Kyber768**: For key encapsulation mechanism (KEM)
- **Dilithium2**: For digital signatures
- **SM2/SM4**: Chinese cryptographic standards for asymmetric encryption/signatures and symmetric encryption
- **AES**: Traditional encryption as fallback

The system consists of three main components:

1. **quantum-client-android**: Android client app implementing local crypto operations
2. **quantum-encryptor-gateway**: Gateway server managing session state and crypto operations
3. **quantum-business-server**: Business server storing processed data
4. **quantum-mock-encryptor**: Mock service for encryptor operations

## Architecture

### Client-Server Flow
1. Client initializes session with server to establish secure channel
2. Server generates Kyber key pair and returns public key to client
3. Client generates session keys using SM2 and Dilithium algorithms
4. Client performs local encryption and signature operations
5. Client uploads encrypted data with signatures to gateway
6. Gateway validates signatures and forwards to business server

### Android Client Components
- `LocalCryptoEngine.kt`: Core encryption/decryption logic
- `PqcOperations.kt`: Placeholder PQC operations (Kyber/Dilithium)
- `Sm2Signer.kt`: SM2 signing operations
- `Sm4Cipher.kt`: SM4 encryption/decryption
- `HmacCalculator.kt`: HMAC calculations for request validation
- `NonceGenerator.kt`: Secure random nonce generation
- `CryptoViewModel.kt`: Handles UI state and API interactions
- `GatewayApiService.kt`: Defines API endpoints for gateway communication

### Gateway Server Components
- `SessionController.java`: Manages session lifecycle (init, key generation, resume)
- `DataController.java`: Handles encrypted data upload with signature verification
- `SessionManager.java`: Maintains session state and lifecycle
- `ReplayProtectionService.java`: Prevents replay attacks with HMAC validation
- `EncryptorClient.java`: Interfaces with encryptor service
- `BusinessServerClient.java`: Forwards validated data to business server

## Development Commands

### Android Client
```bash
# Build the Android app
cd quantum-client-android
./gradlew assembleDebug

# Run the Android app
./gradlew installDebug

# Run tests
./gradlew test
```

### Java Services
```bash
# Build gateway server
cd quantum-encryptor-gateway
mvn clean install
mvn spring-boot:run

# Build business server
cd quantum-business-server
mvn clean install
mvn spring-boot:run

# Build mock encryptor
cd quantum-mock-encryptor
mvn clean install
mvn spring-boot:run
```

## Key Security Features

### Session Management
- Time-limited sessions with expiration
- Nonce-based replay attack prevention
- HMAC-based request authentication
- Session key establishment using PQC algorithms

### Cryptographic Operations
- Hybrid encryption (Kyber + SM4)
- Dual signatures (Dilithium + SM2)
- Secure key derivation and management
- Client-side encryption before transmission

### API Endpoints

#### Gateway Server (Port 8443)
- `POST /alsp/v1/session/init` - Initialize new session with client nonce
- `POST /alsp/v1/session/genKeys` - Generate SM2/Dilithium key pairs for session
- `POST /alsp/v1/session/resume` - Resume existing session
- `POST /alsp/v1/data/upload` - Upload encrypted data with signatures (requires session ID, nonce, timestamp, HMAC headers)

#### Business Server (typically port 8081)
- `POST /api/data/receive` - Receive decrypted data from gateway
- `GET /api/data/{dataId}` - Query stored data

## File Structure

- `quantum-client-android/` - Android client with Kotlin implementation
- `quantum-encryptor-gateway/` - Spring Boot gateway service in Java
- `quantum-business-server/` - Spring Boot business service in Java
- `quantum-mock-encryptor/` - Spring Boot mock encryptor in Java
- `docs/superpowers/` - Project documentation and specifications
- `requirements/` - Project requirements documentation

## Important Configuration

### Client Configuration
- API endpoints defined in `ApiClient.kt` (currently targeting 192.168.31.5:8443)
- Uses Bouncy Castle provider for SM2/SM4 support

### Server Configuration
- Session TTL configured via `session.ttl-ms` property (default 86400000ms/24h)
- Max concurrent sessions configured via `session.max-sessions` property (default 10000)
- CORS enabled for all origins (development only)

## Development Notes

- PQC operations (Kyber/Dilithium) currently use placeholder implementations
- Production deployment requires integration with liboqs-android or pure-Java PQC libraries
- Crypto engine uses session keys established through the gateway
- All API requests use header-based session identification
- Nonces and timestamps prevent replay attacks
- HMAC validation ensures request integrity
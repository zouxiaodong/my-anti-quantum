# AGENTS.md - Quantum Resistant Encryption Project

## Project Overview

This repository contains a quantum-resistant encryption system with three main components:

| Component | Technology | Description |
|-----------|------------|-------------|
| `quantum-mock-encryptor` | Java 17 + Spring Boot 3.2.5 | Mock quantum-resistant encryption server |
| `quantum-client-android` | Kotlin 1.9 + Android | Android client application |
| `test_mock.py` | Python 3.12 | HTTP integration test script |

**Note**: `quantum-server/` and `quantum-encryptor-gateway/` directories are empty placeholders.

---

## Build & Run Commands

### quantum-mock-encryptor (Spring Boot Server)

```bash
cd quantum-mock-encryptor

# Build
mvn clean package

# Run server (default port: 28101)
mvn spring-boot:run

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Skip tests during build
mvn clean package -DskipTests
```

**Key Configuration**:
- `pom.xml`: Maven build with Spring Boot 3.2.5 parent
- Java 17, Lombok, BouncyCastle 1.83, Spring Validation
- No explicit test directory exists yet

### quantum-client-android (Android App)

```bash
cd quantum-client-android

# Build full project
./gradlew build

# Assemble debug APK
./gradlew :app:assembleDebug

# Clean project
./gradlew clean

# Run lint
./gradlew :app:lint
./gradlew :app:lintDebug

# Run unit tests (local JVM)
./gradlew :app:testDebugUnitTest

# Run single unit test class
./gradlew :app:testDebugUnitTest --tests "com.quantum.poc.MyUnitTest"

# Run single unit test method
./gradlew :app:testDebugUnitTest --tests "com.quantum.poc.MyUnitTest#testMethod"

# Run instrumentation tests (requires device/emulator)
./gradlew :app:connectedAndroidTest

# Run single instrumentation test
./gradlew :app:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.quantum.poc.MyTest#testMethod
```

**Key Configuration**:
- Gradle 8.5 (wrapper), Android Gradle Plugin 8.2.0
- Kotlin 1.9.0, compileSdk 34, minSdk 24, targetSdk 34
- Namespace: `com.quantum.poc`
- JVM Target: Java 17, ViewBinding enabled

### test_mock.py (Python Integration Tests)

```bash
# Install dependencies
pip install requests

# Run tests (requires server running at localhost:8101)
python3 test_mock.py
```

**Note**: This is a standalone script, not pytest-based. Exits with code 0 if all tests pass.

---

## Code Style Guidelines

### Java (quantum-mock-encryptor)

**Imports**:
- Standard Java imports first, then framework imports (Spring, Lombok)
- Group imports by package: `java.*`, `javax.*`, `org.*`, `com.*`

**Naming Conventions**:
- Classes: PascalCase (`EncryptorController`, `Sm4Service`)
- Methods/Fields: camelCase (`generateRandom`, `keyData`)
- Constants: UPPER_SNAKE_CASE (`ALGORITHM`, `TRANSFORMATION_ECB`)
- DTOs: Use Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

**Error Handling**:
- Generic exception catching with descriptive messages
- Return structured error responses: `Result.error(code, message)`
- Chinese error messages for user-facing strings

**Code Structure**:
```java
@Service
public class Sm4Service {
    private static final String ALGORITHM = "SM4";
    
    // Constructor injection (no @Autowired on fields)
    public Sm4Service(Dependency dep) { ... }
    
    // Public methods first, private helpers last
    public String encrypt(...) throws Exception { ... }
    private String encryptEcb(...) throws Exception { ... }
}
```

**Annotations**:
- `@Service`, `@RestController`, `@RequestMapping` for Spring components
- `@Valid` for request validation
- `@CrossOrigin` for CORS (currently `origins = "*"`)

### Kotlin (quantum-client-android)

**Imports**:
- Standard library first, then AndroidX, then third-party (Retrofit, Gson)
- Use wildcard imports sparingly

**Naming Conventions**:
- Classes/Objects: PascalCase (`ApiClient`, `CryptoViewModel`)
- Properties/Functions: camelCase (`cryptoApiService`, `newSession()`)
- Constants: Upper camelCase in objects (`private const val BASE_URL`)
- Data classes for models with `@SerializedName` annotations

**Code Style**:
- 4-space indentation, no tabs
- Max line length: 120 characters
- Trailing commas in multi-line collections
- Explicit type declarations for public APIs

**Architecture Patterns**:
- MVVM with ViewModel + LiveData
- Repository pattern for API access (`ApiClient` object)
- Coroutines for async operations (`kotlinx-coroutines-android`)
- ViewBinding for UI (`private lateinit var binding: ActivityMainBinding`)

**Error Handling**:
- Sealed classes or Result types for UI state (`CryptoUiState`)
- Null safety with `?.let {}` and Elvis operator `?:`
- Retrofit callbacks with explicit success/failure handling

**Example**:
```kotlin
object ApiClient {
    private const val BASE_URL = "http://192.168.31.5:8080/"
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val cryptoApiService: CryptoApiService = retrofit.create(...)
}
```

### Python (test_mock.py)

**Style**:
- Follow PEP 8 conventions
- 4-space indentation
- Function names: snake_case (`test_endpoint`, `main`)
- Constants: UPPER_CASE (`BASE_URL`)

**Error Handling**:
- Try-except blocks with specific exception logging
- Return exit codes: 0 for success, 1 for failure
- Descriptive error messages with emoji indicators (✅/❌)

---

## Project Conventions

### API Response Format

All server endpoints return consistent JSON structure:
```json
{
  "code": 0,
  "data": { ... },
  "msg": ""
}
```
- `code: 0` indicates success
- Non-zero `code` indicates error with `msg` description

### Hex Data Encoding

All cryptographic data (keys, ciphertexts, IVs) use lowercase hex strings:
- Keys: 32-char hex (128-bit), 64-char hex (256-bit)
- Data: Variable length hex strings
- No `0x` prefix

### Endpoint Naming

RESTful naming with Chinese-style operation names:
- `/scyh-server/v101/genRandom` - Generate random bytes
- `/scyh-server/v101/symAlgEnc` - Symmetric algorithm encrypt
- `/scyh-server/v101/symAlgDec` - Symmetric algorithm decrypt
- `/scyh-server/v101/hash` - Hash function
- `/scyh-server/v101/hmac` - HMAC generation
- `/scyh-server/v101/genEccKeyPair` - ECC key generation
- `/scyh-server/v101/genPqcKeyPair` - PQC key generation

---

## Development Workflow

### Adding New Features

1. **Server (Java)**:
   - Add DTO in `quantum-mock-encryptor/src/main/java/com/quantum/mock/dto/`
   - Add service logic in `.../service/`
   - Add controller endpoint in `.../controller/`
   - Use constructor injection, not field injection

2. **Android (Kotlin)**:
   - Add API model in `model/ApiModels.kt`
   - Add API interface in `api/CryptoApiService.kt`
   - Update ViewModel with new LiveData/state
   - Update UI in `ui/MainActivity.kt`

3. **Testing**:
   - Server: Add JUnit tests in `src/test/java/`
   - Android: Add unit tests in `app/src/test/java/` or instrumentation tests in `app/src/androidTest/java/`
   - Integration: Update `test_mock.py` for new endpoints

### Git Workflow

- `.gitignore` excludes: `target/`, `*.class`, `.idea/`, `.gradle/`, `build/`, `*.log`
- Commit messages: Conventional Commits format recommended
- Branch naming: `feature/xxx`, `fix/xxx`, `refactor/xxx`

---

## Troubleshooting

### Server Won't Start
- Check port 28101 is not in use
- Verify Java 17 is installed: `java -version`
- Run: `mvn clean` then `mvn spring-boot:run`

### Android Build Fails
- Sync Gradle: `./gradlew --refresh-dependencies`
- Clean build: `./gradlew clean build`
- Check JDK 17 is configured in Android Studio

### Integration Tests Fail
- Ensure server is running: `curl http://localhost:8101/scyh-server/v101/genRandom`
- Check BASE_URL in `test_mock.py` matches server address
- Verify `requests` library: `pip install --upgrade requests`

---

## File Structure Reference

```
quantum/
├── quantum-mock-encryptor/
│   ├── pom.xml
│   ├── src/main/java/com/quantum/mock/
│   │   ├── controller/EncryptorController.java
│   │   ├── dto/*.java (Request/Response DTOs)
│   │   └── service/*.java (Business logic)
│   └── src/main/resources/application.yml
├── quantum-client-android/
│   ├── build.gradle (root)
│   ├── settings.gradle
│   ├── gradle.properties
│   ├── app/build.gradle
│   └── app/src/main/java/com/quantum/poc/
│       ├── api/ApiClient.kt, CryptoApiService.kt
│       ├── model/ApiModels.kt
│       ├── ui/MainActivity.kt
│       └── viewmodel/CryptoViewModel.kt
├── quantum-server/ (empty)
├── quantum-encryptor-gateway/ (empty)
├── requirements/ (documentation)
└── test_mock.py
```

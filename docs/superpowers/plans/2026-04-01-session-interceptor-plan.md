# Session 拦截器与密钥交换实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现服务端会话拦截器，验证 X-Session-Id，并新增 genRandom 接口支持 POC 密钥交换流程

**Architecture:** 使用 Spring WebFlux 的 WebFilter 实现统一拦截，通过 ServerWebExchange.attributes 传递会话数据给后续 handler

**Tech Stack:** Spring Boot 3.x, WebFlux, Java 17

---

## Chunk 1: 服务端拦截器实现

### Task 1: 创建 SessionFilter (WebFilter)

**Files:**
- Create: `quantum-server/src/main/java/com/quantum/poc/config/SessionFilter.java`

```java
package com.quantum.poc.config;

import com.quantum.poc.model.CryptoSession;
import com.quantum.poc.service.SessionService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Order(1)
public class SessionFilter implements WebFilter {
    
    private static final String SESSION_HEADER = "X-Session-Id";
    private static final String SESSION_ATTR = "cryptoSession";
    
    // 不需要 sessionId 的接口
    private static final List<String> EXEMPT_PATHS = List.of(
        "/api/crypto/session/init",
        "/api/crypto/session/genRandom",
        "/api/crypto/genRandom"
    );

    private final SessionService sessionService;

    public SessionFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 检查是否需要 sessionId
        if (requiresSession(path)) {
            String sessionId = exchange.getRequest().getHeaders().getFirst(SESSION_HEADER);
            
            if (sessionId == null || sessionId.isBlank()) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(
                        "{\"code\":401,\"msg\":\"Missing X-Session-Id header\"}".getBytes()
                    )
                ));
            }
            
            // 验证 session 并存入 exchange
            var sessionOpt = sessionService.getSession(sessionId);
            if (sessionOpt.isEmpty()) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(
                        "{\"code\":401,\"msg\":\"Invalid or expired session\"}".getBytes()
                    )
                ));
            }
            
            exchange.getAttributes().put(SESSION_ATTR, sessionOpt.get());
        }
        
        return chain.filter(exchange);
    }
    
    private boolean requiresSession(String path) {
        return EXEMPT_PATHS.stream().noneMatch(path::startsWith);
    }
}
```

- [ ] **Step 1: 创建 SessionFilter.java**

- [ ] **Step 2: 编译验证**

Run: `cd quantum-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add quantum-server/src/main/java/com/quantum/poc/config/SessionFilter.java
git commit -m "feat(server): Add SessionFilter for X-Session-Id validation"
```

---

### Task 2: 修改 SessionController 添加 genRandom 接口

**Files:**
- Modify: `quantum-server/src/main/java/com/quantum/poc/controller/SessionController.java`

- [ ] **Step 1: 添加 genRandom 接口**

在 SessionController.java 中添加:

```java
@GetMapping("/genRandom")
public Mono<ResponseEntity<Result<String>>> genRandom(
        @RequestParam(required = false, defaultValue = "32") Integer length) {
    log.info("========== 生成随机数 ==========");
    log.info("长度: {} 字节", length);
    
    return cryptoGatewayService.genRandom(length)
            .map(result -> {
                log.info("随机数生成完成: {}...", result.getData().substring(0, 8));
                return ResponseEntity.ok(result);
            });
}
```

- [ ] **Step 2: 编译验证**

Run: `cd quantum-server && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add quantum-server/src/main/java/com/quantum/poc/controller/SessionController.java
git commit -m "feat(server): Add /session/genRandom endpoint without session requirement"
```

---

## Chunk 2: Android 客户端更新

### Task 3: 更新 Android CryptoApiService

**Files:**
- Modify: `quantum-client-android/app/src/main/java/com/quantum/poc/api/CryptoApiService.kt`

- [ ] **Step 1: 添加 genRandom 接口**

```kotlin
@GET("api/crypto/session/genRandom")
fun sessionGenRandom(@Query("length") length: Int = 32): Call<ApiResult<String>>
```

- [ ] **Step 2: 更新 CryptoViewModel 使用 session 流程**

Modify: `quantum-client-android/app/src/main/java/com/quantum/poc/viewmodel/CryptoViewModel.kt`

新增方法:

```kotlin
fun sessionGenRandom() {
    val sessionId = _sessionData.value?.sessionId
    if (sessionId.isNullOrEmpty()) {
        appendLog("⚠️ 请先创建会话")
        return
    }
    
    _uiState.value = CryptoUiState.Loading
    appendLog("📌 获取随机数...")
    
    apiService.sessionGenRandom().enqueue(object : Callback<ApiResult<String>> {
        override fun onResponse(call: Call<ApiResult<String>>, response: retrofit2.Response<ApiResult<String>>) {
            if (response.isSuccessful && response.body()?.code == 0) {
                val random = response.body()?.data ?: ""
                _sessionData.value = _sessionData.value?.copy(
                    random = random,
                    sessionKey = random
                )
                appendLog("✅ 随机数获取成功: ${random.take(8)}...")
                _uiState.value = CryptoUiState.Success
            } else {
                appendLog("❌ 随机数获取失败")
                _uiState.value = CryptoUiState.Error("获取失败")
            }
        }
        
        override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
            appendLog("❌ 网络错误: ${t.message}")
            _uiState.value = CryptoUiState.Error(t.message ?: "网络错误")
        }
    })
}
```

- [ ] **Step 3: 编译验证**

Run: 在 Android Studio 中编译

- [ ] **Step 4: 提交**

```bash
git add quantum-client-android/app/src/main/java/com/quantum/poc/api/CryptoApiService.kt
git add quantum-client-android/app/src/main/java/com/quantum/poc/viewmodel/CryptoViewModel.kt
git commit -m "feat(android): Add sessionGenRandom and update session flow"
```

---

## 验证步骤

1. 启动服务端: `cd quantum-server && mvn spring-boot:run`
2. 测试 init (无需 header): `curl -X POST http://localhost:8080/api/crypto/session/init -H "Content-Type: application/json" -d '{"kyberAlgorithm":"Kyber512"}'`
3. 测试 genRandom (无需 header): `curl http://localhost:8080/api/crypto/session/genRandom?length=32`
4. 测试加密接口 (无 header): `curl -X POST http://localhost:8080/api/crypto/session/encrypt -H "Content-Type: application/json" -d '{}'`
   - Expected: 401 Unauthorized
5. 测试加密接口 (有 header 但无效): `curl -X POST http://localhost:8080/api/crypto/session/encrypt -H "X-Session-Id: invalid" -H "Content-Type: application/json" -d '{}'`
   - Expected: 401 Unauthorized

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-01-session-interceptor-plan.md`. Ready to execute?**
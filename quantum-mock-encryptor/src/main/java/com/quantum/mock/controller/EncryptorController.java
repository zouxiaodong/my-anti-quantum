package com.quantum.mock.controller;

import com.quantum.mock.dto.*;
import com.quantum.mock.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/scyh-server/v101")
@CrossOrigin(origins = "*")
public class EncryptorController {

    private final RandomService randomService;
    private final Sm4Service sm4Service;
    private final HashService hashService;
    private final HmacService hmacService;
    private final Sm2Service sm2Service;
    private final PqcService pqcService;

    public EncryptorController(RandomService randomService,
                                Sm4Service sm4Service,
                                HashService hashService,
                                HmacService hmacService,
                                Sm2Service sm2Service,
                                PqcService pqcService) {
        this.randomService = randomService;
        this.sm4Service = sm4Service;
        this.hashService = hashService;
        this.hmacService = hmacService;
        this.sm2Service = sm2Service;
        this.pqcService = pqcService;
    }

    @PostMapping("/genRandom")
    public ResponseEntity<Result<String>> genRandom(@RequestParam(required = false, defaultValue = "32") Integer length) {
        String random = randomService.generateRandom(length);
        return ResponseEntity.ok(Result.success(random));
    }

    @PostMapping("/symAlgEnc")
    public ResponseEntity<Result<String>> symAlgEnc(@Valid @RequestBody EncryptRequest request) {
        try {
            String encrypted = sm4Service.encrypt(request.getAlgorithm(), request.getData(), request.getKeyData(), request.getIv());
            return ResponseEntity.ok(Result.success(encrypted));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "加密失败: " + e.getMessage()));
        }
    }

    @PostMapping("/symAlgDec")
    public ResponseEntity<Result<String>> symAlgDec(@Valid @RequestBody EncryptRequest request) {
        try {
            String decrypted = sm4Service.decrypt(request.getAlgorithm(), request.getData(), request.getKeyData(), request.getIv());
            return ResponseEntity.ok(Result.success(decrypted));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "解密失败: " + e.getMessage()));
        }
    }

    @PostMapping("/hash")
    public ResponseEntity<Result<String>> hash(@Valid @RequestBody HashRequest request) {
        try {
            String hashed = hashService.hash(request.getAlgorithm(), request.getData());
            return ResponseEntity.ok(Result.success(hashed));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "哈希失败: " + e.getMessage()));
        }
    }

    @PostMapping("/hmac")
    public ResponseEntity<Result<String>> hmac(@Valid @RequestBody HMacRequest request) {
        try {
            String hmac = hmacService.hmac(request.getData(), request.getKey());
            return ResponseEntity.ok(Result.success(hmac));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "HMAC失败: " + e.getMessage()));
        }
    }

    @PostMapping("/genEccKeyPair")
    public ResponseEntity<Result<Map>> genEccKeyPair() {
        try {
            Map<String, String> keyPair = sm2Service.generateKeyPair();
            return ResponseEntity.ok(Result.success(keyPair));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "密钥对生成失败: " + e.getMessage()));
        }
    }

    @PostMapping("/sm2Enc")
    public ResponseEntity<Result<String>> sm2Enc(@Valid @RequestBody Sm2Request request) {
        try {
            if (request.getPublicKey() == null || request.getPublicKey().isEmpty()) {
                return ResponseEntity.ok(Result.error(1, "SM2加密需要公钥"));
            }
            String encrypted = sm2Service.encrypt(request.getData(), request.getPublicKey());
            return ResponseEntity.ok(Result.success(encrypted));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "SM2加密失败: " + e.getMessage()));
        }
    }

    @PostMapping("/sm2Dec")
    public ResponseEntity<Result<String>> sm2Dec(@Valid @RequestBody Sm2Request request) {
        try {
            if (request.getPrivateKey() == null || request.getPrivateKey().isEmpty()) {
                return ResponseEntity.ok(Result.error(1, "SM2解密需要私钥"));
            }
            String decrypted = sm2Service.decrypt(request.getData(), request.getPrivateKey());
            return ResponseEntity.ok(Result.success(decrypted));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "SM2解密失败: " + e.getMessage()));
        }
    }

    @PostMapping("/genPqcKeyPair")
    public ResponseEntity<Result<Map>> genPqcKeyPair(@Valid @RequestBody KeyPairRequest request) {
        try {
            Map<String, String> keyPair = pqcService.generateKeyPair(request.getAlgorithm());
            return ResponseEntity.ok(Result.success(keyPair));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "PQC密钥对生成失败: " + e.getMessage()));
        }
    }

    @PostMapping("/pqcKeyWrapper")
    public ResponseEntity<Result<PqcKeyWrapperResponse>> pqcKeyWrapper(@Valid @RequestBody PqcKeyWrapperRequest request) {
        try {
            Map<String, String> wrapperResult = pqcService.keyWrapper(request.getAlgorithm(), request.getPqcPubkey());
            PqcKeyWrapperResponse response = new PqcKeyWrapperResponse();
            response.setKeyCipher(wrapperResult.get("keyCipher"));
            response.setKeyId(wrapperResult.get("keyId"));
            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "PQC密钥封装失败: " + e.getMessage()));
        }
    }

    @PostMapping("/pqcKeyUnWrapper")
    public ResponseEntity<Result<String>> pqcKeyUnWrapper(@Valid @RequestBody PqcKeyUnwrapperRequest request) {
        try {
            String unwrapped = pqcService.keyUnwrapper(request.getAlgorithm(), request.getCipherText(), request.getPqcPrikey());
            return ResponseEntity.ok(Result.success(unwrapped));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(1, "PQC密钥解封失败: " + e.getMessage()));
        }
    }
}

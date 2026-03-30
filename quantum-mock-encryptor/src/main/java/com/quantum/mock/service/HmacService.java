package com.quantum.mock.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;

@Service
public class HmacService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public String hmac(String data, String key) throws Exception {
        byte[] keyBytes = hexToBytes(key);
        byte[] dataBytes = hexToBytes(data);

        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSM3");
        Mac mac = Mac.getInstance("HmacSM3", "BC");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(dataBytes);
        return bytesToHex(hmacBytes);
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

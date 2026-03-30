package com.quantum.mock.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;

@Service
public class HashService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public String hash(String algorithm, String data) throws Exception {
        byte[] input = hexToBytes(data);
        MessageDigest digest = MessageDigest.getInstance(mapAlgorithm(algorithm));
        byte[] hash = digest.digest(input);
        return bytesToHex(hash);
    }

    private String mapAlgorithm(String algorithm) {
        return switch (algorithm.toUpperCase()) {
            case "SM3" -> "SM3";
            case "SHA1" -> "SHA-1";
            case "SHA256" -> "SHA-256";
            default -> throw new IllegalArgumentException("Unsupported hash algorithm: " + algorithm);
        };
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return hex != null ? hex.getBytes(StandardCharsets.UTF_8) : new byte[0];
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

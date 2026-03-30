package com.quantum.mock.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Random;

@Service
public class RandomService {

    private final Random random = new SecureRandom();

    public String generateRandom(int length) {
        byte[] randomBytes = new byte[length];
        random.nextBytes(randomBytes);
        return bytesToHex(randomBytes);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

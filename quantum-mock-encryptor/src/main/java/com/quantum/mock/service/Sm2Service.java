package com.quantum.mock.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class Sm2Service {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public Map<String, String> generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(new ECGenParameterSpec("sm2p256v1"), new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        byte[] privateEncoded = keyPair.getPrivate().getEncoded();
        byte[] publicEncoded = keyPair.getPublic().getEncoded();

        Map<String, String> result = new java.util.HashMap<>();
        result.put("privateKey", bytesToHex(privateEncoded));
        result.put("publicKey", bytesToHex(publicEncoded));
        result.put("keyId", java.util.UUID.randomUUID().toString());
        return result;
    }

    public String encrypt(String data, String publicKeyHex) throws Exception {
        byte[] dataBytes = hexToBytes(data);
        byte[] publicKeyBytes = hexToBytes(publicKeyHex);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Cipher cipher = Cipher.getInstance("SM2", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(dataBytes);
        return bytesToHex(encrypted);
    }

    public String decrypt(String data, String privateKeyHex) throws Exception {
        byte[] dataBytes = hexToBytes(data);
        byte[] privateKeyBytes = hexToBytes(privateKeyHex);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Cipher cipher = Cipher.getInstance("SM2", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(dataBytes);
        return bytesToHex(decrypted);
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

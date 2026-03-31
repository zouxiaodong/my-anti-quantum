package com.quantum.mock.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

@Service
public class Sm2Service {

    private static final String SM2_SIGNATURE_ALGORITHM = "SM3withSM2";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public Map<String, String> generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
        keyGen.initialize(new ECGenParameterSpec("sm2p256v1"), new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        byte[] privateEncoded = keyPair.getPrivate().getEncoded();
        byte[] publicEncoded = keyPair.getPublic().getEncoded();

        Map<String, String> result = new HashMap<>();
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

    public String sign(String data, String privateKeyHex) throws Exception {
        byte[] dataBytes = hexToBytes(data);
        byte[] privateKeyBytes = hexToBytes(privateKeyHex);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance(SM2_SIGNATURE_ALGORITHM, "BC");
        signature.initSign(privateKey);
        signature.update(dataBytes);
        byte[] signed = signature.sign();
        return bytesToHex(signed);
    }

    public boolean verify(String data, String signatureHex, String publicKeyHex) throws Exception {
        byte[] dataBytes = hexToBytes(data);
        byte[] signatureBytes = hexToBytes(signatureHex);
        byte[] publicKeyBytes = hexToBytes(publicKeyHex);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Signature signature = Signature.getInstance(SM2_SIGNATURE_ALGORITHM, "BC");
        signature.initVerify(publicKey);
        signature.update(dataBytes);
        return signature.verify(signatureBytes);
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

package com.quantum.mock.service;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;

@Service
public class Sm4Service {

    private static final String ALGORITHM = "SM4";
    private static final String TRANSFORMATION_ECB = "SM4/ECB/NoPadding";
    private static final String TRANSFORMATION_CBC = "SM4/CBC/NoPadding";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public String encrypt(String algorithm, String data, String keyData, String iv) throws Exception {
        byte[] key = hexToBytes(keyData);
        byte[] input = hexToBytes(data);

        if (algorithm.contains("ECB")) {
            return encryptEcb(input, key);
        } else if (algorithm.contains("CBC")) {
            byte[] ivBytes = hexToBytes(iv);
            return encryptCbc(input, key, ivBytes);
        } else {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    public String decrypt(String algorithm, String data, String keyData, String iv) throws Exception {
        byte[] key = hexToBytes(keyData);
        byte[] input = hexToBytes(data);

        if (algorithm.contains("ECB")) {
            return decryptEcb(input, key);
        } else if (algorithm.contains("CBC")) {
            byte[] ivBytes = hexToBytes(iv);
            return decryptCbc(input, key, ivBytes);
        } else {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    private String encryptEcb(byte[] data, byte[] key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_ECB, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(data);
        return bytesToHex(encrypted);
    }

    private String decryptEcb(byte[] data, byte[] key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_ECB, "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(data);
        return bytesToHex(decrypted);
    }

    private String encryptCbc(byte[] data, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_CBC, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = cipher.doFinal(data);
        return bytesToHex(encrypted);
    }

    private String decryptCbc(byte[] data, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION_CBC, "BC");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decrypted = cipher.doFinal(data);
        return bytesToHex(decrypted);
    }

    public static byte[] hexToBytes(String hex) {
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

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

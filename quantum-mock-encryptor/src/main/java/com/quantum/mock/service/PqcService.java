package com.quantum.mock.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyPairGenerator;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PqcService {

    private static final Map<String, MLKEMParameters> KEM_ALGORITHMS;
    private static final Map<String, DilithiumParameters> SIGN_ALGORITHMS;
    
    static {
        KEM_ALGORITHMS = new HashMap<>();
        KEM_ALGORITHMS.put("kyber512", MLKEMParameters.ml_kem_512);
        KEM_ALGORITHMS.put("kyber768", MLKEMParameters.ml_kem_768);
        KEM_ALGORITHMS.put("kyber1024", MLKEMParameters.ml_kem_1024);
        
        SIGN_ALGORITHMS = new HashMap<>();
        SIGN_ALGORITHMS.put("dilithium2", DilithiumParameters.dilithium2);
        SIGN_ALGORITHMS.put("dilithium3", DilithiumParameters.dilithium3);
        SIGN_ALGORITHMS.put("dilithium5", DilithiumParameters.dilithium5);
    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public Map<String, String> generateKeyPair(String algorithm) throws Exception {
        String lower = algorithm.toLowerCase();

        if (KEM_ALGORITHMS.containsKey(lower)) {
            return generateKyberKeyPair(algorithm);
        } else if (SIGN_ALGORITHMS.containsKey(lower)) {
            return generateDilithiumKeyPair(algorithm);
        } else {
            throw new IllegalArgumentException("Unsupported PQC algorithm: " + algorithm);
        }
    }

    public Map<String, String> keyWrapper(String algorithm, String pqcPubkey) throws Exception {
        String lower = algorithm.toLowerCase();

        if (!KEM_ALGORITHMS.containsKey(lower)) {
            throw new IllegalArgumentException("Unsupported KEM algorithm: " + algorithm);
        }

        MLKEMParameters params = KEM_ALGORITHMS.get(lower);
        byte[] publicKeyBytes = hexToBytes(pqcPubkey);
        MLKEMPublicKeyParameters publicKey = new MLKEMPublicKeyParameters(params, publicKeyBytes);

        MLKEMGenerator generator = new MLKEMGenerator(new SecureRandom());
        org.bouncycastle.crypto.SecretWithEncapsulation encap = generator.generateEncapsulated(publicKey);
        byte[] ciphertext = encap.getEncapsulation();
        byte[] sharedSecret = encap.getSecret();

        Map<String, String> result = new HashMap<>();
        result.put("keyCipher", bytesToHex(ciphertext));
        result.put("keyId", UUID.randomUUID().toString());
        result.put("sharedSecret", bytesToHex(sharedSecret));
        return result;
    }

    public String keyUnwrapper(String algorithm, String cipherText, String pqcPrikey) throws Exception {
        String lower = algorithm.toLowerCase();

        if (!KEM_ALGORITHMS.containsKey(lower)) {
            throw new IllegalArgumentException("Unsupported KEM algorithm: " + algorithm);
        }

        MLKEMParameters params = KEM_ALGORITHMS.get(lower);
        byte[] cipherBytes = hexToBytes(cipherText);
        byte[] privateKeyBytes = hexToBytes(pqcPrikey);
        
        MLKEMPrivateKeyParameters privateKey = new MLKEMPrivateKeyParameters(params, privateKeyBytes);
        MLKEMExtractor extractor = new MLKEMExtractor(privateKey);
        byte[] sharedSecret = extractor.extractSecret(cipherBytes);

        return bytesToHex(sharedSecret);
    }

    private Map<String, String> generateKyberKeyPair(String algorithm) throws Exception {
        MLKEMParameters params = KEM_ALGORITHMS.get(algorithm.toLowerCase());
        
        MLKEMKeyPairGenerator generator = new MLKEMKeyPairGenerator();
        generator.init(new MLKEMKeyGenerationParameters(new SecureRandom(), params));
        
        AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
        
        MLKEMPublicKeyParameters publicKey = (MLKEMPublicKeyParameters) keyPair.getPublic();
        MLKEMPrivateKeyParameters privateKey = (MLKEMPrivateKeyParameters) keyPair.getPrivate();

        Map<String, String> result = new HashMap<>();
        result.put("publicKey", bytesToHex(publicKey.getEncoded()));
        result.put("privateKey", bytesToHex(privateKey.getEncoded()));
        result.put("keyId", UUID.randomUUID().toString());
        return result;
    }

    private Map<String, String> generateDilithiumKeyPair(String algorithm) throws Exception {
        DilithiumParameters params = SIGN_ALGORITHMS.get(algorithm.toLowerCase());
        
        DilithiumKeyPairGenerator generator = new DilithiumKeyPairGenerator();
        generator.init(new DilithiumKeyGenerationParameters(new SecureRandom(), params));
        
        AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
        
        DilithiumPublicKeyParameters publicKey = (DilithiumPublicKeyParameters) keyPair.getPublic();
        DilithiumPrivateKeyParameters privateKey = (DilithiumPrivateKeyParameters) keyPair.getPrivate();

        Map<String, String> result = new HashMap<>();
        result.put("publicKey", bytesToHex(publicKey.getEncoded()));
        result.put("privateKey", bytesToHex(privateKey.getEncoded()));
        result.put("keyId", UUID.randomUUID().toString());
        return result;
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[0];
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

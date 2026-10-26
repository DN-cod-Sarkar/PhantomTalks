package com.phantom.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESCipher implements ICipher {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int AES_KEY_BIT = 256;

    private SecretKey secretKey;

    // Constructor generating a new secure random 256-bit key
    public AESCipher() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_BIT, new SecureRandom());
        this.secretKey = keyGen.generateKey();
    }

    // Constructor loading an existing key (Useful for testing or RSA key exchange)
    public AESCipher(byte[] keyBytes) {
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] getExportableKey() {
        return secretKey.getEncoded();
    }

    @Override
    public String encrypt(String plainText) throws Exception {
        // 1. Generate a new random Initialization Vector (IV) for every message
        byte[] iv = new byte[IV_LENGTH_BYTE];
        new SecureRandom().nextBytes(iv);

        // 2. Setup the Cipher for GCM mode
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        // 3. Encrypt the data
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 4. Combine IV and CipherText into one payload so the receiver knows the IV
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);

        // 5. Encode to Base64 for safe transport over the socket
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    @Override
    public String decrypt(String cipherTextBase64) throws Exception {
        // 1. Decode Base64 string back to bytes
        byte[] combinedData = Base64.getDecoder().decode(cipherTextBase64);

        // 2. Extract the 12-byte IV from the front of the payload
        ByteBuffer byteBuffer = ByteBuffer.wrap(combinedData);
        byte[] iv = new byte[IV_LENGTH_BYTE];
        byteBuffer.get(iv);

        // 3. Extract the remaining bytes as the actual ciphertext
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        // 4. Setup Cipher to Decrypt using the extracted IV
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        // 5. Decrypt and verify the Authentication Tag simultaneously
        byte[] plainText = cipher.doFinal(cipherText);

        return new String(plainText, StandardCharsets.UTF_8);
    }
}
package com.phantom.security;

public interface ICipher {
    String encrypt(String plainText) throws Exception;
    String decrypt(String cipherTextBase64) throws Exception;
}
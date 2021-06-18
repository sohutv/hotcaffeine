package com.hotcaffeine.dashboard.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 加密解密助手
 * 
 * @author yongfeigao
 * @date 2018年10月9日
 */
public class CipherHelper {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String ALGORITHM = "AES";

    private SecretKeySpec secretKeySpec;

    public CipherHelper(String ciperKey) throws UnsupportedEncodingException {
        secretKeySpec = new SecretKeySpec(ciperKey.getBytes("UTF-8"), ALGORITHM);
    }

    /**
     * 加密
     * 
     * @param toBeEncrypt
     * @return
     */
    public String encrypt(String toBeEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(toBeEncrypt.getBytes());
            return new String(Base64.getEncoder().encode(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("encrypt:{} err", toBeEncrypt, e);
        }
        return null;
    }

    /**
     * 解密
     * 
     * @param encrypted
     * @return
     */
    public String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decryptedBytes = cipher
                    .doFinal(Base64.getDecoder().decode(encrypted.getBytes(StandardCharsets.UTF_8)));
            return new String(decryptedBytes);
        } catch (Exception e) {
            logger.error("decrypt:{} err", encrypted, e);
        }
        return null;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        CipherHelper cipherHelper = new CipherHelper("DSJiSEvR5yAC5fct");
        System.out.println(cipherHelper.encrypt("admin"));
    }
}

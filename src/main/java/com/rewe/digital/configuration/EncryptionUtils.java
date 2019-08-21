package com.rewe.digital.configuration;

import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class EncryptionUtils {

    public static String encrypt(String strToEncrypt, String secret) {
        if (StringUtils.isNotBlank(strToEncrypt)) {
            try {
                val secretKey = makeKey(secret);
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(UTF_8)));
            } catch (Exception e) {
                System.out.println("Error while encrypting: " + e.toString());
            }
            return null;
        } else {
            return "";
        }
    }

    public static String decrypt(String strToDecrypt, String secret) {
        if (StringUtils.isNotBlank(strToDecrypt)) {
            try {
                val secretKey = makeKey(secret);
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
            } catch (Exception e) {
                System.out.println("Error while decrypting: " + e.toString());
            }
            return null;
        } else {
            return "";
        }
    }

    private static SecretKeySpec makeKey(String myKey) throws NoSuchAlgorithmException {
        byte[] key = myKey.getBytes(UTF_8);
        val sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }
}


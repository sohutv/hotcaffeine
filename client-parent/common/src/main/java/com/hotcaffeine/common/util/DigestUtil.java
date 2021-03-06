package com.hotcaffeine.common.util;

import java.security.MessageDigest;

/**
 * 工具
 * 
 * @author yongfeigao
 * @date 2021年1月28日
 */
public class DigestUtil {
    
    // 客户端加密码
    public static final String CLIENT_CIPHER_CODE = "@HOTKEY@";

    /**
     * Used to build output as Hex
     */
    private static final char[] DIGITS_LOWER = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
            'e', 'f'};
    
    /**
     * 加密
     * @param str
     * @return
     */
    public static String encode(String str) {
        return md5Hex(str + CLIENT_CIPHER_CODE);
    }

    public static String md5Hex(String str) {
        try {
            return new String(encodeHex(MessageDigest.getInstance("MD5").digest(str.getBytes())));
        } catch (Exception e) {
            throw new IllegalArgumentException(str);
        }
    }

    /**
     * Converts an array of bytes into an array of characters representing the
     * hexadecimal values of each byte in order. The returned array will be
     * double the length of the passed array, as it takes two characters to
     * represent any given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @return A char[] containing hexadecimal characters
     * @since 1.4
     */
    public static char[] encodeHex(final byte[] data) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_LOWER[0x0F & data[i]];
        }
        return out;
    }
}

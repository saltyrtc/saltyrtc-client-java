package org.saltyrtc.client;

import java.util.Random;

public class Utils {
    public static String getRandomString() {
        int length = 32;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        return getRandomString(length, chars);
    }

    public static String getRandomString(int length, String charsString) {
        char[] chars = charsString.toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        return sb.toString();
    }
}

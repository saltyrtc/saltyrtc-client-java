/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

public class HexHelper {

    /**
     * Convert a hex string to a byte array.
     * http://stackoverflow.com/a/19119453
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Convert a byte array to a hex string.
     * https://stackoverflow.com/a/15429408
     */
    public static String byteArrayToHexString(byte[] b) {
        final StringBuilder data = new StringBuilder();
        for (byte value : b) {
            data.append(String.format("%02x", value));
        }
        return data.toString();
    }

}

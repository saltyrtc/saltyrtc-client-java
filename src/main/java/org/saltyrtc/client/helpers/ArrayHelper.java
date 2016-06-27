/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

public class ArrayHelper {

    /**
     * Concatenate two byte arrays.
     *
     * Source: http://stackoverflow.com/a/80503/284318
     */
    public static byte[] concat(byte[] a, byte[] b) {
        int aLength = a.length;
        int bLength = b.length;
        byte[] c = new byte[aLength + bLength];
        System.arraycopy(a, 0, c, 0, aLength);
        System.arraycopy(b, 0, c, aLength, bLength);
        return c;
    }

}
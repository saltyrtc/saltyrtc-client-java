/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

/**
 * Tools for signed/unsigned conversion.
 *
 * Oh, Java. I thought I'd never need to manually apply two's complement anymore :(
 */
public class UnsignedHelper {

    public static short readUnsignedByte(byte val) {
        return (short)(((int)val) & 0x000000FF);
    }

    public static int readUnsignedShort(short val) {
        return ((int)val) & 0xFFFF;
    }

    public static long readUnsignedInt(int val) {
        return ((long)val) & 0xFFFFFFFFL;
    }

    public static byte getUnsignedByte(short val) {
        if (val >= (1 << 8)) {
            throw new IllegalArgumentException("Value is too large to fit in a byte");
        }
        if (val < 0) {
            throw new IllegalArgumentException("Value must not be negative");
        }
        return (byte) val;
    }

    public static short getUnsignedShort(int val) {
        if (val >= (1 << 16)) {
            throw new IllegalArgumentException("Value is too large to fit in a short");
        }
        if (val < 0) {
            throw new IllegalArgumentException("Value must not be negative");
        }
        return (short) val;
    }

    public static int getUnsignedInt(long val) {
        if (val >= (1L << 32)) {
            throw new IllegalArgumentException("Value is too large to fit in an int");
        }
        if (val < 0) {
            throw new IllegalArgumentException("Value must not be negative");
        }
        return (int) val;
    }

}

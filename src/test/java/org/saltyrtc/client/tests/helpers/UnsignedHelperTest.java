/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.helpers;

import org.junit.Test;
import org.saltyrtc.client.helpers.UnsignedHelper;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class UnsignedHelperTest {

    @Test
    public void testReadUnsignedByte() {
        byte b1 = 1; // 0x01
        byte b2 = -126; // 0x82

        // Small positive numbers stay positive
        assertEquals(0x01, b1);

        // Numbers >127 become negative
        assertNotEquals(0x82, b2);

        // Convert and verify
        short u1 = UnsignedHelper.readUnsignedByte(b1);
        short u2 = UnsignedHelper.readUnsignedByte(b2);
        assertEquals(0x01, u1);
        assertEquals(0x82, u2);
    }

    @Test
    public void testReadUnsignedShort() {
        byte hi = -128; // 0x80
        byte lo = 1; // 0x01
        byte[] bytes = new byte[] { hi, lo };
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        short val = buf.getShort();
        assertNotEquals((0x80 << 8) + 1, val);

        int unsigned = UnsignedHelper.readUnsignedShort(val);
        assertEquals((0x80 << 8) + 1, unsigned);
    }

    @Test
    public void testReadUnsignedInt() {
        byte hi = -128; // 0x80
        byte lo = 1; // 0x01
        byte[] bytes = new byte[] { hi, 0, 0, lo };
        ByteBuffer buf = ByteBuffer.wrap(bytes);

        int val = buf.getInt();
        assertNotEquals(2147483649L, val);

        long unsigned = UnsignedHelper.readUnsignedInt(val);
        assertEquals(2147483649L, unsigned);
    }

    @Test
    public void testGetUnsignedByte() {
        short s1 = 0x01; // 1
        short s2 = 0x82; // -126
        byte b1 = UnsignedHelper.getUnsignedByte(s1);
        byte b2 = UnsignedHelper.getUnsignedByte(s2);
        assertEquals(1, b1);
        assertEquals(-126, b2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnsignedByteTooLarge() {
        UnsignedHelper.getUnsignedByte((short) 256 /* 1 << 8 */);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnsignedByteNegative() {
        UnsignedHelper.getUnsignedByte((short) -1);
    }

    @Test
    public void testGetUnsignedShort() {
        int i1 = 0x0001; // 1
        int i2 = 0x8002; // -32766
        short s1 = UnsignedHelper.getUnsignedShort(i1);
        short s2 = UnsignedHelper.getUnsignedShort(i2);
        assertEquals(1, s1);
        assertEquals(-32766, s2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnsignedShortTooLarge() {
        UnsignedHelper.getUnsignedShort(65536 /* 1 << 16 */);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnsignedShortNegative() {
        UnsignedHelper.getUnsignedShort(-1);
    }

    @Test
    public void testGetUnsignedInt() {
        long l1 = 0x00000001L; // 1
        long l2 = 0x80000002L; // -2147483646
        int i1 = UnsignedHelper.getUnsignedInt(l1);
        int i2 = UnsignedHelper.getUnsignedInt(l2);
        assertEquals(1, i1);
        assertEquals(-2147483646, i2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnsignedIntTooLarge() {
        UnsignedHelper.getUnsignedInt(4294967296L /* 1 << 32 */);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetUnsignedIntNegative() {
        UnsignedHelper.getUnsignedInt(-1);
    }

}

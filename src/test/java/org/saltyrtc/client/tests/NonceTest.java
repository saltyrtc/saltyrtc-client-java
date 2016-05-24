/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.saltyrtc.client.Nonce;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class NonceTest {

    private static byte[] cookie = new byte[Nonce.COOKIE_LENGTH];

    @BeforeClass
    public static void setUpStatic() {
        for (int i = 0; i < Nonce.COOKIE_LENGTH; i++) {
            cookie[i] = (byte)i;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonceCookieValidation() {
        new Nonce(new byte[] { 0x00, 0x01, 0x02, 0x03 }, 0, 1, 0);
    }

    @Test
    public void testNonceChannelValidation() {
        int[] invalid = new int[] { -1, 1 << 16 };
        int[] valid = new int[] { 0, 1 << 16 - 1 };
        for (int value : invalid) {
            try {
                new Nonce(cookie, value, 1, 0);
                fail("Did not raise IllegalArgumentException for value " + value);
            } catch (IllegalArgumentException ignored) {}
        }
        for (int value : valid) {
            new Nonce(cookie, value, 1, 0);
        }
    }

    @Test
    public void testNonceOverflowValidation() {
        int[] invalid = new int[] { -1, 1 << 16 };
        int[] valid = new int[] { 0, 1 << 16 - 1 };
        for (int value : invalid) {
            try {
                new Nonce(cookie, 0, value, 0);
                fail("Did not raise IllegalArgumentException for value " + value);
            } catch (IllegalArgumentException ignored) {}
        }
        for (int value : valid) {
            new Nonce(cookie, 0, value, 0);
        }
    }

    @Test
    public void testNonceSequenceValidation() {
        long[] invalid = new long[] { -1, 1L << 32 };
        long[] valid = new long[] { 0, 1L << 32 - 1 };
        for (long value : invalid) {
            try {
                new Nonce(cookie, 0, 1, value);
                fail("Did not raise IllegalArgumentException for value " + value);
            } catch (IllegalArgumentException ignored) {}
        }
        for (long value : valid) {
            new Nonce(cookie, 0, 1, value);
        }
    }

    /**
     * A nonce must be 24 bytes long.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNonceBytesValidation() {
        new Nonce(ByteBuffer.wrap(new byte[] { 0x0 }));
    }

    /**
     * Test the unsigned 16 bit arithmetic used to implement the nonce en-/decoding.
     */
    @Test
    public void testArithmeticShort() {
        byte hi = -128; // 0x80
        byte lo = 1; // 0x01
        byte[] bytes = new byte[] { hi, lo };
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        short val = buf.getShort();
        assertNotEquals((0x80 << 8) + 1, val);
        int unsigned = ((int)val) & 0xFFFF;
        assertEquals((0x80 << 8) + 1, unsigned);
    }

    /**
     * Test the unsigned 32 bit arithmetic used to implement the nonce en-/decoding.
     */
    @Test
    public void testArithmeticInt() {
        byte hi = -128; // 0x80
        byte lo = 1; // 0x01
        byte[] bytes = new byte[] { hi, 0, 0, lo };
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int val = buf.getInt();
        assertNotEquals(2147483649L, val);
        long unsigned = ((long)val) & 0xFFFFFFFFL;
        assertEquals(2147483649L, unsigned);
    }

    @Test
    public void testNonceBytesConstructor() {
        byte[] bytes = new byte[] {
            // Cookie
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            // Data channel id (0x8001)
            -128, 1,
            // Overflow (0x8002)
            -128, 2,
            // Sequence (0x80000003
            -128, 0, 0, 3,
        };
        final Nonce nonce = new Nonce(ByteBuffer.wrap(bytes));
        assertEquals(0x8001, nonce.getChannelId());
        assertEquals(0x8002, nonce.getOverflow());
        assertEquals(0x80000003L, nonce.getSequence());
    }

    @Test
    public void testCombinedSequence() {
        final Nonce nonce = new Nonce(cookie, 0, 0x8000, 42L);
        // (0x8000 << 32) + 42 = 140737488355370
        assertEquals(140737488355370L, nonce.getCombinedSequence());
    }

}
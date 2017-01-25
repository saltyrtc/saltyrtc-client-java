/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.nonce;

import org.junit.BeforeClass;
import org.junit.Test;
import org.saltyrtc.client.nonce.SignalingChannelNonce;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SignalingChannelNonceTest {

    // General nonce related functionality is already covered by DataChannelNonceTest

    private static byte[] cookie = new byte[SignalingChannelNonce.COOKIE_LENGTH];

    @BeforeClass
    public static void setUpStatic() {
        for (int i = 0; i < SignalingChannelNonce.COOKIE_LENGTH; i++) {
            cookie[i] = (byte)i;
        }
    }

    @Test
    public void testNonceSourceValidation() {
        short[] invalid = new short[] { -1, 1 << 8 };
        short[] valid = new short[] { 0, 1 << 8 - 1 };
        for (short value : invalid) {
            try {
                new SignalingChannelNonce(cookie, value, (short)0, 1, 0);
                fail("Did not raise IllegalArgumentException for value " + value);
            } catch (IllegalArgumentException ignored) {}
        }
        for (short value : valid) {
            new SignalingChannelNonce(cookie, value, (short)0, 1, 0);
        }
    }

    @Test
    public void testNonceDestinationValidation() {
        short[] invalid = new short[] { -1, 1 << 8 };
        short[] valid = new short[] { 0, 1 << 8 - 1 };
        for (short value : invalid) {
            try {
                new SignalingChannelNonce(cookie, (short)0, value, 1, 0);
                fail("Did not raise IllegalArgumentException for value " + value);
            } catch (IllegalArgumentException ignored) {}
        }
        for (short value : valid) {
            new SignalingChannelNonce(cookie, (short)0, value, 1, 0);
        }
    }

    @Test
    public void testNonceBytesConstructor() {
        byte[] bytes = new byte[] {
            // Cookie
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            // Source (0x81)
            -127,
            // Destination (0x82)
            -126,
            // Overflow (0x8002)
            -128, 2,
            // Sequence (0x80000003)
            -128, 0, 0, 3,
        };
        final SignalingChannelNonce nonce = new SignalingChannelNonce(ByteBuffer.wrap(bytes));
        assertEquals(0x8002, nonce.getOverflow());
        assertEquals(0x81, nonce.getSource());
        assertEquals(0x82, nonce.getDestination());
        assertEquals(0x80000003L, nonce.getSequence());
    }

    @Test
    public void testByteConversionRoundtrip() {
        byte[] bytes = new byte[] {
            // Cookie
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
            // Source (0x81)
            -127,
            // Destination (0x82)
            -126,
            // Overflow (0x8002)
            -128, 2,
            // Sequence (0x80000003)
            -128, 0, 0, 3,
        };
        final SignalingChannelNonce nonce = new SignalingChannelNonce(ByteBuffer.wrap(bytes));
        byte[] bytesAgain = nonce.toBytes();
        assertArrayEquals(bytes, bytesAgain);
    }

}
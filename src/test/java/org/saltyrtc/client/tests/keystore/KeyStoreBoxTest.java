/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.keystore;

import org.junit.BeforeClass;
import org.junit.Test;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class KeyStoreBoxTest {

    private static byte[] nonce = new byte[NaCl.NONCEBYTES];
    private static byte[] data = new byte[] { 0x00, 0x01, 0x02, 0x03 };

    @BeforeClass
    public static void oneTimeSetUp() {
        // Initialize nonce
        for (int i = 0; i < NaCl.NONCEBYTES; i++) {
            nonce[i] = (byte) i;
        }
    }

    @Test
    public void testCreationFromNonceAndData() {
        final Box box = new Box(nonce, data);
        assertArrayEquals(box.getData(), data);
        assertArrayEquals(box.getNonce(), nonce);
    }

    @Test
    public void testCreationFromByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(nonce.length + data.length);
        buf.put(nonce);
        buf.put(data);
        buf.flip();
        final Box box = new Box(buf, NaCl.NONCEBYTES);
        assertArrayEquals(box.getData(), data);
        assertArrayEquals(box.getNonce(), nonce);
    }

    @Test
    public void testGetSize() {
        final Box box1 = new Box(nonce, data);
        final Box box2 = new Box(nonce, new byte[] { 0x23, 0x42 });
        assertEquals(box1.getSize(), nonce.length + data.length);
        assertEquals(box2.getSize(), nonce.length + 2);

    }
}

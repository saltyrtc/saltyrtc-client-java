/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.neilalexander.jnacl.NaCl;

import org.saltyrtc.client.KeyStore;

import java.nio.ByteBuffer;

public class KeyStoreBoxTest {

    private static byte[] nonce = new byte[NaCl.NONCEBYTES];;
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
        final KeyStore.Box box = new KeyStore.Box(nonce, data);
        assertArrayEquals(box.getData(), data);
        assertArrayEquals(box.getNonce(), nonce);
    }

    @Test
    public void testCreationFromByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(nonce.length + data.length);
        buf.put(nonce);
        buf.put(data);
        buf.flip();
        final KeyStore.Box box = new KeyStore.Box(buf);
        assertArrayEquals(box.getData(), data);
        assertArrayEquals(box.getNonce(), nonce);
    }

    @Test
    public void testGetSize() {
        final KeyStore.Box box1 = new KeyStore.Box(nonce, data);
        final KeyStore.Box box2 = new KeyStore.Box(nonce, new byte[] { 0x23, 0x42 });
        assertEquals(box1.getSize(), nonce.length + data.length);
        assertEquals(box2.getSize(), nonce.length + 2);

    }
}

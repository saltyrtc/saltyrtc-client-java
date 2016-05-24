/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests;

import com.neilalexander.jnacl.NaCl;

import org.junit.Before;
import org.junit.Test;
import org.saltyrtc.client.KeyStore;
import org.saltyrtc.client.exceptions.InvalidKeyException;

import static org.junit.Assert.*;

public class KeyStoreTest {

    private KeyStore ks;

    @Before
    public void setUp() throws Exception {
        this.ks = new KeyStore();
    }

    @Test
    public void testPublicKeyHex() {
        final String pk = this.ks.getPublicKeyHex();
        assertEquals(pk.length(), 64);
    }

    @Test(expected=InvalidKeyException.class)
    public void testSetOtherKeyInvalid() throws InvalidKeyException {
        this.ks.setOtherKey(new byte[] { 0x00, 0x01 });
    }

    @Test
    public void testSetOtherKey() throws InvalidKeyException {
        byte[] bytes = new byte[NaCl.PUBLICKEYBYTES];
        for (int i = 0; i < NaCl.PUBLICKEYBYTES; i++) {
            bytes[i] = (byte)i;
        };
        this.ks.setOtherKey(bytes);
    }

}

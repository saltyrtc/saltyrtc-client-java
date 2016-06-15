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
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.exceptions.InvalidKeyException;

import java.security.SecureRandom;

import static org.junit.Assert.*;

public class KeyStoreTest {

    private KeyStore ks;
    private SecureRandom random = new SecureRandom();

    @Before
    public void setUp() throws Exception {
        this.ks = new KeyStore();
    }

    @Test
    public void testPublicKeyHex() {
        final String pk = this.ks.getPublicKeyHex();
        assertEquals(pk.length(), 64);
    }

    @Test
    public void testEncrypt() throws CryptoFailedException, InvalidKeyException {
        final byte[] in = "hello".getBytes();
        final byte[] nonce = new byte[NaCl.NONCEBYTES];
        final byte[] otherKey = new byte[NaCl.PUBLICKEYBYTES];
        this.random.nextBytes(nonce);
        this.random.nextBytes(otherKey);
        final Box box = this.ks.encrypt(in, nonce, otherKey);
        assertEquals(nonce, box.getNonce());
        assertNotEquals(in, box.getData());
    }

}

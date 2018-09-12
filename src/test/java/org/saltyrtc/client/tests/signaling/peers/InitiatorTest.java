/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.signaling.peers;

import org.junit.Before;
import org.junit.Test;
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.exceptions.InvalidStateException;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.peers.Initiator;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;

import static org.junit.Assert.*;

public class InitiatorTest {
    private byte[] key;

    @Before
    public void setUp() {
        this.key = new byte[CryptoProvider.PUBLICKEYBYTES];
        NaCl.genkeypair(this.key, new byte[CryptoProvider.SECRETKEYBYTES]);
    }

    @Test
    public void testTmpLocalSessionKey() throws Exception {
        final Initiator initiator = new Initiator(this.key, new KeyStore());

        // Initially null
        try {
            initiator.extractTmpLocalSessionKey();
            fail("Exception not thrown");
        } catch (InvalidStateException e) { /* expected */ }

        // Set session key
        final KeyStore ks = new KeyStore();
        initiator.setTmpLocalSessionKey(ks);

        // Return session key and set to null
        assertEquals(ks, initiator.extractTmpLocalSessionKey());
        try {
            initiator.extractTmpLocalSessionKey();
            fail("Exception not thrown");
        } catch (InvalidStateException e) { /* expected */ }
    }

    @Test
    public void testTmpLocalSessionKeySetTwice() throws Exception {
        final Initiator initiator = new Initiator(this.key, new KeyStore());
        // Setting session key twice results in exception
        initiator.setTmpLocalSessionKey(new KeyStore());
        boolean caught = false;
        try {
            initiator.setTmpLocalSessionKey(new KeyStore());
        } catch (InvalidStateException e) {
            caught = true;
        }
        assertTrue(caught);
    }

}

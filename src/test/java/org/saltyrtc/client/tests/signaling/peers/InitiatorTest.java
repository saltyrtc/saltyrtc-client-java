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
import org.saltyrtc.client.crypto.CryptoException;
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.tests.LazysodiumCryptoProvider;
import org.saltyrtc.client.exceptions.InvalidStateException;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.peers.Initiator;

import static org.junit.Assert.*;

public class InitiatorTest {
    private byte[] key;
    private CryptoProvider cryptoProvider;

    @Before
    public void setUp() throws CryptoException {
        this.key = new byte[CryptoProvider.PUBLICKEYBYTES];
        this.cryptoProvider = new LazysodiumCryptoProvider();
        this.cryptoProvider.generateKeypair(this.key, new byte[CryptoProvider.PRIVATEKEYBYTES]);
    }

    @Test
    public void testTmpLocalSessionKey() throws Exception {
        final Initiator initiator = new Initiator(this.key, new KeyStore(this.cryptoProvider));

        // Initially null
        try {
            initiator.extractTmpLocalSessionKey();
            fail("Exception not thrown");
        } catch (InvalidStateException e) { /* expected */ }

        // Set session key
        final KeyStore ks = new KeyStore(this.cryptoProvider);
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
        final Initiator initiator = new Initiator(this.key, new KeyStore(this.cryptoProvider));
        // Setting session key twice results in exception
        initiator.setTmpLocalSessionKey(new KeyStore(this.cryptoProvider));
        boolean caught = false;
        try {
            initiator.setTmpLocalSessionKey(new KeyStore(this.cryptoProvider));
        } catch (InvalidStateException e) {
            caught = true;
        }
        assertTrue(caught);
    }

}

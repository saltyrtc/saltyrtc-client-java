/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.keystore;

import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.slf4j.Logger;

import com.neilalexander.jnacl.NaCl;

/**
 * Handles encrypting and decrypting messages for the peers.
 */
public class KeyStore {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(KeyStore.class);

    // Keys
    private final byte[] secretKey = new byte[NaCl.SECRETKEYBYTES];
    private final byte[] publicKey = new byte[NaCl.PUBLICKEYBYTES];

    /**
     * Create a new key store.
     */
    public KeyStore() {
        LOG.debug("Generating new key pair");
        NaCl.genkeypair(this.publicKey, this.secretKey);
        LOG.debug("Private key: " + NaCl.asHex(this.secretKey));
        LOG.debug("Public key: " + NaCl.asHex(this.publicKey));
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    /**
     * Return public keys as hex string.
     */
    public String getPublicKeyHex() {
        return NaCl.asHex(this.publicKey);
    }

    /**
     * Encrypt data for the peer. Return Box.
     *
     * @param data Bytes to be encrypted.
     * @param nonce The nonce that should be used to encrypt.
     * @param otherKey The public key of the peer.
     * @return The encrypted NaCl box.
     * @throws InvalidKeyException One of the keys was invalid.
     * @throws CryptoFailedException Encryption failed.
     */
    public Box encrypt(byte[] data, byte[] nonce, byte[] otherKey) throws CryptoFailedException, InvalidKeyException {
        // Create NaCl instance
        final NaCl nacl;
        try {
            nacl = new NaCl(this.secretKey, otherKey);
        } catch (Error e) {
            throw new InvalidKeyException(e.toString());
        }

        // Encrypt
        final byte[] encrypted;
        try {
            encrypted = nacl.encrypt(data, nonce);
        } catch (Error e) {
            throw new CryptoFailedException(e.toString());
        }
        if (encrypted == null) {
            throw new CryptoFailedException("Encrypted data is null");
        }

        // Return box
        return new Box(nonce, encrypted);
    }

    /**
     * Decrypt data from the peer. Return contained bytes.
     *
     * @param box NaCl box.
     * @param otherKey The public key of the peer.
     * @return The decrypted data.
     * @throws CryptoFailedException Decryption failed.
     */
    public byte[] decrypt(Box box, byte[] otherKey) throws CryptoFailedException, InvalidKeyException {
        // Create NaCl instance
        final NaCl nacl;
        try {
            nacl = new NaCl(this.secretKey, otherKey);
        } catch (Error e) {
            throw new InvalidKeyException(e.toString());
        }

        final byte[] decrypted;
        try {
            decrypted = nacl.decrypt(box.getData(), box.getNonce());
        } catch (Error e) {
            throw new CryptoFailedException(e.toString());
        }
        if (decrypted == null) {
            throw new CryptoFailedException("Decrypted data is null");
        }

        return decrypted;
    }
}

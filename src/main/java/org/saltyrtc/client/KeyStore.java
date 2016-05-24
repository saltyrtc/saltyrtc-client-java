/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.OtherKeyMissingException;
import org.slf4j.Logger;

import com.neilalexander.jnacl.NaCl;

import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * Handles encrypting and decrypting messages for the peers.
 */
public class KeyStore {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(KeyStore.class);

    // Keys
    private final byte[] privateKey = new byte[NaCl.SECRETKEYBYTES];
    private final byte[] publicKey = new byte[NaCl.PUBLICKEYBYTES];
    private byte[] otherKey = null;

    // The NaCl instance
    private NaCl nacl = null;

    // A secure random number generator
    private static final SecureRandom random = new SecureRandom();

    /**
     * A NaCl box.
     */
    public static class Box {
        private final byte[] nonce;
        private final byte[] data;

        /**
         * Create a new box from nonce and data.
         */
        public Box(byte[] nonce, byte[] data) {
            this.nonce = nonce;
            this.data = data;
        }

        /**
         * Create a new box from a ByteBuffer.
         * @param buffer The ByteBuffer containing nonce and data.
         */
        public Box(ByteBuffer buffer) {
            // Unpack nonce
            this.nonce = new byte[NaCl.NONCEBYTES];
            buffer.get(nonce, 0, NaCl.NONCEBYTES);

            // Unpack data
            this.data = new byte[buffer.remaining()];
            buffer.get(data);
        }

        public byte[] getNonce() {
            return nonce;
        }

        public byte[] getData() {
            return data;
        }

        /**
         * Return the size (in bytes) of the box.
         */
        public int getSize() {
            return this.nonce.length + this.data.length;
        }

        /**
         * Return a ByteBuffer containing nonce and data.
         */
        public ByteBuffer toBuffer() {
            // Pack data
            // Note: 'allocateDirect' does NOT work, DO NOT CHANGE!
            ByteBuffer box = ByteBuffer.allocate(this.getSize());
            box.put(this.nonce);
            box.put(this.data);

            // Flip offset and remaining length for reading
            box.flip();

            // Return box as byte buffer
            return box;
        }
    }

    /**
     * Create a new key store.
     */
    public KeyStore() {
        LOG.debug("Generating new key pair");
        NaCl.genkeypair(this.publicKey, this.privateKey);
        LOG.debug("Private key: " + NaCl.asHex(this.privateKey));
        LOG.debug("Public key: " + NaCl.asHex(this.publicKey));
    }

    /**
     * Return public keys as hex string.
     */
    public String getPublicKeyHex() {
        return NaCl.asHex(this.publicKey);
    }

    /**
     * Set the key of the recipient.
     *
     * @param otherKey Public key of the recipient.
     * @throws InvalidKeyException Thrown if the key is invalid.
     */
    public synchronized void setOtherKey(byte[] otherKey) throws InvalidKeyException {
        // Create getNaCl for encryption and decryption
        try {
            nacl = new NaCl(this.privateKey, otherKey);
            this.otherKey = otherKey;
        } catch (Error e) {
            throw new InvalidKeyException(e.toString());
        }
    }

    /**
     * Return the `NaCl` instance or throw `OtherKeyMissingException`.
     * @return The `NaCl` instance.
     * @throws OtherKeyMissingException Thrown if `otherKey` is not set.
     */
    private synchronized NaCl getNaCl() throws OtherKeyMissingException {
        if (nacl == null) {
            throw new OtherKeyMissingException();
        }
        return nacl;
    }

    /**
     * Encrypt the specified data. Return Box.
     *
     * @param data Bytes to be encrypted.
     * @return The encrypted NaCl box.
     * @throws OtherKeyMissingException No `otherKey` available.
     * @throws CryptoFailedException Encryption failed.
     */
    public Box encrypt(byte[] data) throws OtherKeyMissingException, CryptoFailedException {
        // Generate random nonce
        byte[] nonce = new byte[NaCl.NONCEBYTES];
        random.nextBytes(nonce);

        // Encrypt data with keys and nonce
        try {
            data = getNaCl().encrypt(data, nonce);
        } catch (Error e) {
            throw new CryptoFailedException(e.toString());
        }
        if (data == null) {
            throw new CryptoFailedException("Encrypted data is null");
        }

        // Return box
        return new Box(nonce, data);
    }

    /**
     * Decrypt the specified Box. Return contained bytes.
     *
     * @param box NaCl box.
     * @return The decrypted data.
     * @throws OtherKeyMissingException No `otherKey` available.
     * @throws CryptoFailedException Decryption failed.
     */
    public byte[] decrypt(Box box) throws OtherKeyMissingException, CryptoFailedException {
        byte[] data;
        try {
            data = getNaCl().decrypt(box.data, box.nonce);
        } catch (Error e) {
            throw new CryptoFailedException(e.toString());
        }
        if (data == null) {
            throw new CryptoFailedException("Decrypted data is null");
        }
        return data;
    }
}

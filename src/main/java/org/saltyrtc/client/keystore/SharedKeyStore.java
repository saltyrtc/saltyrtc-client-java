/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.keystore;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;
import org.slf4j.Logger;

/**
 * A `SharedKeyStore` holds the resulting precalculated shared key of the local peer's secret
 * key and the remote peer's public key.
 *
 * Note: Since the shared key is only calculated once, using the `SharedKeyStore`
 *       should always be preferred over over using the `KeyStore` instance.
 */
public class SharedKeyStore {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.SharedKeyStore");

    // Keys
    @NonNull private final byte[] localPublicKey;
    @NonNull private final byte[] remotePublicKey;

    // Precomputed NaCl instance
    private NaCl nacl;

    /**
     * Create a new key store from an existing private key.
     * The public key will automatically be derived.
     */
    public SharedKeyStore(@NonNull byte[] localPrivateKey, @NonNull byte[] remotePublicKey) throws InvalidKeyException {
        this.localPublicKey = NaCl.derivePublicKey(localPrivateKey); // TODO: Maybe we should pass in the local pubkey
        this.remotePublicKey = remotePublicKey;

        LOG.debug("Precalculating shared key");
        try {
            this.nacl = new NaCl(localPrivateKey, remotePublicKey);
        } catch (Error e) {
            throw new InvalidKeyException(e.toString());
        }
    }

    /**
     * Encrypt data for the peer. Return Box.
     *
     * @param data Bytes to be encrypted.
     * @param nonce The nonce that should be used to encrypt.
     * @return The encrypted NaCl box.
     * @throws CryptoFailedException Encryption failed.
     */
    public Box encrypt(@NonNull byte[] data, @NonNull byte[] nonce) throws CryptoFailedException {
        final byte[] encrypted;
        try {
            encrypted = this.nacl.encrypt(data, nonce);
        } catch (Error e) {
            throw new CryptoFailedException(e.toString());
        }
        if (encrypted == null) {
            throw new CryptoFailedException("Encrypted data is null");
        }
        return new Box(nonce, encrypted);
    }

    /**
     * Decrypt data from the peer. Return contained bytes.
     *
     * @param box NaCl box.
     * @return The decrypted data.
     * @throws CryptoFailedException Decryption failed.
     */
    public byte[] decrypt(@NonNull Box box) throws CryptoFailedException {
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

    @NonNull
    public byte[] getRemotePublicKey() {
        return this.remotePublicKey;
    }

    @NonNull
    public byte[] getLocalPublicKey() {
        return this.localPublicKey;
    }
}

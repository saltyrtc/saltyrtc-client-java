/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.keystore;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.crypto.CryptoException;
import org.saltyrtc.client.crypto.CryptoInstance;
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.exceptions.InvalidKeyException;
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

    // Crypto
    @NonNull private final CryptoInstance cryptoInstance;

    /**
     * Create a new key store from an existing private key.
     * The public key will automatically be derived.
     */
    public SharedKeyStore(
        @NonNull CryptoProvider cryptoProvider,
        @NonNull byte[] localPrivateKey,
        @NonNull byte[] remotePublicKey
    ) throws InvalidKeyException {
        try {
            this.localPublicKey = cryptoProvider.derivePublicKey(localPrivateKey); // TODO: Maybe we should pass in the local pubkey
            this.remotePublicKey = remotePublicKey;
            LOG.debug("Precalculating shared key");
            this.cryptoInstance = cryptoProvider.getInstance(localPrivateKey, remotePublicKey);
        } catch (CryptoException e) {
            throw new InvalidKeyException(e.toString());
        }
    }

    /**
     * Encrypt data for the peer. Return Box.
     *
     * @param data Bytes to be encrypted.
     * @param nonce The nonce that should be used to encrypt.
     * @return The encrypted NaCl box.
     * @throws CryptoException Encryption failed.
     */
    public Box encrypt(@NonNull byte[] data, @NonNull byte[] nonce) throws CryptoException {
        final byte[] encrypted = this.cryptoInstance.encrypt(data, nonce);
        return new Box(nonce, encrypted);
    }

    /**
     * Decrypt data from the peer. Return contained bytes.
     *
     * @param box NaCl box.
     * @return The decrypted data.
     * @throws CryptoException Decryption failed.
     */
    public byte[] decrypt(@NonNull Box box) throws CryptoException {
        return this.cryptoInstance.decrypt(box.getData(), box.getNonce());
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

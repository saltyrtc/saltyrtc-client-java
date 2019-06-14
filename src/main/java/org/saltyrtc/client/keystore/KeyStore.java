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
import org.saltyrtc.client.helpers.HexHelper;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Handle encrypting and decrypting messages for the peers.
 *
 * This class uses NaCl asymmetric key encryption.
 */
public class KeyStore {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.KeyStore");

    // Crypto
    @NonNull private final CryptoProvider cryptoProvider;

    // Keys
    private byte[] privateKey = new byte[CryptoProvider.PRIVATEKEYBYTES];
    private byte[] publicKey = new byte[CryptoProvider.PUBLICKEYBYTES];

    /**
     * Create a new key store.
     */
    public KeyStore(@NonNull CryptoProvider cryptoProvider) {
        LOG.debug("Generating new key pair");
        try {
            cryptoProvider.generateKeypair(this.publicKey, this.privateKey);
        } catch (CryptoException e) {
            // Should never happen, since the exception is only thrown if the
            // buffers don't have the correct size.
            e.printStackTrace();
            throw new RuntimeException("Could not generate keypair", e);
        }
        LOG.debug("Public key: " + HexHelper.asHex(this.publicKey));
        this.cryptoProvider = cryptoProvider;
    }

    /**
     * Create a new key store from an existing private key.
     * The public key will automatically be derived.
     *
     * @throws CryptoException when the private key is invalid
     */
    public KeyStore(@NonNull CryptoProvider cryptoProvider, @NonNull byte[] privateKey) throws CryptoException {
        LOG.debug("Deriving public key from private key");
        this.privateKey = Objects.requireNonNull(privateKey);
        this.publicKey = cryptoProvider.derivePublicKey(privateKey);
        LOG.debug("Public key: " + HexHelper.asHex(this.publicKey));
        this.cryptoProvider = cryptoProvider;
    }

    /**
     * Create a new key store from an existing private key.
     * The public key will automatically be derived.
     *
     * @throws CryptoException when the private key is invalid
     */
    public KeyStore(@NonNull CryptoProvider cryptoProvider, @NonNull String privateKeyHex) throws CryptoException {
        this(cryptoProvider, HexHelper.hexStringToByteArray(privateKeyHex));
    }

    /**
     * Create a new key store from an existing keypair.
     */
    public KeyStore(@NonNull final CryptoProvider cryptoProvider, @NonNull byte[] publicKey, @NonNull byte[] privateKey) {
        LOG.debug("Using existing keypair");
        this.privateKey = Objects.requireNonNull(privateKey);
        this.publicKey = Objects.requireNonNull(publicKey);
        LOG.debug("Public key: " + HexHelper.asHex(this.publicKey));
        this.cryptoProvider = Objects.requireNonNull(cryptoProvider);
    }

    /**
     * Create a new key store from an existing keypair.
     */
    public KeyStore(final CryptoProvider cryptoProvider, String publicKeyHex, String privateKeyHex) {
        this(cryptoProvider,
             HexHelper.hexStringToByteArray(publicKeyHex),
             HexHelper.hexStringToByteArray(privateKeyHex));
    }

    /**
     * Create a SharedKeyStore from this instance and the public key of the remote peer.
     *
     * @param publicKey The public key of the remote peer.
     * @throws InvalidKeyException Thrown if the `publicKey` bytes are not a valid public key
     */
    public SharedKeyStore getSharedKeyStore(@NonNull byte[] publicKey) throws InvalidKeyException {
        return new SharedKeyStore(Objects.requireNonNull(this.cryptoProvider),
            Objects.requireNonNull(this.privateKey),
            Objects.requireNonNull(publicKey));
    }

    public byte[] getPublicKey() {
        return this.publicKey;
    }

    public byte[] getPrivateKey() {
        return this.privateKey;
    }

    public String getPublicKeyHex() {
        return HexHelper.asHex(this.getPublicKey());
    }

    public String getPrivateKeyHex() {
        return HexHelper.asHex(this.getPrivateKey());
    }

    /**
     * Encrypt data for the peer. Return Box.
     *
     * @param data Bytes to be encrypted.
     * @param nonce The nonce that should be used to encrypt.
     * @param otherKey The public key of the peer.
     * @return The encrypted NaCl box.
     * @throws InvalidKeyException One of the keys was invalid.
     * @throws CryptoException Encryption failed.
     */
    public Box encrypt(@NonNull byte[] data, @NonNull byte[] nonce, @NonNull byte[] otherKey) throws CryptoException, InvalidKeyException {
        // Create CryptoInstance
        final CryptoInstance cryptoInstance;
        try {
            cryptoInstance = this.cryptoProvider.getInstance(this.privateKey, otherKey);
        } catch (CryptoException e) {
            throw new InvalidKeyException(e.toString());
        }

        // Encrypt
        final byte[] encrypted = cryptoInstance.encrypt(data, nonce);

        // Return box
        return new Box(nonce, encrypted);
    }

    /**
     * Decrypt data from the peer. Return contained bytes.
     *
     * @param box NaCl box.
     * @param otherKey The public key of the peer.
     * @return The decrypted data.
     * @throws CryptoException Decryption failed.
     */
    public byte[] decrypt(@NonNull Box box, @NonNull byte[] otherKey) throws CryptoException, InvalidKeyException {
        final CryptoInstance cryptoInstance;
        try {
            cryptoInstance = this.cryptoProvider.getInstance(this.privateKey, otherKey);
        } catch (CryptoException e) {
            throw new InvalidKeyException(e.toString());
        }
        return cryptoInstance.decrypt(box.getData(), box.getNonce());
    }
}

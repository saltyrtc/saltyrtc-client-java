/*
 * Copyright (c) 2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */
package org.saltyrtc.client.crypto;

import org.saltyrtc.client.annotations.NonNull;

/**
 * An abstraction over NaCl.
 */
public interface CryptoProvider {

    // These will be the same for all implementations, since they're specified by NaCl.
    int PUBLICKEYBYTES = 32;
    int PRIVATEKEYBYTES = 32;
    int SYMMKEYBYTES = 32;
    int NONCEBYTES = 24;
    int BOXOVERHEAD = 16;

    /**
     * Create a public/private key pair.
     *
     * @param publickey A byte array with size PUBLICKEYBYTES
     * @param privatekey A byte array with size PRIVATEKEYBYTES
     */
    void genkeypair(@NonNull byte[] publickey, @NonNull byte[] privatekey);

    /**
     * Derive the public key from a private key.
     *
     * @param privateKey A private key (length PRIVATEKEYBYTES)
     * @return The public key
     * @throws CryptoException when the private key is invalid
     */
    @NonNull
    byte[] derivePublicKey(@NonNull byte[] privateKey) throws CryptoException;

    /**
     * Encrypt data using secret key encryption.
     * Must never return null. If encryption fails, throw CryptoException.
     */
    @NonNull
    byte[] symmetricEncrypt(@NonNull byte[] data, @NonNull byte[] key, @NonNull byte[] nonce) throws CryptoException;

    /**
     * Decrypt data using secret key encryption.
     * Must never return null. If decryption fails, throw CryptoException.
     */
    @NonNull
    byte[] symmetricDecrypt(@NonNull byte[] data, @NonNull byte[] key, @NonNull byte[] nonce) throws CryptoException;

    /**
     * Create a `CryptoInstance` that can encrypt and decrypt data.
     *
     * @param ownPrivateKey The private key
     * @param otherPublicKey The public key
     * @return A `CryptoInstance`
     * @throws CryptoException if one of the keys is invalid
     */
    @NonNull
    CryptoInstance getInstance(@NonNull byte[] ownPrivateKey, @NonNull byte[] otherPublicKey) throws CryptoException;
}

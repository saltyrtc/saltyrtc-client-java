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
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.slf4j.Logger;

import java.security.SecureRandom;

/**
 * Encrypt and decrypt using authentication tokens.
 *
 * This class uses NaCl symmetric key encryption.
 */
public class AuthToken {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.AuthToken");

    // Crypto
    @NonNull
    private final CryptoProvider cryptoProvider;

    // Keys
    private byte[] authToken = new byte[CryptoProvider.SYMMKEYBYTES];

    public AuthToken(@NonNull CryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
        final SecureRandom random = new SecureRandom();
        random.nextBytes(this.authToken);
        LOG.debug("Generated random auth token");
    }

    public AuthToken(@NonNull CryptoProvider cryptoProvider, byte[] authToken) throws InvalidKeyException {
        if (authToken.length != CryptoProvider.SYMMKEYBYTES) {
            throw new InvalidKeyException("Auth token must be " + CryptoProvider.SYMMKEYBYTES + " bytes long.");
        }
        this.cryptoProvider = cryptoProvider;
        this.authToken = authToken;
        LOG.debug("Initialized auth token");
    }

    public byte[] getAuthToken() {
        return this.authToken;
    }

    /**
     * Encrypt data using the auth token.
     *
     * @param data Bytes to be encrypted.
     * @param nonce The nonce that should be used to encrypt.
     * @return The encrypted NaCl box.
     * @throws CryptoException Encryption failed.
     */
    public Box encrypt(byte[] data, byte[] nonce) throws CryptoException {
        final byte[] encrypted = this.cryptoProvider.symmetricEncryptData(data, this.authToken, nonce);
        return new Box(nonce, encrypted);
    }

    /**
     * Decrypt data using the auth token.
     *
     * @param box NaCl box.
     * @return The decrypted data.
     * @throws CryptoException Decryption failed.
     */
    public byte[] decrypt(Box box) throws CryptoException {
        return this.cryptoProvider.symmetricDecryptData(box.getData(), this.authToken, box.getNonce());
    }

}

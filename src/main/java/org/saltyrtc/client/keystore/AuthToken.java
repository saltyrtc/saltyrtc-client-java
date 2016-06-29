/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.keystore;

import com.neilalexander.jnacl.NaCl;

import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.slf4j.Logger;

import java.security.InvalidKeyException;
import java.security.SecureRandom;

/**
 * Encrypt and decrypt using authentication tokens.
 *
 * This class uses NaCl symmetric key encryption.
 */
public class AuthToken {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.AuthToken");

    // Keys
    private byte[] authToken = new byte[NaCl.SYMMKEYBYTES];

    public AuthToken() {
        final SecureRandom random = new SecureRandom();
        random.nextBytes(this.authToken);
        LOG.debug("Generated random auth token");
    }

    public AuthToken(byte[] authToken) throws InvalidKeyException {
        if (authToken.length != NaCl.SYMMKEYBYTES) {
            throw new InvalidKeyException("Auth token must be " + NaCl.SYMMKEYBYTES + " bytes long.");
        }
        this.authToken = authToken;
        LOG.debug("Initialized auth token");
    }

    public byte[] getAuthToken() {
        return authToken;
    }

    /**
     * Encrypt data using the auth token.
     *
     * @param data Bytes to be encrypted.
     * @param nonce The nonce that should be used to encrypt.
     * @return The encrypted NaCl box.
     * @throws CryptoFailedException Encryption failed.
     */
    public Box encrypt(byte[] data, byte[] nonce) throws CryptoFailedException {
        final byte[] encrypted = NaCl.symmetricEncryptData(data, this.authToken, nonce);
        if (encrypted == null) {
            throw new CryptoFailedException("Encrypted data is null");
        }
        return new Box(nonce, encrypted);
    }

    /**
     * Decrypt data using the auth token.
     *
     * @param box NaCl box.
     * @return The decrypted data.
     * @throws CryptoFailedException Decryption failed.
     */
    public byte[] decrypt(Box box) throws CryptoFailedException {
        final byte[] decrypted = NaCl.symmetricDecryptData(box.getData(), this.authToken, box.getNonce());
        if (decrypted == null) {
            throw new CryptoFailedException("Decrypted data is null");
        }
        return decrypted;
    }

}

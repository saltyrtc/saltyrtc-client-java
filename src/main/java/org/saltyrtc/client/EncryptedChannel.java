/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.exceptions.CryptoException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.OtherKeyMissingException;

import java.io.UnsupportedEncodingException;

/**
 * Handles binary packing and unpacking.
 * Simplifies the usage of KeyStore by handling all exceptions.
 */
public class EncryptedChannel {
    protected static final String NAME = "EncryptedChannel";

    protected KeyStore.Box encrypt(String message) throws CryptoException {
        // Encrypt data
        try {
            return KeyStore.encrypt(message);
        } catch (OtherKeyMissingException e) {
            this.throwCryptoException(
                    e, "key", "Cannot encrypt, public key of recipient is missing");
        } catch (UnsupportedEncodingException e) {
            this.throwCryptoException(
                    e, "encode", "Cannot encrypt, UTF-8 encoding not supported");
        } catch (CryptoFailedException e) {
            this.throwCryptoException(
                    e, "crypto", "Cannot encrypt, invalid data or keys don't match");
        }

        // Unreachable section
        return null;
    }

    protected final String decrypt(KeyStore.Box box) throws CryptoException {
        // Decrypt data
        try {
            return KeyStore.decrypt(box);
        } catch (OtherKeyMissingException e) {
            this.throwCryptoException(
                    e, "key", "Cannot decrypt, public key of recipient is missing");
        } catch (UnsupportedEncodingException e) {
            this.throwCryptoException(
                    e, "encode", "Cannot decrypt, UTF-8 encoding not supported");
        } catch (CryptoFailedException e) {
            this.throwCryptoException(
                    e, "crypto", "Cannot decrypt, invalid data or keys don't match");
        }

        // Unreachable section
        return null;
    }

    protected void throwCryptoException(
            Exception e,
            final String state,
            final String error
    ) throws CryptoException {
        e.printStackTrace();
        throw new CryptoException(state, error);
    }
}

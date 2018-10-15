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
 * This object can encrypt and decrypt data using the provided public and private keys.
 */
public interface CryptoInstance {
    /**
     * Encrypt data using asymmetric encryption.
     * Must never return null. If encryption fails, throw CryptoException.
     */
    @NonNull
    byte[] encrypt(@NonNull byte[] data, @NonNull byte[] nonce) throws CryptoException;

    /**
     * Decrypt data using asymmetric encryption.
     * Must never return null. If decryption fails, throw CryptoException.
     */
    @NonNull
    byte[] decrypt(@NonNull byte[] data, @NonNull byte[] nonce) throws CryptoException;
}

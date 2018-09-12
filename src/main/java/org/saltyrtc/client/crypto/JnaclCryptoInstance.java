/*
 * Copyright (c) 2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */
package org.saltyrtc.client.crypto;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;

/**
 * An implementation of the CryptoInstance interface for jnacl.
 */
public class JnaclCryptoInstance implements CryptoInstance {
    @NonNull private final NaCl nacl;

    public JnaclCryptoInstance(@NonNull byte[] ownPrivateKey, @NonNull byte[] otherPublicKey) throws CryptoException {
        try {
            this.nacl = new NaCl(ownPrivateKey, otherPublicKey);
        } catch (Error e) {
            throw new CryptoException("Could not create NaCl instance: " + e.toString(), e);
        }
    }

    @NonNull
    @Override
    public byte[] encrypt(@NonNull byte[] data, @NonNull byte[] nonce) throws CryptoException {
        try {
            return this.nacl.encrypt(data, nonce);
        } catch (Error e) {
            throw new CryptoException("Could not encrypt data: " + e.toString(), e);
        }
    }

    @NonNull
    @Override
    public byte[] decrypt(@NonNull byte[] data, @NonNull byte[] nonce) throws CryptoException {
        final byte[] decrypted;
        try {
            decrypted = this.nacl.decrypt(data, nonce);
        } catch (Error e) {
            throw new CryptoException("Could not decrypt data: " + e.toString(), e);
        }
        if (decrypted == null) {
            throw new CryptoException("Could not decrypt data (data is null)");
        }
        return decrypted;
    }
}

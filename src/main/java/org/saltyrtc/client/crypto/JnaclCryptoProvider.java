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
 * An implementation of the CryptoProvider interface for jnacl.
 */
public class JnaclCryptoProvider implements CryptoProvider {

    @Override
    public void genkeypair(@NonNull byte[] publickey, @NonNull byte[] privatekey) {
        NaCl.genkeypair(publickey, privatekey);
    }

    @NonNull
    @Override
    public byte[] derivePublicKey(@NonNull byte[] privateKey) throws CryptoException {
        try {
            return NaCl.derivePublicKey(privateKey);
        } catch (Error e) {
            throw new CryptoException("Deriving public key from private key failed: " + e.toString(), e);
        }
    }

    @NonNull
    @Override
    public byte[] symmetricEncrypt(@NonNull byte[] input, @NonNull byte[] key, @NonNull byte[] nonce) throws CryptoException {
        try {
            return NaCl.symmetricEncryptData(input, key, nonce);
        } catch (Error e) {
            throw new CryptoException("Could not encrypt data: " + e.toString(), e);
        }
    }

    @NonNull
    @Override
    public byte[] symmetricDecrypt(@NonNull byte[] input, @NonNull byte[] key, @NonNull byte[] nonce) throws CryptoException {
        final byte[] decrypted;
        try {
            decrypted = NaCl.symmetricDecryptData(input, key, nonce);
        } catch (Error e) {
            throw new CryptoException("Could not decrypt data: " + e.toString(), e);
        }
        if (decrypted == null) {
            throw new CryptoException("Could not decrypt data (data is null)");
        }
        return decrypted;
    }

    @NonNull
    @Override
    public CryptoInstance getInstance(@NonNull byte[] ownPrivateKey, @NonNull byte[] otherPublicKey) throws CryptoException {
        return new JnaclCryptoInstance(ownPrivateKey, otherPublicKey);
    }

}

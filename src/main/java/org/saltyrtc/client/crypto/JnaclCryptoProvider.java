package org.saltyrtc.client.crypto;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;

public class JnaclCryptoProvider implements CryptoProvider {

    @NonNull
    @Override
    public byte[] symmetricEncryptData(byte[] input, byte[] key, byte[] nonce) throws CryptoException {
        try {
            return NaCl.symmetricEncryptData(input, key, nonce);
        } catch (Error e) {
            throw new CryptoException("Could not encrypt data: " + e.toString(), e);
        }
    }

    @NonNull
    @Override
    public byte[] symmetricDecryptData(byte[] input, byte[] key, byte[] nonce) throws CryptoException {
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

}

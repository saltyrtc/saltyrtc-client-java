package org.saltyrtc.client.crypto;

import org.saltyrtc.client.annotations.NonNull;

/**
 * An abstraction over NaCl.
 */
public interface CryptoProvider {

    // These will be the same for all implementations, since they're specified by NaCl.
    int PUBLICKEYBYTES = 32;
    int SECRETKEYBYTES = 32;
    int SYMMKEYBYTES = 32;
    int NONCEBYTES = 24;
    int BOXOVERHEAD = 16;

    /**
     * Encrypt data using secret key encryption.
     * Must never return null. If encryption fails, throw CryptoException.
     */
    @NonNull
    byte[] symmetricEncryptData(byte[] input, byte[] key, byte[] nonce) throws CryptoException;

    /**
     * Decrypt data using secret key encryption.
     * Must never return null. If decryption fails, throw CryptoException.
     */
    @NonNull
    byte[] symmetricDecryptData(byte[] input, byte[] key, byte[] nonce) throws CryptoException;

}

package org.saltyrtc.client.crypto;

/**
 * An abstraction over NaCl.
 */
public interface CryptoProvider {

    /**
     * Return the number of bytes in a NaCl public key.
     * Most probably this will return the value of a constant from the NaCl implementation.
     */
    int publicKeyBytes();

    /**
     * Return the number of bytes in a NaCl secret key.
     * Most probably this will return the value of a constant from the NaCl implementation.
     */
    int secretKeyBytes();

    /**
     * Return the number of bytes in a NaCl nonce.
     * Most probably this will return the value of a constant from the NaCl implementation.
     */
    int nonceBytes();


}

package org.saltyrtc.client.crypto;

import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;

public class JnaclCryptoProvider implements CryptoProvider {

    @Override
    public int publicKeyBytes() {
        return NaCl.PUBLICKEYBYTES;
    }

    @Override
    public int secretKeyBytes() {
        return NaCl.SECRETKEYBYTES;
    }

    @Override
    public int nonceBytes() {
        return NaCl.NONCEBYTES;
    }

}

/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.exceptions.InvalidBuilderStateException;
import org.saltyrtc.client.keystore.KeyStore;

import java.security.InvalidKeyException;

import javax.net.ssl.SSLContext;

/**
 * Builder class to construct a SaltyRTC instance.
 */
public class SaltyRTCBuilder {

    private boolean hasKeyStore = false;
    private boolean hasConnectionInfo = false;
    private boolean hasInitiatorInfo = false;

    private KeyStore keyStore;
    private String host;
    private Integer port;
    private SSLContext sslContext;

    private byte[] initiatorPublicKey;
    private byte[] authToken;

    /**
     * Validate the specified host, throw an IllegalArgumentException if it's invalid.
     */
    private void validateHost(String host) {
        if (host.endsWith("/")) {
            throw new IllegalArgumentException("SaltyRTC host may not end with a slash");
        }
        if (host.contains("//")) {
            throw new IllegalArgumentException("SaltyRTC host should not contain protocol");
        }
    }

    /**
     * Assert that a keystore has been set.
     */
    private void requireKeyStore() throws InvalidBuilderStateException {
        if (!this.hasKeyStore) {
            throw new InvalidBuilderStateException(
                    "Keys not set yet. Please call .withKeyStore method first.");
        }
    }

    /**
     * Assert that connection info has been set.
     */
    private void requireConnectionInfo() throws InvalidBuilderStateException {
        if (!this.hasConnectionInfo) {
            throw new InvalidBuilderStateException(
                    "Connection info not set yet. Please call .connectTo method first.");
        }
    }

    /**
     * Assert that initiator info has been set.
     */
    private void requireInitiatorInfo() throws InvalidBuilderStateException {
        if (!this.hasInitiatorInfo) {
            throw new InvalidBuilderStateException(
                    "Initiator info not set yet. Please call .initiatorInfo method first.");
        }
    }

    /**
     * Set SaltyRTC signalling server connection info.
     *
     * @param host The SaltyRTC server host.
     * @param port The SaltyRTC server port.
     * @param sslContext The SSL context used to create the encrypted WebSocket connection.
     * @throws IllegalArgumentException Thrown if the host string is invalid.
     */
    public SaltyRTCBuilder connectTo(String host, int port, SSLContext sslContext) {
        validateHost(host);
        this.host = host;
        this.port = port;
        this.sslContext = sslContext;
        this.hasConnectionInfo = true;
        return this;
    }

    /**
     * Set the key store. This can be either a new `KeyStore` instance, or a saved one if you
     * intend to use trusted keys.
     *
     * @param keyStore The KeyStore instance containing the public and private permanent key to
     * use.
     */
    public SaltyRTCBuilder withKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
        this.hasKeyStore = true;
        return this;
    }

    /**
     * Set initiator connection info transferred via a secure data channel.
     * @param initiatorPublicKey The public key of the initiator.
     * @param authToken The secret auth token.
     */
    public SaltyRTCBuilder initiatorInfo(byte[] initiatorPublicKey, byte[] authToken) {
        this.initiatorPublicKey = initiatorPublicKey;
        this.authToken = authToken;
        this.hasInitiatorInfo = true;
        return this;
    }

    /**
     * Return a SaltyRTC instance configured as initiator.
     *
     * @throws InvalidBuilderStateException Thrown if key or connection info haven't been set yet.
     */
    public SaltyRTC asInitiator() throws InvalidBuilderStateException {
        this.requireKeyStore();
        this.requireConnectionInfo();
        return new SaltyRTC(this.keyStore, this.host, this.port, this.sslContext);
    }

    /**
     * Return a SaltyRTC instance configured as responder.
     *
     * @throws InvalidBuilderStateException Thrown if key or connection info or initiator info
     *     haven't been set yet.
     * @throws InvalidKeyException Thrown if public key or auth token are invalid.
     */
    public SaltyRTC asResponder() throws InvalidBuilderStateException, InvalidKeyException {
        this.requireKeyStore();
        this.requireConnectionInfo();
        this.requireInitiatorInfo();
        return new SaltyRTC(this.keyStore, this.host, this.port, this.sslContext,
                this.initiatorPublicKey, this.authToken);
    }
}

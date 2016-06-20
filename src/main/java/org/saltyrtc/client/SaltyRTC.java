/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.InitiatorSignaling;
import org.saltyrtc.client.signaling.ResponderSignaling;
import org.saltyrtc.client.signaling.Signaling;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;

/**
 * The main class used to create a P2P connection through a SaltyRTC signaling server.
 */
public class SaltyRTC {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SaltyRTC.class);

    protected Signaling signaling;

    /**
     * Create a SaltyRTC instance as initiator.
     *
     * @param permanentKey A KeyStore instance containing the permanent key.
     * @param host The SaltyRTC server host.
     * @param port The SaltyRTC server port.
     */
    public SaltyRTC(KeyStore permanentKey, String host, int port) {
        validateHost(host);
        this.signaling = new InitiatorSignaling(this, host, port, permanentKey);
    }

    /**
     * Create a SaltyRTC instance as responder.
     *
     * @param permanentKey A KeyStore instance containing the permanent key.
     * @param host The SaltyRTC server host.
     * @param port The SaltyRTC server port.
     */
    public SaltyRTC(KeyStore permanentKey, String host, int port,
                    byte[] initiatorPublicKey, byte[] authToken) {
        validateHost(host);
        this.signaling = new ResponderSignaling(
                this, host, port, permanentKey, initiatorPublicKey, authToken);
    }

    private void validateHost(String host) {
        if (host.endsWith("/")) {
            throw new IllegalArgumentException("SaltyRTC host may not end with a slash");
        }
        if (host.contains("//")) {
            throw new IllegalArgumentException("SaltyRTC host should not contain protocol");
        }
    }

    public byte[] getPublicPermanentKey() {
        return this.signaling.getPublicPermanentKey();
    }

    public byte[] getAuthToken() {
        return this.signaling.getAuthToken();
    }

    public SignalingState getSignalingState() {
        return this.signaling.state;
    }
}
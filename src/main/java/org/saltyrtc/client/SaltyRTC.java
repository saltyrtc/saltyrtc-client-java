/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.events.ConnectedEvent;
import org.saltyrtc.client.events.EventRegistry;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.InitiatorSignaling;
import org.saltyrtc.client.signaling.ResponderSignaling;
import org.saltyrtc.client.signaling.Signaling;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;

import java.util.concurrent.FutureTask;

import javax.net.ssl.SSLContext;

/**
 * The main class used to create a P2P connection through a SaltyRTC signaling server.
 */
public class SaltyRTC {

    /**
     * Collection of all possible events.
     */
    private class Events {
        public EventRegistry<ConnectedEvent> connected = new EventRegistry<>();
    }

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SaltyRTC.class);

    protected boolean debug = false;

    protected Signaling signaling;
    protected Events events = new Events();

    /**
     * Create a SaltyRTC instance as initiator.
     *
     * @param permanentKey A KeyStore instance containing the permanent key.
     * @param host The SaltyRTC server host.
     * @param port The SaltyRTC server port.
     */
    public SaltyRTC(KeyStore permanentKey, String host, int port, SSLContext sslContext) {
        validateHost(host);
        this.signaling = new InitiatorSignaling(this, host, port, permanentKey, sslContext);
    }

    /**
     * Create a SaltyRTC instance as responder.
     *
     * @param permanentKey A KeyStore instance containing the permanent key.
     * @param host The SaltyRTC server host.
     * @param port The SaltyRTC server port.
     */
    public SaltyRTC(KeyStore permanentKey, String host, int port, SSLContext sslContext,
                    byte[] initiatorPublicKey, byte[] authToken) {
        validateHost(host);
        this.signaling = new ResponderSignaling(
                this, host, port, permanentKey, sslContext, initiatorPublicKey, authToken);
    }

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

    public byte[] getPublicPermanentKey() {
        return this.signaling.getPublicPermanentKey();
    }

    public byte[] getAuthToken() {
        return this.signaling.getAuthToken();
    }

    /**
     * Return the current signaling state.
     */
    public SignalingState getSignalingState() {
        return this.signaling.state;
    }

    /**
     * Connect to the SaltyRTC server.
     */
    public FutureTask<Void> connect() {
        return this.signaling.connect();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public boolean getDebug() {
        return debug;
    }

}
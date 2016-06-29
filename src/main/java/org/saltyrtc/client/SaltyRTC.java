/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.events.ConnectedEvent;
import org.saltyrtc.client.events.ConnectionClosedEvent;
import org.saltyrtc.client.events.ConnectionErrorEvent;
import org.saltyrtc.client.events.EventRegistry;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.InitiatorSignaling;
import org.saltyrtc.client.signaling.ResponderSignaling;
import org.saltyrtc.client.signaling.Signaling;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;

import java.security.InvalidKeyException;

import javax.net.ssl.SSLContext;

/**
 * The main class used to create a P2P connection through a SaltyRTC signaling server.
 */
public class SaltyRTC {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC");

    // Whether to enable debug mode
    protected boolean debug = false;

    // Reference to signaling class
    protected Signaling signaling;

    // Event registry
    public final SaltyRTC.Events events = new SaltyRTC.Events();

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
     * @throws InvalidKeyException Either the public key or the auth token is invalid.
     */
    public SaltyRTC(KeyStore permanentKey, String host, int port, SSLContext sslContext,
                    byte[] initiatorPublicKey, byte[] authToken)
                    throws InvalidKeyException {
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
     * Connect asynchronously to the SaltyRTC server.
     *
     * To get notified when the connection is up and running,
     * subscribe to the `ConnectedEvent`.
     *
     * @throws ConnectionException if the connection process fails.
     */
    public void connect() throws ConnectionException {
        this.signaling.connect();
    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This operation is asynchronous, once the connection is closed, the
     * `ConnectionClosedEvent` will be emitted.
     */
    public void disconnect() {
        this.signaling.disconnect();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public boolean getDebug() {
        return debug;
    }

    /**
     * Collection of all possible events.
     */
    public static class Events {
        public EventRegistry<ConnectedEvent> connected = new EventRegistry<>();
        public EventRegistry<ConnectionErrorEvent> connectionError = new EventRegistry<>();
        public EventRegistry<ConnectionClosedEvent> connectionClosed = new EventRegistry<>();
    }

}

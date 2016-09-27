/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.events.EventRegistry;
import org.saltyrtc.client.events.SendErrorEvent;
import org.saltyrtc.client.events.SignalingChannelChangedEvent;
import org.saltyrtc.client.events.SignalingStateChangedEvent;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.InitiatorSignaling;
import org.saltyrtc.client.signaling.ResponderSignaling;
import org.saltyrtc.client.signaling.Signaling;
import org.saltyrtc.client.signaling.SignalingChannel;
import org.saltyrtc.client.signaling.SignalingRole;
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
    private boolean debug = false;

    // Reference to signaling class
    private Signaling signaling;

    // Event registry
    public final SaltyRTC.Events events = new SaltyRTC.Events();

    // Internal constructor used by SaltyRTCBuilder.
    // Initialize as initiator without trusted key.
    SaltyRTC(KeyStore permanentKey, String host, int port, SSLContext sslContext) {
        this.signaling = new InitiatorSignaling(
                this, host, port, permanentKey, sslContext);
    }

    // Internal constructor used by SaltyRTCBuilder.
    // Initialize as responder without trusted key.
    SaltyRTC(KeyStore permanentKey, String host, int port, SSLContext sslContext,
                       byte[] initiatorPublicKey, byte[] authToken) throws InvalidKeyException {
        this.signaling = new ResponderSignaling(
                this, host, port, permanentKey, sslContext, initiatorPublicKey, authToken);
    }

    // Internal constructor used by SaltyRTCBuilder.
    // Initialize as initiator or responder with trusted key.
    SaltyRTC(KeyStore permanentKey, String host, int port, SSLContext sslContext,
             byte[] peerTrustedKey, SignalingRole role) throws InvalidKeyException {
        switch (role) {
            case Initiator:
                this.signaling = new InitiatorSignaling(
                        this, host, port, permanentKey, sslContext, peerTrustedKey);
                break;
            case Responder:
                this.signaling = new ResponderSignaling(
                        this, host, port, permanentKey, sslContext, peerTrustedKey);
                break;
            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    public KeyStore getKeyStore() {
        return this.signaling.getKeyStore();
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
        return this.signaling.getState();
    }

    /**
     * Connect asynchronously to the SaltyRTC server.
     *
     * To get notified when the connection is up and running,
     * subscribe to the `ConnectedEvent`.
     *
     * @throws ConnectionException if setting up the WebSocket connection fails.
     */
    public void connect() throws ConnectionException {
        this.signaling.connect();
    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This operation is asynchronous, once the connection is closed, the
     * `SignalingStateChangedEvent` will be emitted.
     */
    public void disconnect() {
        this.signaling.disconnect();
    }

    /**
     * Return the currently used signaling channel.
     */
    public SignalingChannel getSignalingChannel() {
        return this.signaling.getChannel();
    }

    /**
     * Collection of all possible events.
     */
    public static class Events {
        public EventRegistry<SignalingStateChangedEvent> signalingStateChanged = new EventRegistry<>();
        public EventRegistry<SignalingChannelChangedEvent> signalingChannelChanged = new EventRegistry<>();
        public EventRegistry<SendErrorEvent> sendError = new EventRegistry<>();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public boolean getDebug() {
        return debug;
    }

}

/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.events.*;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.InvalidStateException;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.c2c.Application;
import org.saltyrtc.client.signaling.InitiatorSignaling;
import org.saltyrtc.client.signaling.ResponderSignaling;
import org.saltyrtc.client.signaling.Signaling;
import org.saltyrtc.client.signaling.SignalingRole;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tasks.Task;

import javax.net.ssl.SSLContext;

/**
 * The main class used to create a P2P connection through a SaltyRTC signaling server.
 *
 * Note: As end user, you should not instantiate this class directly!
 * Instead, use the `SaltyRTCBuilder` for more convenience.
 */
public class SaltyRTC {

    // Whether to enable debug mode
    private boolean debug = false;

    // Reference to signaling class
    private final Signaling signaling;

    // Event registry
    public final SaltyRTC.Events events = new SaltyRTC.Events();

    // Internal constructor used by SaltyRTCBuilder.
    // Initialize as initiator without trusted key.
    SaltyRTC(KeyStore permanentKey, String host, int port,
             @Nullable SSLContext sslContext,
             @NonNull CryptoProvider cryptoProvider,
             @Nullable Integer wsConnectTimeout,
             @Nullable Integer wsConnectAttemptsMax,
             @Nullable Boolean wsConnectLinearBackoff,
             @Nullable byte[] serverKey,
             Task[] tasks, int pingInterval) {
        this.signaling = new InitiatorSignaling(
            this, host, port, sslContext, cryptoProvider, wsConnectTimeout, wsConnectAttemptsMax, wsConnectLinearBackoff,
            permanentKey, null, serverKey, tasks, pingInterval);
    }

    // Internal constructor used by SaltyRTCBuilder.
    // Initialize as responder without trusted key.
    SaltyRTC(KeyStore permanentKey, String host, int port,
             @Nullable SSLContext sslContext,
             @NonNull CryptoProvider cryptoProvider,
             @Nullable Integer wsConnectTimeout,
             @Nullable Integer wsConnectAttemptsMax,
             @Nullable Boolean wsConnectLinearBackoff,
             byte[] initiatorPublicKey, byte[] authToken,
             @Nullable byte[] serverKey, Task[] tasks, int pingInterval)
             throws InvalidKeyException {
        this.signaling = new ResponderSignaling(
            this, host, port, sslContext, cryptoProvider, wsConnectTimeout, wsConnectAttemptsMax, wsConnectLinearBackoff,
            permanentKey, initiatorPublicKey, authToken, null, serverKey, tasks, pingInterval);
    }

    // Internal constructor used by SaltyRTCBuilder.
    // Initialize as initiator or responder with trusted key.
    SaltyRTC(KeyStore permanentKey, String host, int port,
             @Nullable SSLContext sslContext,
             @NonNull CryptoProvider cryptoProvider,
             @Nullable Integer wsConnectTimeout,
             @Nullable Integer wsConnectAttemptsMax,
             @Nullable Boolean wsConnectLinearBackoff,
             byte[] peerTrustedKey, @Nullable byte[] serverKey, Task[] tasks, int pingInterval,
             SignalingRole role)
             throws InvalidKeyException {
        switch (role) {
            case Initiator:
                this.signaling = new InitiatorSignaling(
                    this, host, port, sslContext, cryptoProvider, wsConnectTimeout, wsConnectAttemptsMax, wsConnectLinearBackoff,
                    permanentKey, peerTrustedKey, serverKey, tasks, pingInterval);
                break;
            case Responder:
                this.signaling = new ResponderSignaling(
                    this, host, port, sslContext, cryptoProvider, wsConnectTimeout, wsConnectAttemptsMax, wsConnectLinearBackoff,
                    permanentKey, null, null, peerTrustedKey, serverKey, tasks, pingInterval);
                break;
            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    public KeyStore getKeyStore() {
        synchronized (this.signaling) {
            return this.signaling.getKeyStore();
        }
    }

    public byte[] getPublicPermanentKey() {
        synchronized (this.signaling) {
            return this.signaling.getPublicPermanentKey();
        }
    }

    public byte[] getAuthToken() {
        synchronized (this.signaling) {
            return this.signaling.getAuthToken();
        }
    }

    /**
     * Return the current signaling state.
     */
    public SignalingState getSignalingState() {
        synchronized (this.signaling) {
            return this.signaling.getState();
        }
    }

    /**
     * Return the negotiated task, or null if no task has been negotiated yet.
     */
    @Nullable
    public Task getTask() {
        synchronized (this.signaling) {
            return this.signaling.getTask();
        }
    }

    /**
     * Connect asynchronously to the SaltyRTC server.
     *
     * To get notified when the connection is up and running,
     * subscribe to the `SignalingStateChangedEvent`.
     *
     * @throws ConnectionException if setting up the WebSocket connection fails.
     */
    public void connect() throws ConnectionException {
        synchronized (this.signaling) {
            this.signaling.connect();
        }
    }

    /**
     * Send an application message to the peer.
     *
     * @throws ConnectionException if sending the message fails due to connectivity issues.
     * @throws InvalidStateException if the SaltyRTC instance is not currently in the TASK signaling state.
     */
    public void sendApplicationMessage(Object data) throws ConnectionException, InvalidStateException {
        synchronized (this.signaling) {
            if (this.signaling.getState() != SignalingState.TASK) {
                throw new InvalidStateException(
                    "Application messages can only be sent in TASK state, not in " + this.signaling.getState().name());
            }
            this.signaling.sendApplication(new Application(data));
        }
    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This is a synchronous operation. The event handlers for the `SignalingStateChangedEvent`
     * will also be called synchronously with the states `CLOSING` and `CLOSED`. Therefore make sure not to call
     * this method again from within your `SignalingStateChangedEvent` event handlers, or deadlocks may occur!
     */
    public void disconnect() {
        synchronized (this.signaling) {
            this.signaling.disconnect();
        }
    }

    /**
     * Collection of all possible events.
     */
    public static class Events {
        public final EventRegistry<SignalingStateChangedEvent> signalingStateChanged = new EventRegistry<>();
        public final EventRegistry<HandoverEvent> handover = new EventRegistry<>();
        public final EventRegistry<ApplicationDataEvent> applicationData = new EventRegistry<>();
        public final EventRegistry<SignalingConnectionLostEvent> signalingConnectionLost = new EventRegistry<>();
        public final EventRegistry<PeerDisconnectedEvent> peerDisconnected = new EventRegistry<>();
        public final EventRegistry<CloseEvent> close = new EventRegistry<>();

        /**
         * Deregister all event handlers.
         */
        @SuppressWarnings("unused")
        public void clearAll() {
            this.signalingStateChanged.clear();
            this.handover.clear();
            this.applicationData.clear();
            this.signalingConnectionLost.clear();
            this.peerDisconnected.clear();
            this.close.clear();
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public boolean getDebug() {
        return debug;
    }

}

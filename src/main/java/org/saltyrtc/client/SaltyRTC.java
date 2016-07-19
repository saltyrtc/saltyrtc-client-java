/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.datachannel.SecureDataChannel;
import org.saltyrtc.client.events.DataEvent;
import org.saltyrtc.client.events.EventRegistry;
import org.saltyrtc.client.events.SignalingChannelChangedEvent;
import org.saltyrtc.client.events.SignalingStateChangedEvent;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.Data;
import org.saltyrtc.client.signaling.InitiatorSignaling;
import org.saltyrtc.client.signaling.ResponderSignaling;
import org.saltyrtc.client.signaling.Signaling;
import org.saltyrtc.client.signaling.SignalingChannel;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;
import org.webrtc.DataChannel;
import org.webrtc.PeerConnection;

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
     * @param sslContext The SSL context used to create the encrypted WebSocket connection.
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
     * Do the handover from WebSocket to WebRTC data channel.
     *
     * The caller must ensure that the `PeerConnection` being passed in has already finished the
     * ICE setup (iceConnectionState==COMPLETED). Otherwise, an exception will be thrown.
     *
     * This operation is asynchronous. To get notified when the handover is finished, subscribe to
     * the `SignalingChannelChangedEvent`.
     *
     * @throws ConnectionException if PeerConnection IceConnectionState is not "COMPLETED".
     */
    public void handover(PeerConnection pc) throws ConnectionException {
        this.signaling.handover(pc);
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
     * Send signaling data to the peer.
     *
     * This method should only be used for signaling, not for sending arbitrary data!
     * For arbitrary data, use `wrapDataChannel` after doing the handover.
     *
     * @throws ConnectionException if signaling channel is not open.
     */
    public void sendSignalingData(Data data) throws ConnectionException {
        this.signaling.sendSignalingData(data);
    }

    /**
     * Return the currently used signaling channel.
     */
    public SignalingChannel getSignalingChannel() {
        return this.signaling.getChannel();
    }

    /**
     * Wrap a data channel. Return a secure data channel.
     * @throws ConnectionException if handover hasn't taken place yet.
     */
    public SecureDataChannel wrapDataChannel(DataChannel dc) throws ConnectionException {
        if (this.getSignalingChannel() != SignalingChannel.DATA_CHANNEL) {
            throw new ConnectionException("Handover must be finished before wrapping a data channel.");
        }
        return new SecureDataChannel(dc, this.signaling);
    }

    /**
     * Collection of all possible events.
     */
    public static class Events {
        public EventRegistry<SignalingStateChangedEvent> signalingStateChanged = new EventRegistry<>();
        public EventRegistry<SignalingChannelChangedEvent> signalingChannelChanged = new EventRegistry<>();
        public EventRegistry<DataEvent> data = new EventRegistry<>();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public boolean getDebug() {
        return debug;
    }

}

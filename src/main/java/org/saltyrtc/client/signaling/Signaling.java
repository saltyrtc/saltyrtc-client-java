/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;
import org.webrtc.DataChannel;

import javax.net.ssl.SSLContext;

public abstract class Signaling {

    protected static String SALTYRTC_WS_SUBPROTOCOL = "saltyrtc-1.0";
    protected static int SALTYRTC_ADDR_UNKNOWN = 0x00;
    protected static int SALTYRTC_ADDR_SERVER = 0x00;
    protected static int SALTYRTC_ADDR_INITIATOR = 0x01;

    // Logger
    protected abstract Logger getLogger();

    // WebSocket
    protected String host;
    protected int port;
    protected String protocol = "wss";
    protected WsClient ws;
    protected SSLContext sslContext;

    // WebRTC / ORTC
    protected DataChannel dc;

    // Connection state
    public SignalingState state = SignalingState.NEW;
    public SignalingChannel channel = SignalingChannel.WEBSOCKET;

    // Reference to main class
    protected SaltyRTC saltyRTC;

    // Keys
    protected byte[] serverKey;
    protected KeyStore permanentKey;
    protected KeyStore sessionKey;
    protected AuthToken authToken;

    // Signaling
    protected int address = SALTYRTC_ADDR_UNKNOWN;
    protected CookiePair cookiePair;
    protected CombinedSequence serverCsn = new CombinedSequence();

    public Signaling(SaltyRTC saltyRTC, String host, int port,
                     KeyStore permanentKey, SSLContext sslContext) {
        this.saltyRTC = saltyRTC;
        this.host = host;
        this.port = port;
        this.permanentKey = permanentKey;
        this.sslContext = sslContext;
    }

    public byte[] getPublicPermanentKey() {
        return this.permanentKey.getPublicKey();
    }

    public byte[] getAuthToken() {
        return this.authToken.getAuthToken();
    }
}

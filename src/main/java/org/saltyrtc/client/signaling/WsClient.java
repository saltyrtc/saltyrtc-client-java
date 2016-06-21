/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class WsClient extends WebSocketClient {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(WsClient.class);

    // Reference to signaling class
    private Signaling signaling;

    /**
     * Create a new WebSocket client instance.
     *
     * @param serverURI URI to the WebSocket server
     * @param headers Map of additional HTTP headers to be sent to the server
     * @param connectTimeout Connect timeout (probably in ms)
     * @param signaling Reference to Signaling instance
     */
    public WsClient(URI serverURI, Map<String, String> headers, int connectTimeout, Signaling signaling) {
        super(serverURI, new Draft_17(), headers, connectTimeout);
        this.signaling = signaling;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOG.debug("Connection opened");
    }

    @Override
    public void onMessage(String message) {
        LOG.debug("New string message: " + message);
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        LOG.debug("New bytes message (" + bytes.array().length + " bytes)");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOG.debug("Connection closed with code " + code + ": " + reason);
        this.signaling.state = SignalingState.CLOSED; // TODO don't set this on handover
    }

    @Override
    public void onError(Exception ex) {
        LOG.error("An error occured!");
    }
}

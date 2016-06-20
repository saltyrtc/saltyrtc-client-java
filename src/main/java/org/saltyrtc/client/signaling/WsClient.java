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
import org.slf4j.Logger;

import java.net.URI;
import java.nio.ByteBuffer;

public class WsClient extends WebSocketClient {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(WsClient.class);

    // Reference to signaling class
    private Signaling signaling;

    public WsClient(URI serverURI, Signaling signaling) {
        super(serverURI, new Draft_17());
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
        LOG.debug("New bytes message: " + bytes.toString());
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOG.debug("Connection closed with code " + code + ": " + reason);
    }

    @Override
    public void onError(Exception ex) {
        LOG.error("An error occured!");
    }
}

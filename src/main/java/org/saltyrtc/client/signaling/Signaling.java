/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.events.ConnectionClosedEvent;
import org.saltyrtc.client.events.ConnectionErrorEvent;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ArrayHelper;
import org.saltyrtc.client.helpers.MessageReader;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.ClientAuth;
import org.saltyrtc.client.messages.InitiatorServerAuth;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.ServerHello;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;
import org.webrtc.DataChannel;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.net.ssl.SSLContext;

public abstract class Signaling {

    protected static String SALTYRTC_WS_SUBPROTOCOL = "saltyrtc-1.0";
    protected static short SALTYRTC_WS_CONNECT_TIMEOUT = 2000;
    protected static short SALTYRTC_ADDR_UNKNOWN = 0x00;
    protected static short SALTYRTC_ADDR_SERVER = 0x00;
    protected static short SALTYRTC_ADDR_INITIATOR = 0x01;

    // Logger
    protected abstract Logger getLogger();

    // WebSocket
    protected String host;
    protected int port;
    protected String protocol = "wss";
    protected WebSocketClient ws;
    protected SSLContext sslContext;

    // WebRTC / ORTC
    protected DataChannel dc;

    // Connection state
    public SignalingState state = SignalingState.NEW;
    public SignalingChannel channel = SignalingChannel.WEBSOCKET;
    protected ServerHandshakeState serverHandshakeState = ServerHandshakeState.NEW;

    // Reference to main class
    protected SaltyRTC saltyRTC;

    // Keys
    protected byte[] serverKey;
    protected KeyStore permanentKey;
    protected KeyStore sessionKey;
    protected AuthToken authToken;

    // Signaling
    protected short address = SALTYRTC_ADDR_UNKNOWN;
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

    /**
     * Connect to the SaltyRTC server.
     *
     * The future will resolve once the WebSocket connection to the server is established.
     * This does not yet mean that the handshake is done. For that, you need to wait for the
     * `ConnectedEvent`.
     */
    public FutureTask<Void> initConnection() {
        return new FutureTask<>(
            new Callable<Void>() {
                @Override
                public Void call() throws InterruptedException, ConnectionException {
                    resetConnection();
                    initWebsocket();
                    if (!connectWebsocket()) {
                        throw new ConnectionException("Connecting to server failed");
                    }
                    return null;
                }
            }
        );
    }

    /**
     * Reset / close the connection.
     *
     * - Close WebSocket if still open.
     * - Set `ws` attribute to null.
     * - Set `state` attribute to `NEW`
     * - Reset server CSN
     */
    protected void resetConnection() {
        this.state = SignalingState.NEW;
        this.serverHandshakeState = ServerHandshakeState.NEW;
        this.serverCsn = new CombinedSequence();

        // Close websocket instance
        if (this.ws != null) {
            getLogger().debug("Disconnecting WebSocket");
            try {
                this.ws.closeBlocking();
            } catch (InterruptedException e) {
                this.ws.close();
            }
            this.ws = null;
        }
    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This operation is asynchronous, once the connection is closed, the
     * `ConnectionClosedEvent` will be emitted.
     */
    public void disconnect() {
        this.state = SignalingState.CLOSING;
        switch (this.channel) {
            case WEBSOCKET:
                // Close websocket instance
                if (this.ws != null) {
                    getLogger().debug("Disconnecting WebSocket");
                    this.ws.close();
                    this.ws = null;
                    // The status will be changed to CLOSED in the `onClose`
                    // implementation of the WebSocket instance.
                }
                break;
            case DATA_CHANNEL:
                throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    /**
     * Return the WebSocket path.
     */
    protected abstract String getWebsocketPath();

    /**
     * Initialize the WebSocket including TLS configuration.
     */
    protected void initWebsocket() {
        // Build connection URL
        final String baseUrl = this.protocol + "://" + this.host + ":" + this.port + "/";
        final URI uri = URI.create(baseUrl + this.getWebsocketPath());

        // Set debug mode
        WebSocketImpl.DEBUG = this.saltyRTC.getDebug();

        // Create WebSocket client instance
        final Map<String, String> headers = new HashMap<>();
        headers.put("Sec-WebSocket-Protocol", SALTYRTC_WS_SUBPROTOCOL);
        this.ws = new WebSocketClient(uri, new Draft_17(), headers, SALTYRTC_WS_CONNECT_TIMEOUT) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                getLogger().debug("WebSocket connection open");
            }

            @Override
            public void onMessage(String message) {
                getLogger().debug("New string message: " + message);
                getLogger().error("Protocol error: Received string message, but only binary messages are valid.");
                Signaling.this.resetConnection();
            }

            @Override
            public void onMessage(ByteBuffer buffer) {
                getLogger().debug("New binary message (" + buffer.array().length + " bytes)");
                try {
                    switch (Signaling.this.state) {
                        case SERVER_HANDSHAKE:
                            Signaling.this.onServerHandshakeMessage(buffer);
                            break;
                        case PEER_HANDSHAKE:
                            Signaling.this.onPeerHandshakeMessage(buffer);
                            break;
                        default:
                            getLogger().warn("Received message in " + Signaling.this.state.name() +
                                             " signaling state. Ignoring.");
                    }
                } catch (ValidationError | SerializationError e) {
                    getLogger().error("Protocol error: Invalid incoming message: " + e.getMessage());
                    Signaling.this.resetConnection();
                } catch (ProtocolException e) {
                    getLogger().error("Protocol error: " + e.getMessage());
                    Signaling.this.resetConnection();
                }
            }

            @Override
            public void onClose(int closeCode, String reason, boolean remote) {
                getLogger().debug("WebSocket connection closed with code " + closeCode + ": " + reason);
                if (closeCode == CloseCode.HANDOVER) {
                    getLogger().info("Handover to data channel");
                } else {
                    switch (closeCode) {
                        case CloseCode.GOING_AWAY:
                            getLogger().error("Server is being shut down");
                            break;
                        case CloseCode.SUBPROTOCOL_ERROR:
                            getLogger().error("No shared sub-protocol could be found");
                            break;
                        case CloseCode.PATH_FULL:
                            getLogger().error("Path full (no free responder byte)");
                            break;
                        case CloseCode.PROTOCOL_ERROR:
                            getLogger().error("Protocol error"); // TODO handle?
                            break;
                        case CloseCode.INTERNAL_ERROR:
                            getLogger().error("Internal server error");
                            break;
                        case CloseCode.DROPPED:
                            getLogger().warn("Dropped by initiator");
                            break;
                    }
                    saltyRTC.events.connectionClosed.notifyHandlers(
                            new ConnectionClosedEvent(SignalingChannel.WEBSOCKET)
                    );
                }
                state = SignalingState.CLOSED; // TODO don't set this on handover
            }

            @Override
            public void onError(Exception ex) {
                getLogger().error("A WebSocket error occured: " + ex.getMessage());
                ex.printStackTrace();
                saltyRTC.events.connectionError.notifyHandlers(
                        new ConnectionErrorEvent(SignalingChannel.WEBSOCKET)
                );
            }
        };

        // Set up TLS
        this.ws.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(this.sslContext));

        getLogger().debug("Initialize WebSocket connection to " + uri);
    }

    /**
     * Connect to WebSocket.
     *
     * @return boolean indicating whether connecting succeeded or not.
     */
    protected boolean connectWebsocket() throws InterruptedException {
        Signaling.this.state = SignalingState.WS_CONNECTING;
        final boolean connected = Signaling.this.ws.connectBlocking();
        if (connected) {
            Signaling.this.state = SignalingState.SERVER_HANDSHAKE;
        } else {
            Signaling.this.state = SignalingState.ERROR;
            Signaling.this.getLogger().error("Connecting to server failed");
        }
        return connected;
    }

    /**
     * Build an optionally encrypted msgpacked message.
     *
     * @param msg The `Message` to be sent.
     * @param receiver The receiver byte.
     * @param encrypt Whether to encrypt the message.
     * @return Encrypted msgpacked bytes, ready to send.
     */
    public byte[] buildPacket(Message msg, short receiver, boolean encrypt) throws ProtocolException {
        // Choose proper combined sequence number
        final CombinedSequence csn = this.getNextCsn(receiver);

        // Create nonce
        final byte[] cookie = this.cookiePair.getOurs().getBytes();
        final SignalingChannelNonce nonce = new SignalingChannelNonce(
                cookie, this.address, receiver,
                csn.getOverflow(), csn.getSequenceNumber());
        final byte[] nonceBytes = nonce.toBytes();

        // Encode message
        final byte[] payload = msg.toBytes();

        // Non encrypted messages can be created by concatenation
        if (!encrypt) {
            return ArrayHelper.concat(nonceBytes, payload);
        }

        // Otherwise, encrypt packet
        final Box box;
        try {
            if (receiver == SALTYRTC_ADDR_SERVER) {
                box = this.encryptForServer(payload, nonceBytes);
            } else if (receiver == SALTYRTC_ADDR_INITIATOR || isResponderByte(receiver)) {
                box = this.encryptForPeer(receiver, msg.getType(), payload, nonceBytes);
            } else {
                throw new ProtocolException("Bad receiver byte: " + receiver);
            }
        } catch (CryptoFailedException | InvalidKeyException e) {
            e.printStackTrace();
            throw new ProtocolException("Encrypting failed: " + e.getMessage());
        }
        return box.toBytes();
    }

    /**
     * Build an encrypted msgpacked message.
     *
     * @param msg The `Message` to be sent.
     * @param receiver The receiver byte.
     * @return Encrypted msgpacked bytes, ready to send.
     */
    public byte[] buildPacket(Message msg, short receiver) throws ProtocolException {
        return this.buildPacket(msg, receiver, true);
    }

    /**
     * Message received during server handshake.
     *
     * @param buffer The ByteBuffer containing the raw message bytes.
     */
    protected void onServerHandshakeMessage(ByteBuffer buffer) throws ValidationError, SerializationError, ProtocolException {
        // Parse nonce
        final SignalingChannelNonce nonce = new SignalingChannelNonce(buffer);
        assert buffer.position() == SignalingChannelNonce.TOTAL_LENGTH;

        // Get payload bytes
        final byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        // Decrypt if necessary
        final byte[] payload;
        if (this.serverHandshakeState != ServerHandshakeState.NEW) {
            final Box box = new Box(nonce.toBytes(), data);
            try {
                payload = this.permanentKey.decrypt(box, this.serverKey);
            } catch (CryptoFailedException | InvalidKeyException e) {
                e.printStackTrace();
                throw new ProtocolException("Could not decrypt server message");
            }
        } else {
            payload = data;
        }

        Message msg = MessageReader.read(payload);
        switch (this.serverHandshakeState) {
            case NEW:
                // Expect server-hello
                if (msg instanceof ServerHello) {
                    getLogger().debug("Received server-hello");
                    // TODO: Validate nonce
                    this.handleServerHello((ServerHello) msg, nonce);
                    this.sendClientHello();
                    this.sendClientAuth();
                } else {
                    throw new ProtocolException("Expected server-hello message, but got " + msg.getType());
                }
                break;
            case HELLO_SENT:
                throw new ProtocolException("Received " + msg.getType() + " message before sending client-auth");
            case AUTH_SENT:
                // Expect server-auth
                if (msg instanceof InitiatorServerAuth || msg instanceof ResponderServerAuth) {
                    getLogger().debug("Received server-auth");
                    // TODO: Validate nonce
                    this.handleServerAuth(msg, nonce);
                }
        }
    }

    /**
     * Handle an incoming server-hello message.
     */
    protected void handleServerHello(ServerHello msg, SignalingChannelNonce nonce) {
        // Store server public key
        this.serverKey = msg.getKey();

        // Generate cookie
        Cookie ourCookie;
        final Cookie serverCookie = nonce.getCookie();
        do {
            ourCookie = new Cookie();
        } while (ourCookie.equals(serverCookie));
        this.cookiePair = new CookiePair(ourCookie, serverCookie);
    }

    /**
     * Send a client-hello message to the server.
     */
    protected abstract void sendClientHello() throws ProtocolException;

    /**
     * Send a client-auth message to the server.
     */
    protected void sendClientAuth() throws ProtocolException {
        final ClientAuth msg = new ClientAuth(this.cookiePair.getTheirs().getBytes());
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_SERVER);
        getLogger().debug("Sending client-auth");
        this.ws.send(packet);
        this.serverHandshakeState = ServerHandshakeState.AUTH_SENT;
    }

    /**
     * Handle an incoming server-auth message.
     *
     * Note that the message has not yet been casted to the correct subclass.
     * That needs to be done (differently) in the initiator and
     * responder signaling subclasses.
     */
    protected abstract void handleServerAuth(Message baseMsg, SignalingChannelNonce nonce) throws ProtocolException;

    /**
     * Message received during peer handshake.
     */
    protected void onPeerHandshakeMessage(ByteBuffer buffer) {

    }

    /**
     * Choose proper combined sequence number
     */
    protected abstract CombinedSequence getNextCsn(short receiver) throws ProtocolException;

    /**
     * Return `true` if receiver byte is a valid responder byte.
     */
    protected boolean isResponderByte(short receiver) {
        return receiver >= 0x02 && receiver <= 0xff;
    }

    /**
     * Encrypt data for the server.
     */
    protected Box encryptForServer(byte[] payload, byte[] nonce)
            throws CryptoFailedException, InvalidKeyException {
        return this.permanentKey.encrypt(payload, nonce, this.serverKey);
    }

    /**
     * Encrypt data for the specified peer.
     */
    protected abstract Box encryptForPeer(short receiver, String messageType, byte[] payload, byte[] nonce)
        throws CryptoFailedException, InvalidKeyException, ProtocolException;
}
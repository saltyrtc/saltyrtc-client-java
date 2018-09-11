/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import com.neovisionaries.ws.client.*;
import org.saltyrtc.chunkedDc.UnsignedHelper;
import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.events.*;
import org.saltyrtc.client.exceptions.*;
import org.saltyrtc.client.helpers.ArrayHelper;
import org.saltyrtc.client.helpers.MessageHistory;
import org.saltyrtc.client.helpers.MessageReader;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.keystore.SharedKeyStore;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.c2c.Application;
import org.saltyrtc.client.messages.c2c.Close;
import org.saltyrtc.client.messages.c2c.TaskMessage;
import org.saltyrtc.client.messages.s2c.*;
import org.saltyrtc.client.nonce.CombinedSequenceSnapshot;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.peers.Peer;
import org.saltyrtc.client.signaling.peers.Server;
import org.saltyrtc.client.signaling.state.HandoverState;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class for initiator and responder signaling.
 *
 * Note: As end user, you should not instantiate this class directly!
 * Instead, use the `SaltyRTCBuilder` for more convenience.
 */
@SuppressWarnings("WeakerAccess")
public abstract class Signaling implements SignalingInterface {

    static final String SALTYRTC_SUBPROTOCOL = "v1.saltyrtc.org";
    static final short SALTYRTC_WS_CONNECT_TIMEOUT = 3000;
    static final short SALTYRTC_WS_CONNECT_ATTEMPTS_MAX = 20;
    static final boolean SALTYRTC_WS_CONNECT_LINEAR_BACKOFF = true;
    static final long SALTYRTC_WS_PING_INTERVAL = 20000;
    static final short SALTYRTC_ADDR_UNKNOWN = 0x00;
    static final short SALTYRTC_ADDR_SERVER = 0x00;
    static final short SALTYRTC_ADDR_INITIATOR = 0x01;

    // Logger
    abstract Logger getLogger();

    // WebSocket
    private final String host;
    private final int port;
    private final SSLContext sslContext;
    private WebSocket ws;
    final private int pingInterval;
    final private int wsConnectTimeoutInitial;
    final private int wsConnectAttemptsMax;
    final private boolean wsConnectLinearBackoff;
    private int wsConnectTimeout;
    private int wsConnectAttempt = 0;

    // Connection state
    private SignalingState state = SignalingState.NEW;
    private final HandoverState handoverState = new HandoverState();

    // Reference to main class
    final SaltyRTC salty;

    // Server information
    @NonNull Server server;

    // Our keys
    @NonNull final KeyStore permanentKey;

    // Peer trusted key or auth token
    @Nullable AuthToken authToken;
    @Nullable byte[] peerTrustedKey;

    // Server trusted key
    @Nullable byte[] expectedServerKey;

    // Signaling
    @NonNull private SignalingRole role;
    short address = SALTYRTC_ADDR_UNKNOWN;

    // Tasks
    @NonNull final Task[] tasks;
    Task task;

    // Message history
    private final MessageHistory history = new MessageHistory(10);

    public Signaling(SaltyRTC salty, String host, int port,
                     @Nullable SSLContext sslContext,
                     @Nullable Integer wsConnectTimeout,
                     @Nullable Integer wsConnectAttemptsMax,
                     @Nullable Boolean wsConnectLinearBackoff,
                     @NonNull KeyStore permanentKey,
                     @Nullable byte[] peerTrustedKey,
                     @Nullable byte[] expectedServerKey,
                     @NonNull SignalingRole role,
                     @NonNull Task[] tasks,
                     int pingInterval) {
        this.salty = salty;
        this.host = host;
        this.port = port;
        this.sslContext = sslContext;
        this.wsConnectTimeoutInitial = wsConnectTimeout == null ? SALTYRTC_WS_CONNECT_TIMEOUT : wsConnectTimeout;
        this.wsConnectAttemptsMax = wsConnectAttemptsMax == null ? SALTYRTC_WS_CONNECT_ATTEMPTS_MAX : wsConnectAttemptsMax;
        this.wsConnectLinearBackoff = wsConnectLinearBackoff == null ? SALTYRTC_WS_CONNECT_LINEAR_BACKOFF : wsConnectLinearBackoff;
        this.permanentKey = permanentKey;
        this.peerTrustedKey = peerTrustedKey;
        this.expectedServerKey = expectedServerKey;
        this.role = role;
        this.tasks = tasks;
        this.server = new Server();
        this.pingInterval = pingInterval;

        // When the handover is complete, notify event handlers and close the WebSocket.
        this.handoverState.handoverComplete.register(event -> {
            Signaling.this.salty.events.handover.notifyHandlers(new HandoverEvent());
            Signaling.this.ws.sendClose(CloseCode.HANDOVER);
            return false;
        });
    }

    @NonNull
    public KeyStore getKeyStore() {
        return this.permanentKey;
    }

    @NonNull
    public byte[] getPublicPermanentKey() {
        return this.permanentKey.getPublicKey();
    }

    @Nullable
    public byte[] getAuthToken() {
        if (this.authToken != null) {
            return this.authToken.getAuthToken();
        }
        return null;
    }

    /**
     * Return true if the signaling class has been initialized with a trusted peer key.
     */
    boolean hasTrustedKey() {
        return this.peerTrustedKey != null;
    }

    @NonNull
    public SignalingState getState() {
        return this.state;
    }

    public void setState(SignalingState newState) {
        if (this.state != newState) {
            this.state = newState;
            this.salty.events.signalingStateChanged.notifyHandlers(
                    new SignalingStateChangedEvent(newState));
        }
    }

    public HandoverState getHandoverState() {
        return this.handoverState;
    }

    @NonNull
    public SignalingRole getRole() {
        return this.role;
    }

    /**
     * Connect asynchronously to the SaltyRTC server.
     *
     * To get notified when the connection is up and running, subscribe to the `SignalingStateChangedEvent`.
     *
     * @throws ConnectionException if setting up the WebSocket connection fails.
     */
    public void connect() throws ConnectionException {
        this.getLogger().info("Connecting to SaltyRTC server at "
                + this.host + ":" + this.port + "...");
        this.resetConnection(null);
        try {
            this.initWebsocket();
        } catch (IOException e) {
            throw new ConnectionException("Setting up WebSocket failed.", e);
        }
        this.connectWebsocket();
    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This is a synchronous operation. The event handlers for the `SignalingStateChangedEvent`
     * will also be called synchronously with the states `CLOSING` and `CLOSED`. Therefore make sure not to call
     * this method again from within your `SignalingStateChangedEvent` event handlers, or deadlocks may occur!
     */
    synchronized void disconnect(int reason) {
        this.setState(SignalingState.CLOSING);

        // Send close message if necessary
        if (this.getState() == SignalingState.TASK) {
            this.sendClose(reason);
        }

        // Close WebSocket instance
        if (this.ws != null) {
            this.getLogger().debug("Disconnecting WebSocket (reason: " + reason + ")");
            this.ws.disconnect(reason);
        }
        this.ws = null;

        // Close task connections
        if (this.task != null) {
            this.getLogger().debug("Closing task connections (reason: " + reason + ")");
            this.task.close(reason);
        }

        // Update state
        this.setState(SignalingState.CLOSED);
    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This is a synchronous operation. The event handlers for the `SignalingStateChangedEvent`
     * will also be called synchronously with the states `CLOSING` and `CLOSED`. Therefore make sure not to call
     * this method again from within your `SignalingStateChangedEvent` event handlers, or deadlocks may occur!
     */
    public void disconnect() {
        this.disconnect(CloseCode.CLOSING_NORMAL);
    }

    /**
     * Reset the connection.
     */
    public synchronized void resetConnection(@Nullable Integer reason) {
        // Disconnect
        if (this.state != SignalingState.NEW) {
            final int code = reason != null ? reason : CloseCode.CLOSING_NORMAL;
            this.disconnect(code);
        }

        // Reset
        this.server = new Server();
        this.handoverState.reset();
        this.setState(SignalingState.NEW);
        this.getLogger().debug("Connection reset");
    }

    /**
     * Return the WebSocket path.
     */
    abstract String getWebsocketPath();

    /**
     * Initialize the WebSocket including TLS configuration.
     *
     * @throws IOException if setting up websocket fails
     */
    private void initWebsocket() throws IOException {
        // Build connection URL
        final String baseUrl = "wss://" + this.host + ":" + this.port + "/";
        final URI uri = URI.create(baseUrl + this.getWebsocketPath());
        this.getLogger().debug("Initialize WebSocket connection to " + uri);

        WebSocketAdapter listener = new WebSocketAdapter() {
            @Override
            @SuppressWarnings("UnqualifiedMethodAccess")
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
                synchronized (this) {
                    if (getState() == SignalingState.WS_CONNECTING) {
                        getLogger().warn("WebSocket connection open");
                        setState(SignalingState.SERVER_HANDSHAKE);
                    } else {
                        getLogger().warn("Got onConnected event, but WebSocket connection already open");
                    }
                }
            }

            @Override
            @SuppressWarnings("UnqualifiedMethodAccess")
            public void onConnectError(WebSocket websocket, WebSocketException ex) throws Exception {
                getLogger().error("Could not connect to websocket (" + ex.getError().toString() + "): " + ex.getMessage());
                if (Signaling.this.wsConnectAttemptsMax <= 0 || Signaling.this.wsConnectAttempt < Signaling.this.wsConnectAttemptsMax) {
                    // Increase #attempts (and timeout if needed)
                    if (Signaling.this.wsConnectLinearBackoff) {
                        Signaling.this.wsConnectTimeout += Signaling.this.wsConnectTimeoutInitial;
                    }
                    Signaling.this.wsConnectAttempt += 1;

                    // Log retry attempt
                    final String retryConstraint;
                    if (Signaling.this.wsConnectAttemptsMax <= 0) {
                        retryConstraint = "infinitely";
                    } else {
                        retryConstraint = Signaling.this.wsConnectAttempt + "/" + Signaling.this.wsConnectAttemptsMax;
                    }
                    getLogger().info("Retrying to reconnect (" + retryConstraint + ")...");

                    // Retry WS connection
                    Signaling.this.setState(SignalingState.WS_CONNECTING);
                    Signaling.this.ws.recreate(Signaling.this.wsConnectTimeout).connectAsynchronously();
                } else {
                    getLogger().info("Giving up.");
                    setState(SignalingState.ERROR);
                }
            }

            @Override
            @SuppressWarnings("UnqualifiedMethodAccess")
            public void onTextMessage(WebSocket websocket, String text) {
                getLogger().debug("New string message: " + text);
                getLogger().error("Protocol error: Received string message, but only binary messages are valid.");
                Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
            }

            @Override
            @SuppressWarnings("UnqualifiedMethodAccess")
            public synchronized void onBinaryMessage(WebSocket websocket, byte[] binary) {
                getLogger().debug("New binary message (" + binary.length + " bytes)");

                // Update state if necessary
                if (getState() == SignalingState.WS_CONNECTING) {
                    getLogger().info("WebSocket connection open");
                    setState(SignalingState.SERVER_HANDSHAKE);
                }

                SignalingChannelNonce nonce = null;
                try {
                    // Parse buffer
                    final Box box = new Box(ByteBuffer.wrap(binary), SignalingChannelNonce.TOTAL_LENGTH);

                    // Parse and validate nonce
                    nonce = new SignalingChannelNonce(ByteBuffer.wrap(box.getNonce()));
                    validateNonce(nonce);

                    // Check peer handover state
                    if (nonce.getSource() != SALTYRTC_ADDR_SERVER && Signaling.this.handoverState.getPeer()) {
                        getLogger().error("Protocol error: Received WebSocket message from peer " +
                            "even though it has already handed over to task.");
                        Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                        return;
                    }

                    // Dispatch message
                    switch (Signaling.this.getState()) {
                        case SERVER_HANDSHAKE:
                            Signaling.this.onServerHandshakeMessage(box, nonce);
                            break;
                        case PEER_HANDSHAKE:
                            Signaling.this.onPeerHandshakeMessage(box, nonce);
                            break;
                        case TASK:
                            Signaling.this.onSignalingMessage(box, nonce);
                            break;
                        default:
                            getLogger().warn("Received message in " + Signaling.this.getState().name() +
                                    " signaling state. Ignoring.");
                    }
                // TODO: The following errors could also be handled using `handleCallbackError` on the websocket.
                } catch (ValidationError e) {
                    if (e.critical) {
                        getLogger().error("Protocol error: Invalid incoming message: " + e.getMessage());
                        e.printStackTrace();
                        Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                    } else {
                        getLogger().warn("Dropping invalid message: " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (SerializationError e) {
                    getLogger().error("Protocol error: Invalid incoming message: " + e.getMessage());
                    e.printStackTrace();
                    Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                } catch (InternalException e) {
                    getLogger().error("Internal server error: " + e.getMessage());
                    e.printStackTrace();
                    Signaling.this.resetConnection(CloseCode.INTERNAL_ERROR);
                } catch (ConnectionException e) {
                    getLogger().error("Connection error: " + e.getMessage());
                    e.printStackTrace();
                    Signaling.this.resetConnection(CloseCode.INTERNAL_ERROR);
                } catch (SignalingException e) {
                    getLogger().error("Signaling error: " + CloseCode.explain(e.getCloseCode()));
                    e.printStackTrace();
                    switch (Signaling.this.getState()) {
                        case NEW:
                        case WS_CONNECTING:
                        case SERVER_HANDSHAKE:
                            // Close connection
                            Signaling.this.resetConnection(e.getCloseCode());
                            break;
                        case PEER_HANDSHAKE:
                            // Handle error depending on role
                            Signaling.this.handlePeerHandshakeSignalingError(e, nonce.getSource());
                            break;
                        case TASK:
                            // Close websocket connection
                            Signaling.this.sendClose(e.getCloseCode());
                            Signaling.this.resetConnection(CloseCode.CLOSING_NORMAL);
                            break;
                        case CLOSING:
                        case CLOSED:
                            // Ignore
                            break;
                    }
                }
            }

            @Override
            @SuppressWarnings("UnqualifiedMethodAccess")
            public void onDisconnected(WebSocket websocket,
                                       @Nullable WebSocketFrame serverCloseFrame,
                                       @Nullable WebSocketFrame clientCloseFrame,
                                       boolean closedByServer) {
                // Log details to debug log
                final String closer = closedByServer ? "server" : "client";
                final WebSocketFrame frame = closedByServer ? serverCloseFrame : clientCloseFrame;
                final int closeCode = frame == null ? 0 : frame.getCloseCode();
                String closeReason = frame == null ? null : frame.getCloseReason();
                if (closeReason == null) {
                    closeReason = CloseCode.explain(closeCode);
                }
                getLogger().debug("WebSocket connection closed by " + closer +
                                  " with code " + closeCode + ": " + closeReason);

                // Log some of the codes on higher log levels too
                if (closedByServer) {
                    switch (closeCode) {
                        case 0:
                            getLogger().warn("WebSocket closed (no close frame provided)");
                            break;
                        case CloseCode.CLOSING_NORMAL:
                            getLogger().info("WebSocket closed normally");
                            break;
                        case CloseCode.TIMEOUT:
                            getLogger().info("WebSocket closed due to timeout");
                            break;
                        case CloseCode.GOING_AWAY:
                            getLogger().warn("WebSocket closed, server is being shut down");
                            break;
                        case CloseCode.NO_SHARED_SUBPROTOCOL:
                            getLogger().warn("WebSocket closed: No shared sub-protocol could be found");
                            break;
                        case CloseCode.PATH_FULL:
                            getLogger().warn("WebSocket closed: Path full (no free responder byte)");
                            break;
                        case CloseCode.PROTOCOL_ERROR:
                            getLogger().error("WebSocket closed: Protocol error");
                            break;
                        case CloseCode.INTERNAL_ERROR:
                            getLogger().error("WebSocket closed: Internal server error");
                            break;
                        case CloseCode.DROPPED_BY_INITIATOR:
                            getLogger().info("WebSocket closed: Dropped by initiator");
                            break;
                        case CloseCode.INITIATOR_COULD_NOT_DECRYPT:
                            getLogger().warn("WebSocket closed: Initiator could not decrypt message");
                            break;
                        case CloseCode.NO_SHARED_TASK:
                            getLogger().warn("WebSocket closed: No shared task was found");
                            break;
                        case CloseCode.INVALID_KEY:
                            getLogger().error("WebSocket closed: An invalid public permanent server key was specified");
                            break;
                    }
                }
                // Note: Don't check for signaling state here, it will already have been resetted.
                if (closeCode != CloseCode.HANDOVER) {
                    Signaling.this.salty.events.close.notifyHandlers(new CloseEvent(closeCode));
                    setState(SignalingState.CLOSED);
                }
            }

            /**
             * WebSocketListener has some onXxxError() methods such as onFrameError() and onSendError(). Among such
             * methods, onError() is a special one. It is always called before any other onXxxError() is called.
             * For example, in the implementation of run() method of ReadingThread, Throwable is caught and onError()
             * and onUnexpectedError() are called in this order. The following is the implementation.
             */
            @Override
            @SuppressWarnings("UnqualifiedMethodAccess")
            public void onError(WebSocket websocket, WebSocketException cause) {
                getLogger().warn("A WebSocket error occured: " + cause.getMessage(), cause);
            }

            @Override
            @SuppressWarnings("UnqualifiedMethodAccess")
            public void handleCallbackError(WebSocket websocket, Throwable cause) {
                getLogger().error("WebSocket callback error: " + cause);
                cause.printStackTrace();
                Signaling.this.resetConnection(CloseCode.INTERNAL_ERROR);
            }
        };

        // Reset timeout
        this.wsConnectTimeout = this.wsConnectTimeoutInitial;

        // Create WebSocket client instance
        this.ws = new WebSocketFactory()
                .setConnectionTimeout(this.wsConnectTimeout)
                .setSSLContext(this.sslContext)
                .setVerifyHostname(true)
                .createSocket(uri)
                .setPingInterval(SALTYRTC_WS_PING_INTERVAL)
                .addProtocol(SALTYRTC_SUBPROTOCOL)
                .addListener(listener);
    }

    /**
     * Connect asynchronously to WebSocket.
     */
    private void connectWebsocket() {
        this.setState(SignalingState.WS_CONNECTING);
        this.wsConnectAttempt = 1;
        this.ws.connectAsynchronously();
    }

    /**
     * Build an optionally encrypted msgpacked message.
     *
     * @param msg The `Message` to be sent.
     * @param receiver The receiver.
     * @param encrypt Whether to encrypt the message.
     * @return Encrypted msgpacked bytes, ready to send.
     */
    byte[] buildPacket(Message msg, Peer receiver, boolean encrypt) throws ProtocolException {
        // Choose proper combined sequence number
        final CombinedSequenceSnapshot csn;
        try {
            csn = receiver.getCsnPair().getOurs().next();
        } catch (OverflowException e) {
            throw new ProtocolException("CSN overflow", e);
        }

        // Create nonce
        final SignalingChannelNonce nonce = new SignalingChannelNonce(
                receiver.getCookiePair().getOurs().getBytes(), this.address, receiver.getId(),
                csn.getOverflow(), csn.getSequenceNumber());
        final byte[] nonceBytes = nonce.toBytes();

        // Encode message
        final byte[] payload = msg.toBytes();

        // Non encrypted messages can be created by concatenation
        if (!encrypt) {
            return ArrayHelper.concat(nonceBytes, payload);
        }

        // Otherwise, encrypt packet
        // TODO: Use polymorphism using peer object
        final Box box;
        try {
            if (receiver.getId() == SALTYRTC_ADDR_SERVER) {
                box = this.encryptHandshakeDataForServer(payload, nonceBytes);
            } else if (receiver.getId() == SALTYRTC_ADDR_INITIATOR || this.isResponderId(receiver.getId())) {
                box = this.encryptHandshakeDataForPeer(receiver.getId(), msg.getType(), payload, nonceBytes);
            } else {
                throw new ProtocolException("Bad receiver byte: " + receiver);
            }
        } catch (CryptoFailedException | InvalidKeyException e) {
            throw new ProtocolException("Encrypting failed: " + e.getMessage(), e);
        }

        // Store message in message history
        this.history.store(msg, nonce);

        return box.toBytes();
    }

    /**
     * Build an encrypted msgpacked message.
     *
     * @param msg The `Message` to be sent.
     * @param receiver The receiver byte.
     * @return Encrypted msgpacked bytes, ready to send.
     */
    byte[] buildPacket(Message msg, Peer receiver) throws ProtocolException {
        return this.buildPacket(msg, receiver, true);
    }

    /**
     * Handle signaling errors during peer handshake.
     */
    abstract void handlePeerHandshakeSignalingError(@NonNull SignalingException e, short source);

    /**
     * Return the peer instance.
     *
     * May return null if peer is not yet set.
     */
    @Nullable
    abstract Peer getPeer();

    /**
     * Message received during server handshake.
     *
     * @param box The box containing raw nonce and payload bytes.
     */
    private void onServerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ValidationError, SerializationError, SignalingException, ConnectionException {
        // Decrypt if necessary
        final byte[] payload;
        if (this.server.handshakeState == ServerHandshakeState.NEW) {
            // The very first message is unencrypted
            payload = box.getData();
        } else {
            // Later, they're encrypted with our permanent key and the server key
            try {
                final SharedKeyStore sks = this.server.getSessionSharedKey();
                assert sks != null;
                payload = sks.decrypt(box);
            } catch (CryptoFailedException e) {
                throw new ProtocolException("Could not decrypt server message", e);
            }
        }

        // Handle message depending on state
        Message msg = MessageReader.read(payload);
        switch (this.server.handshakeState) {
            case NEW:
                // Expect server-hello
                if (msg instanceof ServerHello) {
                    this.getLogger().debug("Received server-hello");
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
                    this.getLogger().debug("Received server-auth");
                    this.handleServerAuth(msg, nonce);
                } else {
                    throw new ProtocolException("Expected server-auth message, but got " + msg.getType());
                }
                break;
            case DONE:
                throw new SignalingException(CloseCode.INTERNAL_ERROR,
                    "Received server handshake message even though server handshake state is set to DONE");
            default:
                throw new SignalingException(CloseCode.INTERNAL_ERROR, "Unknown server handshake state");
        }

        // Check if we're done yet
        if (this.server.handshakeState == ServerHandshakeState.DONE) {
            this.setState(SignalingState.PEER_HANDSHAKE);
            this.getLogger().info("Server handshake done");
            this.initPeerHandshake();
        }
    }

    /**
     * Message received during peer handshake.
     */
    abstract void onPeerHandshakeMessage(Box box, SignalingChannelNonce nonce)
        throws ValidationError, SerializationError,
        InternalException, ConnectionException, SignalingException;

    /**
     * Message received from peer or server *after* the handshake is done.
     */
    private void onSignalingMessage(Box box, SignalingChannelNonce nonce) throws SignalingException {
        this.getLogger().debug("Message received");
        if (nonce.getSource() == SALTYRTC_ADDR_SERVER) {
            this.onSignalingServerMessage(box);
        } else {
            final byte[] decrypted;
            try {
                decrypted = this.decryptFromPeer(box);
            } catch (CryptoFailedException e) {
                this.getLogger().error("Could not decrypt incoming message from peer " + nonce.getSource(), e);
                return;
            }
            this.onSignalingPeerMessage(decrypted);
        }
    }

    /**
     * Signaling message received from server *after* the handshake is done.
     */
    private void onSignalingServerMessage(Box box) throws SignalingException {
        final Message message;

        try {
            final SharedKeyStore sks = this.server.getSessionSharedKey();
            assert sks != null;
            final byte[] decrypted = sks.decrypt(box);
            message = MessageReader.read(decrypted);
        } catch (CryptoFailedException e) {
            this.getLogger().error("Could not decrypt incoming message from server", e);
            return;
        } catch (ValidationError | SerializationError e) {
            this.getLogger().error("Received invalid message from server", e);
            return;
        }

        if (message instanceof SendError) {
            this.handleSendError((SendError) message);
        } else if (message instanceof Disconnected) {
            this.handleDisconnected((Disconnected) message);
        } else {
            this.getLogger().error("Invalid server message type: " + message.getType());
        }
    }

    /**
     * Signaling message received from peer *after* the handshake is done.
     */
    public void onSignalingPeerMessage(byte[] decryptedBytes) {
        final Message message;

        try {
            message = MessageReader.read(decryptedBytes, this.task.getSupportedMessageTypes());
        } catch (ValidationError | SerializationError e) {
            this.getLogger().error("Received invalid message from peer", e);
            return;
        }

        if (message instanceof Close) {
            this.getLogger().debug("Received close");
            this.handleClose((Close) message);
        } else if (message instanceof TaskMessage) {
            this.getLogger().debug("Received task message");
            this.task.onTaskMessage((TaskMessage) message);
        } else if (message instanceof Application) {
            this.getLogger().debug("Received application message");
            this.handleApplication((Application) message);
        } else {
            this.getLogger().error("Received message with invalid type from peer");
        }
    }

    /**
     * Handle an incoming server-hello message.
     */
    private void handleServerHello(ServerHello msg, SignalingChannelNonce nonce) throws ProtocolException {
        // Update server instance
        try {
            this.server.setSessionSharedKey(msg.getKey(), this.permanentKey);
        } catch (InvalidKeyException e) {
            throw new ProtocolException("Server sent invalid session key in server-hello message", e);
        }
        this.server.getCookiePair().setTheirs(nonce.getCookie());
    }

    /**
     * Send a client-hello message to the server.
     */
    abstract void sendClientHello() throws SignalingException, ConnectionException;

    /**
     * Send a client-auth message to the server.
     */
    private void sendClientAuth() throws SignalingException, ConnectionException {
        final Cookie serverCookie = this.server.getCookiePair().getTheirs();
        assert serverCookie != null;
        final byte[] yourCookie = serverCookie.getBytes();

        final List<String> subprotocols = Collections.singletonList(Signaling.SALTYRTC_SUBPROTOCOL);

        final ClientAuth msg;
        if (this.server.hasPermanentSharedKey()) {
            final SharedKeyStore permanentSharedKey = this.server.getPermanentSharedKey();
            assert permanentSharedKey != null;
            final byte[] serverKey = permanentSharedKey.getRemotePublicKey();
            msg = new ClientAuth(yourCookie, serverKey, subprotocols, this.pingInterval);
        } else {
            msg = new ClientAuth(yourCookie, subprotocols, this.pingInterval);
        }

        final byte[] packet = this.buildPacket(msg, this.server);
        this.getLogger().debug("Sending client-auth");
        this.send(packet, msg);
        this.server.handshakeState = ServerHandshakeState.AUTH_SENT;
    }

    /**
     * Handle an incoming server-auth message.
     *
     * Note that the message has not yet been casted to the correct subclass.
     * That needs to be done (differently) in the initiator and
     * responder signaling subclasses.
     */
    abstract void handleServerAuth(Message baseMsg, SignalingChannelNonce nonce) throws
        SignalingException, ConnectionException;

    /**
     * Validate the signed keys sent by the server in the server-auth message.
     *
     * @param signedKeys The `signed_keys` field from the server-auth message.
     * @param nonce The incoming message nonce.
     * @param expectedServerKey The expected server public permanent key.
     * @throws ValidationError if the signed keys are not valid.
     */
    void validateSignedKeys(@Nullable byte[] signedKeys,
                            @NonNull SignalingChannelNonce nonce,
                            @NonNull byte[] expectedServerKey)
            throws ValidationError {
        assert this.server.hasSessionSharedKey();
        if (signedKeys == null) {
            throw new ValidationError("Server did not send signed_keys in server-auth message");
        }
        final Box box = new Box(nonce.toBytes(), signedKeys);
        final byte[] decrypted;
        final SharedKeyStore sessionSharedKey = this.server.getSessionSharedKey();
        assert sessionSharedKey != null;
        try {
            this.getLogger().debug("Expected server key is " + NaCl.asHex(expectedServerKey));
            this.getLogger().debug("Server session key is " + NaCl.asHex(sessionSharedKey.getRemotePublicKey()));
            // Note: We will not create a SharedKeyStore here since this will be done only once
            decrypted = this.permanentKey.decrypt(box, expectedServerKey);
        } catch (CryptoFailedException e) {
            throw new ValidationError("Could not decrypt signed_keys in server-auth message", e);
        } catch (InvalidKeyException e) {
            throw new ValidationError("Invalid key when trying to decrypt signed_keys in server-auth message", e);
        }
        final byte[] expected = ArrayHelper.concat(
            sessionSharedKey.getRemotePublicKey(),
            this.permanentKey.getPublicKey()
        );
        if (!Arrays.equals(decrypted, expected)) {
            throw new ValidationError("Decrypted signed_keys in server-auth message is invalid");
        }
    }

    /**
     * Initialize the peer handshake.
     */
    abstract void initPeerHandshake() throws SignalingException, ConnectionException;

    /**
     * Initialize the task with the task data sent by the peer.
     * @param task The task instance.
     * @param data The task data provided by the peer.
     */
    void initTask(Task task, Map<Object, Object> data) throws ProtocolException {
        try {
            task.init(this, data);
        } catch (ValidationError e) {
            e.printStackTrace();
            throw new ProtocolException("Peer sent invalid task data", e);
        }
        this.task = task;
    }

    /**
     * Return the negotiated task, or null if no task has been negotiated yet.
     */
    @Nullable
    public Task getTask() {
        return this.task;
    }

    /**
     * Send a close message to the peer.
     */
    public void sendClose(int reason) {
        final Close msg = new Close(reason);
        final byte[] packet;
        try {
            packet = this.buildPacket(msg, this.getPeer());
        } catch (ProtocolException | NullPointerException e) {
            e.printStackTrace();
            this.getLogger().error("Could not build close message");
            return;
        }
        this.getLogger().debug("Sending close");
        try {
            this.send(packet, msg);
        } catch (SignalingException | ConnectionException e) {
            e.printStackTrace();
            this.getLogger().error("Could not send close message");
        }
    }

    /**
     * Return `true` if receiver byte is a valid responder id (in the range 0x02-0xff).
     */
    boolean isResponderId(short receiver) {
        return receiver >= 0x02 && receiver <= 0xff;
    }

    /**
     * Validate the nonce.
     */
    private void validateNonce(SignalingChannelNonce nonce) throws ValidationError, SignalingException {
        this.validateNonceSource(nonce);
        this.validateNonceDestination(nonce);
        this.validateNonceCsn(nonce);
        this.validateNonceCookie(nonce);
    }

    /**
     * Validate the sender address in the nonce.
     */
    private void validateNonceSource(SignalingChannelNonce nonce) throws ValidationError {
        // An initiator SHALL ONLY process messages from the server (0x00). As
        // soon as the initiator has been assigned an identity, it MAY ALSO accept
        // messages from other responders (0x02..0xff). Other messages SHALL be
        // discarded and SHOULD trigger a warning.
        //
        // A responder SHALL ONLY process messages from the server (0x00). As soon
        // as the responder has been assigned an identity, it MAY ALSO accept
        // messages from the initiator (0x01). Other messages SHALL be discarded
        // and SHOULD trigger a warning.
        switch (this.getState()) {
            case SERVER_HANDSHAKE:
                // Messages during server handshake must come from the server.
                if (nonce.getSource() != SALTYRTC_ADDR_SERVER) {
                    throw new ValidationError("Received message during server handshake " +
                            "with invalid sender address (" +
                            nonce.getSource() + " != " + SALTYRTC_ADDR_SERVER + ")",
                            false);
                }
                break;
            case PEER_HANDSHAKE:
            case TASK:
                // Messages after server handshake may come from server or peer.
                if (nonce.getSource() != SALTYRTC_ADDR_SERVER) {
                    switch (this.role) {
                        case Initiator:
                            if (!this.isResponderId(nonce.getSource())) {
                                throw new ValidationError("Initiator peer message does not come from " +
                                        "a valid responder address: " + nonce.getSource(),
                                        false);
                            }
                            break;
                        case Responder:
                            if (nonce.getSource() != SALTYRTC_ADDR_INITIATOR) {
                                throw new ValidationError("Responder peer message does not come from " +
                                        "intitiator (" + SALTYRTC_ADDR_INITIATOR + "), " +
                                        "but from " + nonce.getSource(),
                                        false);
                            }
                            break;
                    }
                }
                break;
            default:
                throw new ValidationError("Cannot validate message nonce in signaling state " +
                        this.getState());
        }
    }

    /**
     * Validate the receiver address in the nonce.
     */
    private void validateNonceDestination(SignalingChannelNonce nonce) throws ValidationError {
        Short expected = null;
        if (this.getState() == SignalingState.SERVER_HANDSHAKE) {
            switch (this.server.handshakeState) {
                // Before receiving the server auth message, the receiver byte is 0x00
                case NEW:
                case HELLO_SENT:
                    expected = SALTYRTC_ADDR_UNKNOWN;
                    break;
                // The server auth message contains the assigned receiver byte for the first time
                case AUTH_SENT:
                    if (this.role == SignalingRole.Initiator) {
                        expected = SALTYRTC_ADDR_INITIATOR;
                    } else { // Responder
                        if (!this.isResponderId(nonce.getDestination())) {
                            throw new ValidationError("Received message during server handshake " +
                                    "with invalid receiver address (" + nonce.getDestination() +
                                    " is not a valid responder id)");
                        }
                    }
                    break;
                // Afterwards, the receiver byte is the assigned address
                case DONE:
                    expected = this.address;
                    break;
            }
        } else if (this.getState() == SignalingState.PEER_HANDSHAKE ||
                   this.getState() == SignalingState.TASK) {
            expected = this.address;
        } else {
            throw new ValidationError("Cannot validate message nonce in signaling state " +
                this.getState());
        }

        if (expected != null && nonce.getDestination() != expected) {
            throw new ValidationError("Received message during server handshake with invalid " +
                    "receiver address (" + nonce.getDestination() + " != " + expected + ")");
        }
    }

    @Nullable
    abstract Peer getPeerWithId(short id) throws SignalingException;

    /**
     * Validate the CSN in the nonce.
     *
     * @param nonce The nonce from the incoming message.
     */
    private void validateNonceCsn(SignalingChannelNonce nonce) throws ValidationError, SignalingException {
        final Peer peer = this.getPeerWithId(nonce.getSource());
        if (peer == null) {
            // This can happen e.g. when a responder was dropped between validating
            // the source and the CSN.
            throw new ProtocolException("Could not find peer " + nonce.getSource());
        }

        // If this is the first message from that sender,
        // validate the overflow number and store the CSN.
        if (!peer.getCsnPair().hasTheirs()) {
            if (nonce.getOverflow() != 0) {
                throw new ValidationError("First message from " + peer.getName() + " must have set the overflow number to 0");
            }
            peer.getCsnPair().setTheirs(nonce.getCombinedSequence());

        // Otherwise, make sure that the CSN has been incremented
        } else {
            final long previous = peer.getCsnPair().getTheirs();
            final long current = nonce.getCombinedSequence();
            if (current < previous) {
                throw new ValidationError(peer.getName() + " CSN is lower than last time");
            } else if (current == previous) {
                throw new ValidationError(peer.getName() + " CSN hasn't been incremented");
            } else {
                peer.getCsnPair().setTheirs(current);
            }
        }
    }

    /**
     * Validate the cookie in the nonce.
     */
    private void validateNonceCookie(SignalingChannelNonce nonce) throws ValidationError, SignalingException {
        final Peer peer = this.getPeerWithId(nonce.getSource());
        if (peer != null && peer.getCookiePair().hasTheirs()) {
            if (!nonce.getCookie().equals(peer.getCookiePair().getTheirs())) {
                throw new ValidationError(peer.getName() + " cookie changed");
            }
        }
    }

    /**
     * Validate a repeated cookie in a p2p Auth message.
     * @param theirCookie The cookie bytes of the peer.
     * @throws ProtocolException Thrown if repeated cookie does not match our own cookie.
     */
    void validateRepeatedCookie(Peer peer, byte[] theirCookie) throws ProtocolException {
        // Verify the cookie
        final Cookie repeatedCookie = new Cookie(theirCookie);
        final Cookie ourCookie = peer.getCookiePair().getOurs();
        if (!repeatedCookie.equals(ourCookie)) {
            this.getLogger().debug("Peer repeated cookie: " + Arrays.toString(theirCookie));
            this.getLogger().debug("Our cookie: " + Arrays.toString(ourCookie.getBytes()));
            throw new ProtocolException("Peer repeated cookie does not match our cookie");
        }
    }

    /**
     * Encrypt data for the server during the handshake.
     */
    private Box encryptHandshakeDataForServer(
        @NonNull byte[] payload,
        @NonNull byte[] nonce
    ) throws CryptoFailedException {
        final SharedKeyStore sks = this.server.getSessionSharedKey();
        assert sks != null;
        return sks.encrypt(payload, nonce);
    }

    /**
     * Encrypt data for the specified peer during the handshake.
     */
    abstract Box encryptHandshakeDataForPeer(short receiver, String messageType,
                                             byte[] payload, byte[] nonce)
        throws CryptoFailedException, InvalidKeyException, ProtocolException;

    /**
     * Send data through the signaling channel.
     *
     * The message needs to be passed in too, because encryption after handshake is done in the
     * task.
     */
    synchronized void send(@NonNull byte[] payload, @NonNull Message msg) throws ConnectionException, SignalingException {
        // Verify connection state
        final SignalingState state = this.getState();
        if (state != SignalingState.TASK &&
                state != SignalingState.SERVER_HANDSHAKE &&
                state != SignalingState.PEER_HANDSHAKE) {
            this.getLogger().error("Trying to send message, but connection state is " + this.getState());
            throw new ConnectionException("SaltyRTC instance is not connected");
        }

        // Send data...
        if (!this.handoverState.getLocal()) {
            // ...through websocket...
            if (this.ws == null) {
                this.getLogger().error("Trying to send message, but websocket is null");
                throw new ConnectionException("SaltyRTC instance is not connected");
            }
            this.ws.sendBinary(payload);
        } else {
            // ...or via task.
            // Note: By sending a message through the task, the packet with the already sent CSN is dropped.
            // That's not a problem though, as the CSN will never be used again after handover.
            this.task.sendSignalingMessage(msg.toBytes());
        }
    }


    /**
     * Send an application message through the signaling channel.
     *
     * This function should only be called in TASK state.
     */
    public void sendApplication(Application msg) throws ConnectionException {
        try {
            this.sendPostClientHandshakeMessage(msg, "application");
        } catch (SignalingException e) {
            e.printStackTrace();
            Signaling.this.sendClose(e.getCloseCode());
            Signaling.this.resetConnection(CloseCode.CLOSING_NORMAL);
        }
    }

    /**
     * Send a task message through the signaling channel.
     */
    public void sendTaskMessage(TaskMessage msg) throws SignalingException, ConnectionException {
        this.sendPostClientHandshakeMessage(msg, "task");
    }

    /**
     * Send messages after the client to client handshake has been completed.
     *
     * @throws SignalingException if client to client handshake has not been completed.
     */
    private void sendPostClientHandshakeMessage(Message msg, String name)
            throws SignalingException, ConnectionException {

        // Make sure the c2c handshake has been completed
        switch (this.getState()) {
            case TASK:
                break;
            case CLOSING:
            case CLOSED:
                throw new ConnectionException(
                    "Cannot send " + name + " message, signaling state is " + this.getState());
            default:
                throw new ProtocolException(
                    "Cannot send " + name + " message in " + this.getState() + " state");
        }

        // Make sure the message type is valid
        if (!(msg instanceof Application || msg instanceof TaskMessage)) {
            throw new ProtocolException("Message type must be Application or TaskMessage");
        }

        // Get peer
        final Peer receiver = this.getPeer();
        if (receiver == null) {
            throw new SignalingException(CloseCode.INTERNAL_ERROR, "No peer address could be found");
        }

        // Send message
        this.getLogger().debug("Sending " + name + " message");
        if (this.handoverState.getLocal()) {
            this.task.sendSignalingMessage(msg.toBytes());
        } else {
            final byte[] packet = this.buildPacket(msg, receiver);
            this.send(packet, msg);
        }
    }

    private void handleClose(Close msg) {
        final Integer closeCode = msg.getReason();
        this.getLogger().warn("Received close message. Reason: " + CloseCode.explain(closeCode));

        // Notify the task
        this.task.close(closeCode);

        // Reset signaling
        this.resetConnection(CloseCode.GOING_AWAY);
    }

    private void handleApplication(Application msg) {
        this.salty.events.applicationData.notifyHandlers(new ApplicationDataEvent(msg.getData()));
    }

    /**
     * Handle the case where sending a message to the specified receiver failed.
     */
    abstract void handleSendError(short receiver) throws SignalingException;

    /**
     * Handle incoming send-error messages.
     */
    void handleSendError(SendError msg) throws SignalingException {
        // Get the message id from the SendError message
        final byte[] id = msg.getId();
        final String idString = NaCl.asHex(id);

        // Determine the sender and receiver of the message
        final ByteBuffer buf = ByteBuffer.wrap(id);
        final short source = UnsignedHelper.readUnsignedByte(buf.get());
        final short destination = UnsignedHelper.readUnsignedByte(buf.get());

        // Validate source
        if (source != this.address) {
            throw new ProtocolException("Received send-error message for a message not sent by us!");
        }

        // Log info about message
        final Message message = this.history.find(id);
        if (message != null) {
            this.getLogger().warn("SendError: Could not send " + message.getType() + " message " + idString);
        } else {
            this.getLogger().warn("SendError: Could not send unknown message: " + idString);
        }

        this.handleSendError(destination);
    }

    /**
     * Handle incoming disconnected messages.
     */
    void handleDisconnected(Disconnected msg) throws SignalingException {
        // Get the peer id from the Disconnected message
        final int id = msg.getId();

        switch (this.getRole()) {
            case Initiator:
                // An initiator who receives a 'disconnected' message SHALL validate
                // that the id field contains a valid responder address (0x02..0xff).
                if (id < 0x02 || id > 0xff) {
                    throw new ProtocolException("Received 'disconnected' message from server "
                        + "with invalid responder id: " + id);
                }
                break;
            case Responder:
                // A responder who receives a 'disconnected' message SHALL validate
                // that the id field contains a valid initiator address (0x01).
                if (id != 0x01) {
                    throw new ProtocolException("Received 'disconnected' message from server "
                        + "with invalid initiator id: " + id);
                }
                break;
        }

        this.getLogger().debug("Peer with id " + id + " disconnected from server");

        // A receiving client MUST notify the user application about the
        // incoming 'disconnected' message, along with the id field.
        this.salty.events.peerDisconnected.notifyHandlers(
            new PeerDisconnectedEvent((short) id)
        );
    }

    /**
     * Helper method. Return the peer session shared key.
     *
     * @throws InvalidStateException If the peer or the shared keystore are not set
     */
    private SharedKeyStore getPeerSessionSharedKey() throws InvalidStateException {
        final Peer peer = this.getPeer();
        if (peer == null) {
            throw new InvalidStateException("Peer is null");
        }
        final SharedKeyStore sks = peer.getSessionSharedKey();
        if (sks == null) {
            throw new InvalidStateException("Peer session shared key is null");
        }
        return sks;
    }

    /**
     * Encrypt data for the peer using the session key and the specified nonce.
     *
     * This method should primarily be used by tasks.
     */
    public Box encryptForPeer(@NonNull byte[] data, @NonNull byte[] nonce) throws CryptoFailedException {
        try {
            return this.getPeerSessionSharedKey().encrypt(data, nonce);
        } catch (InvalidStateException e) {
            // If that happens, something went massively wrong.
            e.printStackTrace();
            if (this.getState() == SignalingState.TASK) {
                this.sendClose(CloseCode.INTERNAL_ERROR);
            }
            // Close connection
            this.resetConnection(CloseCode.INTERNAL_ERROR);
            return null;
        }
    }

    /**
     * Decrypt data from the peer.
     */
    public byte[] decryptFromPeer(Box box) throws CryptoFailedException {
        try {
            return this.getPeerSessionSharedKey().decrypt(box);
        } catch (InvalidStateException e) {
            // If that happens, something went massively wrong.
            e.printStackTrace();
            if (this.getState() == SignalingState.TASK) {
                this.sendClose(CloseCode.INTERNAL_ERROR);
            }
            // Close connection
            this.resetConnection(CloseCode.INTERNAL_ERROR);
            return null;
        }
    }
}

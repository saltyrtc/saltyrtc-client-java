/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import com.neilalexander.jnacl.NaCl;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.events.SignalingChannelChangedEvent;
import org.saltyrtc.client.events.SignalingStateChangedEvent;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InternalException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ArrayHelper;
import org.saltyrtc.client.helpers.MessageHistory;
import org.saltyrtc.client.helpers.MessageReader;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.c2c.Close;
import org.saltyrtc.client.messages.s2c.ClientAuth;
import org.saltyrtc.client.messages.s2c.InitiatorServerAuth;
import org.saltyrtc.client.messages.s2c.ResponderServerAuth;
import org.saltyrtc.client.messages.s2c.SendError;
import org.saltyrtc.client.messages.s2c.ServerHello;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.CombinedSequencePair;
import org.saltyrtc.client.nonce.DataChannelNonce;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tasks.Task;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class Signaling implements SignalingInterface {

    protected final static String SALTYRTC_SUBPROTOCOL = "v0.saltyrtc.org";
    protected final static short SALTYRTC_WS_CONNECT_TIMEOUT = 2000;
    protected final static long SALTYRTC_WS_PING_INTERVAL = 20000;
    protected final static int SALTYRTC_WS_CLOSE_LINGER = 1000;
    protected final static short SALTYRTC_ADDR_UNKNOWN = 0x00;
    protected final static short SALTYRTC_ADDR_SERVER = 0x00;
    protected final static short SALTYRTC_ADDR_INITIATOR = 0x01;

    // Logger
    protected abstract Logger getLogger();

    // WebSocket
    protected final String host;
    protected final int port;
    protected final String protocol = "wss";
    protected final SSLContext sslContext;
    protected WebSocket ws;

    // Connection state
    protected SignalingState state = SignalingState.NEW;
    protected SignalingChannel channel = SignalingChannel.WEBSOCKET;
    protected ServerHandshakeState serverHandshakeState = ServerHandshakeState.NEW;

    // Reference to main class
    protected final SaltyRTC salty;

    // Keys
    protected byte[] serverKey;
    protected KeyStore sessionKey;
    @NonNull
    protected final KeyStore permanentKey;
    @Nullable
    protected AuthToken authToken;
    @Nullable
    protected byte[] peerTrustedKey = null;

    // Signaling
    protected SignalingRole role;
    protected short address = SALTYRTC_ADDR_UNKNOWN;
    protected Cookie cookie;
    protected Cookie serverCookie;
    protected CombinedSequencePair serverCsn = new CombinedSequencePair();

    // Tasks
    final protected Task[] tasks;
    protected Map<String, Map<Object, Object>> tasksData;
    protected Task task;

    // Message history
    protected final MessageHistory history = new MessageHistory(10);

    public Signaling(SaltyRTC salty, String host, int port,
                     KeyStore permanentKey, SSLContext sslContext,
                     Task[] tasks) {
        this.salty = salty;
        this.host = host;
        this.port = port;
        this.permanentKey = permanentKey;
        this.sslContext = sslContext;
        this.tasks = tasks;
        this.tasksData = new HashMap<>();
        for (Task task : this.tasks) {
            this.tasksData.put(task.getName(), task.getData());
        }
    }

    public Signaling(SaltyRTC salty, String host, int port,
                     KeyStore permanentKey, SSLContext sslContext,
                     byte[] peerTrustedKey,
                     Task[] tasks) {
        this(salty, host, port, permanentKey, sslContext, tasks);
        this.peerTrustedKey = peerTrustedKey;
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
    public boolean hasTrustedKey() {
        return this.peerTrustedKey != null;
    }

    @NonNull
    public SignalingState getState() {
        return this.state;
    }

    protected void setState(SignalingState newState) {
        if (this.state != newState) {
            this.state = newState;
            this.salty.events.signalingStateChanged.notifyHandlers(
                    new SignalingStateChangedEvent(newState));
        }
    }

    public SignalingChannel getChannel() {
        return this.channel;
    }

    protected void setChannel(SignalingChannel newChannel) {
        if (this.channel != newChannel) {
            this.channel = newChannel;
            this.salty.events.signalingChannelChanged.notifyHandlers(
                    new SignalingChannelChangedEvent(newChannel));
        }
    }

    /**
     * Connect asynchronously to the SaltyRTC server.
     *
     * To get notified when the connection is up and running, subscribe to the `ConnectedEvent`.
     */
    public void connect() throws ConnectionException {
        this.getLogger().info("Connecting to SaltyRTC server at "
                + Signaling.this.host + ":" + Signaling.this.port + "...");
        this.resetConnection(CloseCode.CLOSING_NORMAL);
        try {
            this.initWebsocket();
        } catch (IOException e) {
            throw new ConnectionException("Connecting to WebSocket failed.", e);
        }
        this.connectWebsocket();
    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This operation is asynchronous, once the connection is closed, the
     * `SignalingStateChangedEvent` will be emitted.
     */
    protected void disconnect(int reason) {
        this.setState(SignalingState.CLOSING);

        // Close websocket instance
        if (this.ws != null) {
            this.getLogger().debug("Disconnecting WebSocket (close code " + reason + ")");
            this.ws.disconnect(reason);
            this.ws = null;
        }
    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This operation is asynchronous, once the connection is closed, the
     * `SignalingStateChangedEvent` will be emitted.
     *
     * See `this.resetConnection(int)`
     */
    public void disconnect() {
        this.disconnect(CloseCode.CLOSING_NORMAL);
    }

    /**
     * Reset the connection.
     */
    private void resetConnection(int reason) {
        // Unregister listeners
        if (this.ws != null) {
            this.ws.clearListeners();
        }

        // Disconnect
        this.disconnect(reason);

        // Reset
        this.setChannel(SignalingChannel.WEBSOCKET);
        this.serverHandshakeState = ServerHandshakeState.NEW;
        this.serverCsn = new CombinedSequencePair();
        this.setState(SignalingState.NEW);
        this.getLogger().debug("Connection reset");
    }

    /**
     * Return the WebSocket path.
     */
    protected abstract String getWebsocketPath();

    /**
     * Initialize the WebSocket including TLS configuration.
     *
     * @throws IOException if setting up websocket fails
     */
    private void initWebsocket() throws IOException {
        // Build connection URL
        final String baseUrl = this.protocol + "://" + this.host + ":" + this.port + "/";
        final URI uri = URI.create(baseUrl + this.getWebsocketPath());
        this.getLogger().debug("Initialize WebSocket connection to " + uri);

        WebSocketAdapter listener = new WebSocketAdapter() {
            @Override
            public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                synchronized (this) {
                    getLogger().info("WebSocket connection open");
                    setState(SignalingState.SERVER_HANDSHAKE);
                }
            }

            @Override
            public void onConnectError(WebSocket websocket, WebSocketException ex) throws Exception {
                getLogger().error("Could not connect to websocket: " + ex.getMessage());
                setState(SignalingState.ERROR);
            }

            @Override
            public void onTextMessage(WebSocket websocket, String text) throws Exception {
                getLogger().debug("New string message: " + text);
                getLogger().error("Protocol error: Received string message, but only binary messages are valid.");
                Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
            }

            @Override
            public synchronized void onBinaryMessage(WebSocket websocket, byte[] binary) {
                getLogger().debug("New binary message (" + binary.length + " bytes)");
                try {
                    // Parse buffer
                    final Box box = new Box(ByteBuffer.wrap(binary), SignalingChannelNonce.TOTAL_LENGTH);

                    // Parse and validate nonce
                    final SignalingChannelNonce nonce = new SignalingChannelNonce(ByteBuffer.wrap(box.getNonce()));
                    validateSignalingNonce(nonce);

                    // Dispatch message
                    switch (Signaling.this.getState()) {
                        case SERVER_HANDSHAKE:
                            Signaling.this.onServerHandshakeMessage(box, nonce);
                            break;
                        case PEER_HANDSHAKE:
                            Signaling.this.onPeerHandshakeMessage(box, nonce);
                            break;
                        case OPEN:
                            Signaling.this.onPeerMessage(box, nonce);
                            break;
                        default:
                            getLogger().warn("Received message in " + Signaling.this.getState().name() +
                                    " signaling state. Ignoring.");
                    }
                // TODO: The following errors could also be handled using `handleCallbackError` on the websocket.
                } catch (ValidationError | SerializationError e) {
                    getLogger().error("Protocol error: Invalid incoming message: " + e.getMessage());
                    e.printStackTrace();
                    Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                } catch (ProtocolException e) {
                    getLogger().error("Protocol error: " + e.getMessage());
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
                }
            }

            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                       WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                // Log details to debug log
                final String closer = closedByServer ? "server" : "client";
                final WebSocketFrame frame = closedByServer ? serverCloseFrame : clientCloseFrame;
                final int closeCode = frame.getCloseCode();
                String closeReason = frame.getCloseReason();
                if (closeReason == null) {
                    closeReason = CloseCode.explain(closeCode);
                }
                getLogger().debug("WebSocket connection closed by " + closer +
                                  " with code " + closeCode + ": " + closeReason);

                // Log some of the codes on higher log levels too
                if (closedByServer) {
                    switch (closeCode) {
                        case CloseCode.CLOSING_NORMAL:
                            getLogger().info("WebSocket closed");
                            break;
                        case CloseCode.GOING_AWAY:
                            getLogger().error("Server is being shut down");
                            break;
                        case CloseCode.NO_SHARED_SUBPROTOCOL:
                            getLogger().error("No shared sub-protocol could be found");
                            break;
                        case CloseCode.PATH_FULL:
                            getLogger().error("Path full (no free responder byte)");
                            break;
                        case CloseCode.PROTOCOL_ERROR:
                            break;
                        case CloseCode.INTERNAL_ERROR:
                            getLogger().error("Internal server error");
                            break;
                        case CloseCode.DROPPED_BY_INITIATOR:
                            getLogger().warn("Dropped by initiator");
                            break;
                        case CloseCode.INITIATOR_COULD_NOT_DECRYPT:
                            getLogger().error("Initiator could not decrypt message");
                            break;
                        case CloseCode.NO_SHARED_TASK:
                            getLogger().error("No shared task was found");
                            break;
                    }
                }
                if (closeCode != CloseCode.HANDOVER && Signaling.this.state != SignalingState.NEW) {
                    setState(SignalingState.CLOSED);
                }
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                getLogger().error("A WebSocket connect error occured: " + cause.getMessage(), cause);
                // TODO: Do we need to handle these?
            }

            @Override
            public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
                getLogger().error("WebSocket callback error: " + cause);
                cause.printStackTrace();
                Signaling.this.resetConnection(CloseCode.INTERNAL_ERROR);
            }
        };

        // Create WebSocket client instance
        this.ws = new WebSocketFactory()
                .setConnectionTimeout(SALTYRTC_WS_CONNECT_TIMEOUT)
                .setSSLContext(this.sslContext)
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
        this.ws.connectAsynchronously();
    }

    /**
     * Build an optionally encrypted msgpacked message.
     *
     * @param msg The `Message` to be sent.
     * @param receiver The receiver byte.
     * @param encrypt Whether to encrypt the message.
     * @return Encrypted msgpacked bytes, ready to send.
     */
    protected byte[] buildPacket(Message msg, short receiver, boolean encrypt) throws ProtocolException {
        // Choose proper combined sequence number
        final CombinedSequence csn = this.getNextCsn(receiver);

        // Create nonce
        final SignalingChannelNonce nonce = new SignalingChannelNonce(
                this.cookie.getBytes(), this.address, receiver,
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
            } else if (receiver == SALTYRTC_ADDR_INITIATOR || this.isResponderId(receiver)) {
                // TODO: Do we re-use the same cookie everywhere?
                box = this.encryptForPeer(receiver, msg.getType(), payload, nonceBytes);
            } else {
                throw new ProtocolException("Bad receiver byte: " + receiver);
            }
        } catch (CryptoFailedException | InvalidKeyException e) {
            throw new ProtocolException("Encrypting failed: " + e.getMessage(), e);
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
    protected byte[] buildPacket(Message msg, short receiver) throws ProtocolException {
        return this.buildPacket(msg, receiver, true);
    }

    /**
     * Return the address of the peer.
     *
     * May return null if peer is not yet set.
     */
    @Nullable
    protected abstract Short getPeerAddress();

    /**
     * Return the cookie of the peer.
     *
     * May return null if peer is not yet set or if cookie is not yet stored.
     */
    @Nullable
    public abstract Cookie getPeerCookie();

    /**
     * Return the session key of the peer.
     *
     * May return null if peer is not yet set.
     */
    @Nullable
    protected abstract byte[] getPeerSessionKey();

    /**
     * Return the own cookie.
     */
    @Nullable
    public Cookie getCookie() {
        return this.cookie;
    }

    /**
     * Decrypt the peer message using the session key.
     */
    private Message decryptPeerMessage(Box box)
            throws CryptoFailedException, InvalidKeyException, ValidationError, SerializationError {
        final byte[] decrypted = this.sessionKey.decrypt(box, this.getPeerSessionKey());
        return MessageReader.read(decrypted);
    }

    /**
     * Decrypt the server message using the permanent key.
     */
    private Message decryptServerMessage(Box box)
            throws CryptoFailedException, InvalidKeyException, ValidationError, SerializationError {
        final byte[] decrypted = this.permanentKey.decrypt(box, this.serverKey);
        return MessageReader.read(decrypted);
    }

    /**
     * Message received during server handshake.
     *
     * @param box The box containing raw nonce and payload bytes.
     */
    private void onServerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ValidationError, SerializationError, ProtocolException,
            InternalException, ConnectionException {
        // Decrypt if necessary
        final byte[] payload;
        if (this.serverHandshakeState == ServerHandshakeState.NEW) {
            // The very first message is unencrypted
            payload = box.getData();
        } else {
            // Later, they're encrypted with our permanent key and the server key
            try {
                payload = this.permanentKey.decrypt(box, this.serverKey);
            } catch (CryptoFailedException | InvalidKeyException e) {
                throw new ProtocolException("Could not decrypt server message", e);
            }
        }

        // Handle message depending on state
        Message msg = MessageReader.read(payload);
        switch (this.serverHandshakeState) {
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
                throw new InternalException("Received server handshake message even though " +
                                                  "server handshake state is set to DONE");
            default:
                throw new InternalException("Unknown server handshake state");
        }

        // Check if we're done yet
        if (this.serverHandshakeState == ServerHandshakeState.DONE) {
            this.setState(SignalingState.PEER_HANDSHAKE);
            this.getLogger().info("Server handshake done");
            this.initPeerHandshake();
        }
    }

    /**
     * Message received during peer handshake.
     */
    protected abstract void onPeerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ProtocolException, ValidationError, SerializationError,
            InternalException, ConnectionException;

    /**
     * Message received from peer *after* the handshake is done.
     *
     * Note that although this method is called `onPeerMessage`, it's still
     * possible that server messages arrive, e.g. a `send-error` message.
     */
    private void onPeerMessage(Box box, SignalingChannelNonce nonce) {
        this.getLogger().debug("Message received");

        final Message message;

        // Process server messages
        if (nonce.getSource() == SALTYRTC_ADDR_SERVER) {
            try {
                message = this.decryptServerMessage(box);
            } catch (CryptoFailedException e) {
                this.getLogger().error("Could not decrypt incoming message from server", e);
                return;
            } catch (InvalidKeyException e) {
                this.getLogger().error("InvalidKeyException while processing incoming message from server", e);
                return;
            } catch (ValidationError | SerializationError e) {
                this.getLogger().error("Received invalid message from server", e);
                return;
            }

            if (message instanceof SendError) {
                this.handleSendError((SendError) message);
            } else {
                this.getLogger().error("Invalid server message type: " + message.getType());
            }

        // Process peer messages
        } else {
            try {
                message = this.decryptPeerMessage(box);
            } catch (CryptoFailedException e) {
                this.getLogger().error("Could not decrypt incoming message from peer " + nonce.getSource(), e);
                return;
            } catch (InvalidKeyException e) {
                this.getLogger().error("InvalidKeyException while processing incoming message from peer", e);
                return;
            } catch (ValidationError | SerializationError e) {
                this.getLogger().error("Received invalid message from peer", e);
                return;
            }

            if (message instanceof Close) {
                this.getLogger().debug("Received close");
                handleClose((Close) message);
            } else {
                this.getLogger().error("Received message with invalid type from peer");
            }
        }
    }

    /**
     * Handle an incoming server-hello message.
     */
    private void handleServerHello(ServerHello msg, SignalingChannelNonce nonce) {
        // Store server public key
        this.serverKey = msg.getKey();

        // Generate cookie
        Cookie ourCookie;
        final Cookie serverCookie = nonce.getCookie();
        do {
            ourCookie = new Cookie();
        } while (ourCookie.equals(serverCookie));

        // Store cookies
        this.cookie = ourCookie;
        this.serverCookie = serverCookie;
    }

    /**
     * Send a client-hello message to the server.
     */
    protected abstract void sendClientHello() throws ProtocolException, ConnectionException;

    /**
     * Send a client-auth message to the server.
     */
    private void sendClientAuth() throws ProtocolException, ConnectionException {
        final List<String> subprotocols = Arrays.asList(Signaling.SALTYRTC_SUBPROTOCOL);
        final ClientAuth msg = new ClientAuth(this.serverCookie.getBytes(), subprotocols);
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_SERVER);
        this.getLogger().debug("Sending client-auth");
        this.send(packet, msg);
        this.serverHandshakeState = ServerHandshakeState.AUTH_SENT;
    }

    /**
     * Handle an incoming server-auth message.
     *
     * Note that the message has not yet been casted to the correct subclass.
     * That needs to be done (differently) in the initiator and
     * responder signaling subclasses.
     */
    protected abstract void handleServerAuth(Message baseMsg, SignalingChannelNonce nonce) throws
            ProtocolException, ConnectionException;

    /**
     * Initialize the peer handshake.
     */
    protected abstract void initPeerHandshake() throws ProtocolException, ConnectionException;

    /**
     * Choose proper combined sequence number
     */
    protected abstract CombinedSequence getNextCsn(short receiver) throws ProtocolException;

    /**
     * Return `true` if receiver byte is a valid responder id (in the range 0x02-0xff).
     */
    protected boolean isResponderId(short receiver) {
        return receiver >= 0x02 && receiver <= 0xff;
    }

    /**
     * Validate the signaling nonce.
     *
     * See https://github.com/saltyrtc/saltyrtc-meta/issues/41
     */
    private void validateSignalingNonce(SignalingChannelNonce nonce) throws ValidationError {
        this.validateSignalingNonceSender(nonce);
        this.validateSignalingNonceReceiver(nonce);
        this.validateSignalingNonceCsn(nonce);
        this.validateSignalingNonceCookie(nonce);
    }

    /**
     * Validate the sender address in the nonce.
     */
    private void validateSignalingNonceSender(SignalingChannelNonce nonce) throws ValidationError {
        switch (this.getState()) {
            case SERVER_HANDSHAKE:
                // Messages during server handshake must come from the server.
                if (nonce.getSource() != SALTYRTC_ADDR_SERVER) {
                    throw new ValidationError("Received message during server handshake " +
                            "with invalid sender address (" +
                            nonce.getSource() + " != " + SALTYRTC_ADDR_SERVER + ")");
                }
                break;
            case PEER_HANDSHAKE:
                // Messages during peer handshake may come from server or peer.
                if (nonce.getSource() != SALTYRTC_ADDR_SERVER) {
                    switch (this.role) {
                        case Initiator:
                            if (!this.isResponderId(nonce.getSource())) {
                                throw new ValidationError("Initiator peer message does not come from " +
                                        "a valid responder address: " + nonce.getSource());
                            }
                            break;
                        case Responder:
                            if (nonce.getSource() != SALTYRTC_ADDR_INITIATOR) {
                                throw new ValidationError("Responder peer message does not come from " +
                                        "intitiator (" + SALTYRTC_ADDR_INITIATOR + "), " +
                                        "but from " + nonce.getSource());
                            }
                            break;
                    }
                }
                break;
            case OPEN:
                // Messages after the handshake must come from the peer.
                if (nonce.getSource() != this.getPeerAddress()) {
                    // TODO: Ignore instead of throw?
                    throw new ValidationError("Received message with invalid sender address (" +
                            nonce.getSource() + " != " + this.getPeerAddress() + ")");
                }
                break;
            default:
                throw new ValidationError("Cannot validate message nonce with signaling state " +
                        this.getState());
        }
    }

    /**
     * Validate the receiver address in the nonce.
     */
    private void validateSignalingNonceReceiver(SignalingChannelNonce nonce) throws ValidationError {
        Short expected = null;
        if (this.getState() == SignalingState.SERVER_HANDSHAKE) {
            switch (this.serverHandshakeState) {
                // Before receiving the server auth-message, the receiver byte is 0x00
                case NEW:
                case HELLO_SENT:
                    expected = SALTYRTC_ADDR_UNKNOWN;
                    break;
                // The server auth-message contains the assigned receiver byte for the first time
                case AUTH_SENT:
                    if (this.role == SignalingRole.Initiator) {
                        expected = SALTYRTC_ADDR_INITIATOR;
                    } else if (this.role == SignalingRole.Responder) {
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
        }

        if (expected != null && nonce.getDestination() != expected) {
            throw new ValidationError("Received message during server handshake with invalid " +
                    "receiver address (" + nonce.getDestination() + " != " + expected + ")");
        }
    }

    /**
     * Validate the CSN in the nonce.
     *
     * @param nonce The nonce from the incoming message.
     */
    private void validateSignalingNonceCsn(SignalingChannelNonce nonce) throws ValidationError {
        if (nonce.getSource() == SALTYRTC_ADDR_SERVER) {
            this.validateSignalingNonceCsn(nonce, this.serverCsn, "server");
        } else {
            this.validateSignalingNoncePeerCsn(nonce);
        }
    }

    /**
     * Validate the CSN in the nonce.
     *
     * If it's the first message from that sender, validate the overflow number and store the CSN.
     *
     * Otherwise, make sure that the CSN has been increased.
     *
     * @param nonce The nonce from the incoming message.
     * @param csnPair The CSN pair for the message sender.
     * @param peerName Name of the peer (e.g. "server" or "initiator") used in error messages.
     */
    protected void validateSignalingNonceCsn(SignalingChannelNonce nonce, CombinedSequencePair csnPair, String peerName)
            throws ValidationError {
        // If this is the first message from the initiator, validate the overflow number
        // and store it for future reference.
        if (!csnPair.hasTheirs()) {
            if (nonce.getOverflow() != 0) {
                throw new ValidationError("First message from " + peerName + " must have set the overflow number to 0");
            }
            csnPair.setTheirs(nonce.getCombinedSequence());

        // Otherwise, make sure that the CSN has been incremented
        } else {
            final long previous = csnPair.getTheirs();
            final long current = nonce.getCombinedSequence();
            if (current < previous) {
                throw new ValidationError(peerName + " CSN is lower than last time");
            } else if (current == previous) {
                throw new ValidationError(peerName + " CSN hasn't been incremented");
            } else {
                csnPair.setTheirs(current);
            }
        }
    }

    /**
     * Validate the peer CSN in the nonce.
     */
    abstract void validateSignalingNoncePeerCsn(SignalingChannelNonce nonce) throws ValidationError;

    /**
     * Validate the cookie in the nonce.
     */
    private void validateSignalingNonceCookie(SignalingChannelNonce nonce) throws ValidationError {
        if (nonce.getSource() == SALTYRTC_ADDR_SERVER) {
            if (this.serverCookie != null) { // Server cookie might not yet have been set
                if (!nonce.getCookie().equals(this.serverCookie)) {
                    throw new ValidationError("Server cookie changed");
                }
            }
        } else {
            final Cookie cookie = this.getPeerCookie();
            if (cookie != null) { // Peer cookie might not yet have been set
                if (!nonce.getCookie().equals(cookie)) {
                    throw new ValidationError("Peer cookie changed");
                }
            }
        }
    }

    /**
     * Validate a repeated cookie in a p2p Auth message.
     * @param theirCookie The cookie bytes of the peer.
     * @throws ProtocolException Thrown if repeated cookie does not match our own cookie.
     */
    protected void validateRepeatedCookie(byte[] theirCookie) throws ProtocolException {
        // Verify the cookie
        final Cookie repeatedCookie = new Cookie(theirCookie);
        if (!repeatedCookie.equals(this.cookie)) {
            this.getLogger().debug("Peer repeated cookie: " + Arrays.toString(theirCookie));
            this.getLogger().debug("Our cookie: " + Arrays.toString(this.cookie.getBytes()));
            throw new ProtocolException("Peer repeated cookie does not match our cookie");
        }
    }

    /**
     * Encrypt data for the server.
     */
    private Box encryptForServer(byte[] payload, byte[] nonce)
            throws CryptoFailedException, InvalidKeyException {
        return this.permanentKey.encrypt(payload, nonce, this.serverKey);
    }

    /**
     * Encrypt data for the specified peer.
     */
    protected abstract Box encryptForPeer(short receiver, String messageType, byte[] payload, byte[] nonce)
        throws CryptoFailedException, InvalidKeyException, ProtocolException;

    /**
     * Send binary data through the signaling channel.
     */
    private void send(byte[] payload) throws ConnectionException, ProtocolException {
        // Verify connection state
        final SignalingState state = this.getState();
        if (state != SignalingState.OPEN &&
                state != SignalingState.SERVER_HANDSHAKE &&
                state != SignalingState.PEER_HANDSHAKE) {
            this.getLogger().error("Trying to send data message, but connection state is " + this.getState());
            throw new ConnectionException("SaltyRTC instance is not connected");
        }

        // Send data
        switch (this.getChannel()) {
            case WEBSOCKET:
                this.ws.sendBinary(payload);
                break;
            case TASK:
                // TODO: Implement via task
                throw new NotImplementedException();
            default:
                throw new ProtocolException("Unknown or invalid signaling channel: " + this.channel);
        }
    }

    /**
     * Like `send(byte[] payload)`, but additionally store the sent message in the message history.
     *
     * This allows the message to be recognized in the case of a send error.
     */
    protected void send(byte[] payload, Message message) throws ConnectionException, ProtocolException {
        // Send data
        this.send(payload);

        // Store sent message in history
        this.history.store(message, payload);
    }

    /**
     * Encrypt arbitrary data for the peer using the session keys.
     *
     * TODO: Rewrite this after tasks have been implemented
     *
     * @param data Plain data bytes.
     * @param csn The `CombinedSequenceNumber` instance to use.
     * @return Encrypted box.
     */
    public @Nullable Box encryptData(@NonNull byte[] data,
                                     @NonNull CombinedSequence csn)
            throws CryptoFailedException, InvalidKeyException {
        // Create nonce
        final DataChannelNonce nonce = new DataChannelNonce(
                this.cookie.getBytes(),
                123, // TODO: Get actual dc id (https://bugs.chromium.org/p/webrtc/issues/detail?id=6106)
                csn.getOverflow(), csn.getSequenceNumber());

        // Encrypt
        return this.sessionKey.encrypt(data, nonce.toBytes(), this.getPeerSessionKey());
    }

    /**
     * Decrypt data from the peer using the session keys.
     * @param box Encrypted box.
     * @return Decrypted bytes.
     */
    public @NonNull byte[] decryptData(@NonNull Box box) throws CryptoFailedException, InvalidKeyException {
        // TODO: Do we need to verify the nonce?
        return this.sessionKey.decrypt(box, this.getPeerSessionKey());
    }

    private void handleClose(Close msg) {
        throw new UnsupportedOperationException("Close not yet implemented"); // TODO
    }

    private void handleSendError(SendError msg) {
        final byte[] hash = msg.getHash();
        final String hashPrefix = NaCl.asHex(hash).substring(0, 7);
        final Message message = this.history.find(hash);

        if (message != null) {
            this.getLogger().warn("SendError: Could not send " + message.getType() + " message " + hashPrefix);
            // TODO: Implement. See git history for old implementation.
        } else {
            this.getLogger().warn("SendError: " + NaCl.asHex(hash));
        }
    }
}

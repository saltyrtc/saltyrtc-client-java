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
import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.datachannel.SecureDataChannel;
import org.saltyrtc.client.events.DataEvent;
import org.saltyrtc.client.events.SendErrorEvent;
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
import org.saltyrtc.client.messages.Auth;
import org.saltyrtc.client.messages.ClientAuth;
import org.saltyrtc.client.messages.Data;
import org.saltyrtc.client.messages.InitiatorServerAuth;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.Restart;
import org.saltyrtc.client.messages.SendError;
import org.saltyrtc.client.messages.ServerHello;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.CombinedSequencePair;
import org.saltyrtc.client.nonce.DataChannelNonce;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;
import org.webrtc.DataChannel;
import org.webrtc.PeerConnection;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

public abstract class Signaling {

    protected final static String SALTYRTC_PROTOCOL = "saltyrtc-1.0";
    protected final static short SALTYRTC_WS_CONNECT_TIMEOUT = 2000;
    protected final static long SALTYRTC_WS_PING_INTERVAL = 20000;
    protected final static int SALTYRTC_WS_CLOSE_LINGER = 1000;
    protected final static String SALTYRTC_DC_LABEL = "saltyrtc-signaling";
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

    // WebRTC / ORTC
    protected DataChannel dc;

    // Connection state
    protected SignalingState state = SignalingState.NEW;
    protected SignalingChannel channel = SignalingChannel.WEBSOCKET;
    protected ServerHandshakeState serverHandshakeState = ServerHandshakeState.NEW;

    // Reference to main class
    protected final SaltyRTC salty;

    // Keys
    protected byte[] serverKey;
    protected final KeyStore permanentKey;
    protected KeyStore sessionKey;
    protected AuthToken authToken;

    // Signaling
    protected SignalingRole role;
    protected short address = SALTYRTC_ADDR_UNKNOWN;
    protected CookiePair serverCookiePair;
    protected CombinedSequencePair serverCsn = new CombinedSequencePair();

    // Message history
    protected final MessageHistory history = new MessageHistory(10);

    public Signaling(SaltyRTC salty, String host, int port,
                     KeyStore permanentKey, SSLContext sslContext) {
        this.salty = salty;
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
        getLogger().info("Connecting to SaltyRTC server at "
                + Signaling.this.host + ":" + Signaling.this.port + "...");
        resetConnection(CloseCode.CLOSING_NORMAL);
        try {
            initWebsocket();
        } catch (IOException e) {
            throw new ConnectionException("Connecting to WebSocket failed.", e);
        }
        connectWebsocket();
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
            getLogger().debug("Disconnecting WebSocket (close code " + reason + ")");
            this.ws.disconnect(reason);
            this.ws = null;
        }
        // Close datachannel instance
        if (this.dc != null) {
            getLogger().debug("Disconnecting data channel");
            //this.dc.close();
            // The line above will cause a deadlock as of 2016-07-12.
            // Instead, we'll let the GC handle this.
            this.dc.unregisterObserver();
            this.setState(SignalingState.CLOSING);
            this.dc = null;
            this.setState(SignalingState.CLOSED);
        }

    }

    /**
     * Disconnect from the SaltyRTC server.
     *
     * This operation is asynchronous, once the connection is closed, the
     * `SignalingStateChangedEvent` will be emitted.
     *
     * @see this.resetConnection(int)
     */
    public void disconnect() {
        this.disconnect(CloseCode.CLOSING_NORMAL);
    }

    /**
     * Reset the connection.
     */
    protected void resetConnection(int reason) {
        // Unregister listeners
        if (this.ws != null) {
            this.ws.clearListeners();
        }
        if (this.dc != null) {
            this.dc.unregisterObserver();
        }

        // Disconnect
        this.disconnect(reason);

        // Reset
        this.setChannel(SignalingChannel.WEBSOCKET);
        this.serverHandshakeState = ServerHandshakeState.NEW;
        this.serverCsn = new CombinedSequencePair();
        this.setState(SignalingState.NEW);
        getLogger().debug("Connection reset");
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
    protected void initWebsocket() throws IOException {
        // Build connection URL
        final String baseUrl = this.protocol + "://" + this.host + ":" + this.port + "/";
        final URI uri = URI.create(baseUrl + this.getWebsocketPath());
        getLogger().debug("Initialize WebSocket connection to " + uri);

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
                    Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                } catch (ProtocolException e) {
                    getLogger().error("Protocol error: " + e.getMessage());
                    Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                } catch (InternalException e) {
                    getLogger().error("Internal server error: " + e.getMessage());
                    Signaling.this.resetConnection(CloseCode.INTERNAL_ERROR);
                } catch (ConnectionException e) {
                    getLogger().error("Connection error: " + e.getMessage());
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
                .addProtocol(SALTYRTC_PROTOCOL)
                .addListener(listener);

    }

    /**
     * Connect asynchronously to WebSocket.
     */
    protected void connectWebsocket() {
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
        final byte[] cookie = this.serverCookiePair.getOurs().getBytes();
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
            } else if (receiver == SALTYRTC_ADDR_INITIATOR || isResponderId(receiver)) {
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
    protected abstract Short getPeerAddress();

    /**
     * Return the session key of the peer.
     *
     * May return null if peer is not yet set.
     */
    protected abstract byte[] getPeerSessionKey();

    /**
     * Decrypt the peer message using the session key.
     */
    protected Message decryptPeerMessage(Box box)
            throws CryptoFailedException, InvalidKeyException, ValidationError, SerializationError {
        final byte[] decrypted = this.sessionKey.decrypt(box, this.getPeerSessionKey());
        return MessageReader.read(decrypted);
    }

    /**
     * Decrypt the server message using the permanent key.
     */
    protected Message decryptServerMessage(Box box)
            throws CryptoFailedException, InvalidKeyException, ValidationError, SerializationError {
        final byte[] decrypted = this.permanentKey.decrypt(box, this.serverKey);
        return MessageReader.read(decrypted);
    }

    /**
     * Message received during server handshake.
     *
     * @param box The box containing raw nonce and payload bytes.
     */
    protected void onServerHandshakeMessage(Box box, SignalingChannelNonce nonce)
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
                    getLogger().debug("Received server-hello");
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
            getLogger().info("Server handshake done");
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
    protected void onPeerMessage(Box box, SignalingChannelNonce nonce) {
        getLogger().debug("Message received");

        final Message message;

        // Process server messages
        if (nonce.getSource() == SALTYRTC_ADDR_SERVER) {
            try {
                message = this.decryptServerMessage(box);
            } catch (CryptoFailedException e) {
                getLogger().error("Could not decrypt incoming message from server", e);
                return;
            } catch (InvalidKeyException e) {
                getLogger().error("InvalidKeyException while processing incoming message from server", e);
                return;
            } catch (ValidationError | SerializationError e) {
                getLogger().error("Received invalid message from server", e);
                return;
            }

            if (message instanceof SendError) {
                handleSendError((SendError) message);
            } else {
                getLogger().error("Invalid server message type: " + message.getType());
            }

        // Process peer messages
        } else {
            try {
                message = this.decryptPeerMessage(box);
            } catch (CryptoFailedException e) {
                getLogger().error("Could not decrypt incoming message from peer " + nonce.getSource(), e);
                return;
            } catch (InvalidKeyException e) {
                getLogger().error("InvalidKeyException while processing incoming message from peer", e);
                return;
            } catch (ValidationError | SerializationError e) {
                getLogger().error("Received invalid message from peer", e);
                return;
            }

            if (message instanceof Data) {
                getLogger().debug("Received data");
                // TODO: move this to handleData method for consistency
                salty.events.data.notifyHandlers(new DataEvent((Data) message));
            } else if (message instanceof Restart) {
                getLogger().debug("Received restart");
                handleRestart((Restart) message);
            } else {
                getLogger().error("Received message with invalid type from peer");
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
        this.serverCookiePair = new CookiePair(ourCookie, serverCookie);
    }

    /**
     * Send a client-hello message to the server.
     */
    protected abstract void sendClientHello() throws ProtocolException, ConnectionException;

    /**
     * Send a client-auth message to the server.
     */
    protected void sendClientAuth() throws ProtocolException, ConnectionException {
        final ClientAuth msg = new ClientAuth(this.serverCookiePair.getTheirs().getBytes());
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_SERVER);
        getLogger().debug("Sending client-auth");
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
    protected abstract void handleServerAuth(Message baseMsg, SignalingChannelNonce nonce) throws ProtocolException;

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
        validateSignalingNonceSender(nonce);
        validateSignalingNonceReceiver(nonce);
        validateSignalingNonceCsn(nonce);
        validateSignalingNonceCookie(nonce);
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
                        if (!isResponderId(nonce.getDestination())) {
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
            if (this.serverCookiePair != null) { // Server cookie pair might not yet have been initialized
                this.validateSignalingNonceCookie(nonce, this.serverCookiePair, "server");
            }
        } else {
            this.validateSignalingNoncePeerCookie(nonce);
        }
    }

    /**
     * Validate the cookie in the nonce.
     *
     * @param nonce The nonce from the incoming message.
     * @param cookiePair The cookie pair for the message sender.
     * @param peerName Name of the peer (e.g. "server" or "initiator") used in error messages
     */
    protected void validateSignalingNonceCookie(@NonNull SignalingChannelNonce nonce,
                                                @NonNull CookiePair cookiePair,
                                                @NonNull String peerName) throws ValidationError {
        if (nonce.getCookie().equals(cookiePair.getOurs())) {
            throw new ValidationError("Local and remote " + peerName + " cookies are equal");
        }
        if (!cookiePair.hasTheirs()) {
            cookiePair.setTheirs(nonce.getCookie());
        } else {
            if (!nonce.getCookie().equals(cookiePair.getTheirs())) {
                throw new ValidationError("Remote " + peerName + " cookie changed");
            }
        }
    }

    /**
     * Validate the peer cookie in the nonce.
     */
    abstract void validateSignalingNoncePeerCookie(SignalingChannelNonce nonce) throws ValidationError;

    /**
     * Validate a repeated cookie in an Auth message.
     * @param msg The Auth message.
     * @param msg The cookie pair to use.
     * @throws ProtocolException Thrown if repeated cookie does not match our own cookie.
     */
    protected void validateRepeatedCookie(Auth msg, CookiePair cookiePair) throws ProtocolException {
        // Verify the cookie
        final Cookie repeatedCookie = new Cookie(msg.getYourCookie());
        if (!repeatedCookie.equals(cookiePair.getOurs())) {
            getLogger().debug("Peer repeated cookie: " + Arrays.toString(msg.getYourCookie()));
            getLogger().debug("Our cookie: " + Arrays.toString(cookiePair.getOurs().getBytes()));
            throw new ProtocolException("Peer repeated cookie does not match our cookie");
        }
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

    /**
     * Send binary data through the signaling channel.
     */
    protected void send(byte[] payload) throws ConnectionException, ProtocolException {
        // Verify connection state
        final SignalingState state = this.getState();
        if (state != SignalingState.OPEN &&
                state != SignalingState.SERVER_HANDSHAKE &&
                state != SignalingState.PEER_HANDSHAKE) {
            getLogger().error("Trying to send data message, but connection state is " + this.getState());
            throw new ConnectionException("SaltyRTC instance is not connected");
        }

        // Send data
        switch (this.getChannel()) {
            case WEBSOCKET:
                this.ws.sendBinary(payload);
                break;
            case DATA_CHANNEL:
                final boolean success = this.dc.send(new DataChannel.Buffer(ByteBuffer.wrap(payload), true));
                // TODO: Return code indicates success. Handle.
                break;
            default:
                throw new ProtocolException("Unknown or invalid signaling channel: " + channel);
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
     * Send a data message to the peer through the signaling channel,
     * encrypted with the session key.
     *
     * If a ProtocolException occurs during sending, the connection will be reset.
     */
    public void sendSignalingData(Data data) throws ConnectionException {
        try {
            // Build packet
            final byte[] packet = this.buildPacket(data, this.getPeerAddress());
            // Send message
            getLogger().debug("Sending " + data.getDataType() + " data message through " + this.getChannel());
            this.send(packet, data);
        } catch (ProtocolException e) {
            this.resetConnection(CloseCode.PROTOCOL_ERROR);
        }
    }

    /**
     * Handover asynchronously from WebSocket to WebRTC data channel.
     *
     * To get notified when the connection is up and running,
     * subscribe to the `SignalingChannelChangedEvent`.
     */
    public void handover(final PeerConnection pc) {
        // Create new signaling DataChannel
        // TODO (https://github.com/saltyrtc/saltyrtc-meta/issues/3): Negotiate channel id
        getLogger().debug("Initiate handover");
        DataChannel.Init init = new DataChannel.Init();
        init.id = 0;
        init.negotiated = true;
        init.ordered = true;
        init.protocol = SALTYRTC_PROTOCOL;
        this.dc = pc.createDataChannel(SALTYRTC_DC_LABEL, init);
        this.dc.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                Signaling.this.getLogger().info("DataChannel: Buffered amount changed");
            }

            @Override
            public void onStateChange() {
                Signaling.this.getLogger().info("DataChannel: State changed to " + Signaling.this.dc.state());
                switch (Signaling.this.dc.state()) {
                    case OPEN:
                        Signaling.this.setChannel(SignalingChannel.DATA_CHANNEL);
                        Signaling.this.getLogger().info("Handover to data channel finished");

                        // Close the websocket with some delay.
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Hacks solution, maybe there's a better way?
                                    Thread.sleep(SALTYRTC_WS_CLOSE_LINGER);
                                } catch (InterruptedException e) {
                                    getLogger().warn("Websocket closing thread was interrupted early while lingering.");
                                }
                                Signaling.this.ws.sendClose(CloseCode.HANDOVER);
                            }
                        }, "close-websocket").start();

                        break;
                    case CLOSING:
                        Signaling.this.setState(SignalingState.CLOSING);
                        break;
                    case CLOSED:
                        Signaling.this.setState(SignalingState.CLOSED);
                        break;
                }
            }

            @Override
            public synchronized void onMessage(DataChannel.Buffer buffer) {
                final String type = buffer.binary ? "Binary" : "Text";
                getLogger().info("DataChannel: " + type + " message arrived ("
                        + buffer.data.remaining() + " bytes)");

                // We only support binary messages
                if (!buffer.binary) {
                    getLogger().warn("Received non-binary message through data channel. Ignoring.");
                    return;
                }

                final Box box = new Box(buffer.data, SignalingChannelNonce.TOTAL_LENGTH);
                final SignalingChannelNonce nonce = new SignalingChannelNonce(ByteBuffer.wrap(box.getNonce()));
                try {
                    validateSignalingNonce(nonce);
                    Signaling.this.onPeerMessage(box, nonce);
                } catch (ValidationError e) {
                    getLogger().error("Protocol error: Invalid incoming message: " + e.getMessage());
                    Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                }
            }
        });
    }

    /**
     * Encrypt arbitrary data for the peer using the session keys.
     *
     * @param data Plain data bytes.
     * @param dc The secure data channel that will be used to send this data.
     * @param cookie The `Cookie` instance to use.
     * @param csn The `CombinedSequenceNumber` instance to use.
     * @return Encrypted box.
     */
    public @Nullable Box encryptData(@NonNull byte[] data,
                                     @NonNull SecureDataChannel dc,
                                     @NonNull Cookie cookie,
                                     @NonNull CombinedSequence csn)
            throws CryptoFailedException, InvalidKeyException {
        // Create nonce
        final DataChannelNonce nonce = new DataChannelNonce(
                cookie.getBytes(),
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
    public @NonNull
    byte[] decryptData(@NonNull Box box)
            throws CryptoFailedException, InvalidKeyException {
        // TODO: Do we need to verify the nonce?
        return this.sessionKey.decrypt(box, this.getPeerSessionKey());
    }

    protected void handleRestart(Restart msg) {
        throw new UnsupportedOperationException("Restart not yet implemented");
    }

    protected void handleSendError(SendError msg) {
        final byte[] hash = msg.getHash();
        final String hashPrefix = NaCl.asHex(hash).substring(0, 7);
        final Message message = this.history.find(hash);

        if (message != null) {
            if (message instanceof Data) {
                final Data dataMsg = (Data) message;
                final String description = dataMsg.getType() + "/" + dataMsg.getDataType();
                getLogger().warn("SendError: Could not send " + description + " message " + hashPrefix);
                // Notify subscribers about the send-error, so they can take appropriate action.
                this.salty.events.sendError.notifyHandlers(new SendErrorEvent(msg, dataMsg));
            } else {
                getLogger().warn("SendError: Could not send " + message.getType() + " message " + hashPrefix);
                // TODO: Handle
            }
        } else {
            getLogger().warn("SendError: " + NaCl.asHex(hash));
        }
    }
}

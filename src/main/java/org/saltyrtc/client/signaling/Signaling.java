/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.events.ConnectionClosedEvent;
import org.saltyrtc.client.events.ConnectionErrorEvent;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InternalServerException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ArrayHelper;
import org.saltyrtc.client.helpers.MessageReader;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.Auth;
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
import org.webrtc.PeerConnection;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.net.ssl.SSLContext;

public abstract class Signaling {

    protected static String SALTYRTC_PROTOCOL = "saltyrtc-1.0";
    protected static short SALTYRTC_WS_CONNECT_TIMEOUT = 2000;
    protected static long SALTYRTC_WS_PING_INTERVAL = 20000;
    protected static String SALTYRTC_DC_LABEL = "saltyrtc-signaling";
    protected static short SALTYRTC_ADDR_UNKNOWN = 0x00;
    protected static short SALTYRTC_ADDR_SERVER = 0x00;
    protected static short SALTYRTC_ADDR_INITIATOR = 0x01;

    // Logger
    protected abstract Logger getLogger();

    // WebSocket
    protected String host;
    protected int port;
    protected String protocol = "wss";
    protected WebSocket ws;
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
     * Connect asynchronously to the SaltyRTC server.
     *
     * To get notified when the connection is up and running, subscribe to the `ConnectedEvent`.
     */
    public void connect() throws ConnectionException {
        getLogger().info("Connecting to SaltyRTC server at "
                + Signaling.this.host + ":" + Signaling.this.port + "...");
        resetConnection();
        try {
            initWebsocket();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ConnectionException("Connecting to WebSocket failed.");
        }
        connectWebsocket();
    }

    /**
     * Reset / close the connection.
     *
     * - Close WebSocket if still open.
     * - Set `ws` attribute to null.
     * - Set `state` attribute to `NEW`
     * - Reset server CSN
     */
    protected void resetConnection(int reason) {
        this.state = SignalingState.NEW;
        this.serverHandshakeState = ServerHandshakeState.NEW;
        this.serverCsn = new CombinedSequence();

        // Close websocket instance
        if (this.ws != null) {
            getLogger().debug("Disconnecting WebSocket (close code " + reason + ")");
            this.ws.disconnect(reason);
            this.ws = null;
        }
    }

    /**
     * @see this.resetConnection()
     */
    protected void resetConnection() {
        this.resetConnection(CloseCode.CLOSING_NORMAL);
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
                    this.ws.disconnect();
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
                    getLogger().debug("WebSocket connection open");
                    Signaling.this.state = SignalingState.SERVER_HANDSHAKE;
                }
            }

            @Override
            public void onConnectError(WebSocket websocket, WebSocketException ex) throws Exception {
                getLogger().error("Could not connect to websocket: " + ex.getMessage());
                Signaling.this.state = SignalingState.ERROR;
            }

            @Override
            public void onTextMessage(WebSocket websocket, String text) throws Exception {
                getLogger().debug("New string message: " + text);
                getLogger().error("Protocol error: Received string message, but only binary messages are valid.");
                Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
            }

            @Override
            public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
                synchronized (this) {
                    getLogger().debug("New binary message (" + binary.length + " bytes)");
                    try {
                        // Parse buffer
                        final Box box = new Box(ByteBuffer.wrap(binary), SignalingChannelNonce.TOTAL_LENGTH);

                        // Parse nonce
                        final SignalingChannelNonce nonce = new SignalingChannelNonce(ByteBuffer.wrap(box.getNonce()));

                        // Dispatch message
                        switch (Signaling.this.state) {
                            case SERVER_HANDSHAKE:
                                Signaling.this.onServerHandshakeMessage(box, nonce);
                                break;
                            case PEER_HANDSHAKE:
                                Signaling.this.onPeerHandshakeMessage(box, nonce);
                                break;
                            default:
                                getLogger().warn("Received message in " + Signaling.this.state.name() +
                                        " signaling state. Ignoring.");
                        }
                    } catch (ValidationError | SerializationError e) {
                        getLogger().error("Protocol error: Invalid incoming message: " + e.getMessage());
                        Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                    } catch (ProtocolException e) {
                        getLogger().error("Protocol error: " + e.getMessage());
                        Signaling.this.resetConnection(CloseCode.PROTOCOL_ERROR);
                    } catch (InternalServerException e) {
                        getLogger().error("Internal server error: " + e.getMessage());
                        Signaling.this.resetConnection(CloseCode.INTERNAL_ERROR);
                    }
                }
            }

            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                       WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                final String closer = closedByServer ? "server" : "client";
                final WebSocketFrame frame = closedByServer ? serverCloseFrame : clientCloseFrame;
                final int closeCode = frame.getCloseCode();
                final String closeReason = frame.getCloseReason();
                getLogger().debug("WebSocket connection closed by " + closer +
                                  " with code " + closeCode + ": " + closeReason);

                if (!closedByServer && closeCode == CloseCode.HANDOVER) {
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
                        default:
                            getLogger().warn("Unknown close code: " + closeCode);
                    }
                    saltyRTC.events.connectionClosed.notifyHandlers(
                            new ConnectionClosedEvent(SignalingChannel.WEBSOCKET)
                    );
                }
                state = SignalingState.CLOSED; // TODO don't set this on handover
            }

            @Override
            public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                getLogger().error("A WebSocket connect error occured: " + cause.getMessage());
                cause.printStackTrace();
                saltyRTC.events.connectionError.notifyHandlers(
                        new ConnectionErrorEvent(SignalingChannel.WEBSOCKET)
                );
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
        this.state = SignalingState.WS_CONNECTING;
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
            } else if (receiver == SALTYRTC_ADDR_INITIATOR || isResponderId(receiver)) {
                // TODO: Do we re-use the same cookie everywhere?
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
     * @param box The box containing raw nonce and payload bytes.
     */
    protected void onServerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ValidationError, SerializationError, ProtocolException, InternalServerException {
        // Decrypt if necessary
        final byte[] payload;
        if (this.serverHandshakeState != ServerHandshakeState.NEW) {
            try {
                payload = this.permanentKey.decrypt(box, this.serverKey);
            } catch (CryptoFailedException | InvalidKeyException e) {
                e.printStackTrace();
                throw new ProtocolException("Could not decrypt server message");
            }
        } else {
            payload = box.getData();
        }

        // Handle message
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
                break;
            case DONE:
                throw new InternalServerException("Received server handshake message even though " +
                                                  "server handshake state is set to DONE");
            default:
                throw new InternalServerException("Unknown server handshake state");
        }

        // Check if we're done yet
        if (this.serverHandshakeState == ServerHandshakeState.DONE) {
            this.state = SignalingState.PEER_HANDSHAKE;
            this.initPeerHandshake();
        }
    }

    /**
     * Message received during peer handshake.
     */
    protected abstract void onPeerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ProtocolException, ValidationError, SerializationError, InternalServerException;

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
        this.ws.sendBinary(packet);
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
    protected abstract void initPeerHandshake() throws ProtocolException;

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
     * Validate a repeated cookie in an Auth message.
     * @param msg The Auth message
     * @throws ProtocolException Thrown if repeated cookie does not match our own cookie.
     */
    protected void validateRepeatedCookie(Auth msg) throws ProtocolException {
        // Verify the cookie
        final Cookie repeatedCookie = new Cookie(msg.getYourCookie());
        if (!repeatedCookie.equals(this.cookiePair.getOurs())) {
            getLogger().debug("Peer repeated cookie: " + Arrays.toString(msg.getYourCookie()));
            getLogger().debug("Our cookie: " + Arrays.toString(this.cookiePair.getOurs().getBytes()));
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
     * Handover from WebSocket to WebRTC data channel.
     * @param pc The WebRTC PeerConnection instance
     * @return A FutureTask. It returns once the handover is done.
     */
    public FutureTask<Void> handover(final PeerConnection pc) {
        return new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() {
                // Create new signaling DataChannel
                DataChannel.Init init = new DataChannel.Init();
                init.id = 0;
                init.negotiated = true;
                init.ordered = true;
                init.protocol = SALTYRTC_PROTOCOL;
                Signaling.this.dc = pc.createDataChannel(SALTYRTC_DC_LABEL, new DataChannel.Init());
                Signaling.this.dc.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
                        Signaling.this.getLogger().info("DataChannel: Buffered amount changed");
                    }

                    @Override
                    public void onStateChange() {
                        Signaling.this.getLogger().info("DataChannel: State changed");
                    }

                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        Signaling.this.getLogger().info("DataChannel: Message arrived");
                    }
                });

                return null;
            }
        });
    }
}

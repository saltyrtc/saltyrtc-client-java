/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.events.SignalingConnectionLostEvent;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InternalException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.SignalingException;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.MessageReader;
import org.saltyrtc.client.helpers.TaskHelper;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.c2c.InitiatorAuth;
import org.saltyrtc.client.messages.c2c.Key;
import org.saltyrtc.client.messages.c2c.ResponderAuth;
import org.saltyrtc.client.messages.c2c.Token;
import org.saltyrtc.client.messages.s2c.ClientHello;
import org.saltyrtc.client.messages.s2c.NewInitiator;
import org.saltyrtc.client.messages.s2c.ResponderServerAuth;
import org.saltyrtc.client.messages.s2c.SendError;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.InitiatorHandshakeState;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

public class ResponderSignaling extends Signaling {

    @NonNull
    private Initiator initiator;
    @Nullable
    private AuthToken authToken = null;

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger("SaltyRTC.RSignaling");
    }

    public ResponderSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              @Nullable byte[] initiatorPublicKey, @Nullable byte[] authToken,
                              @Nullable byte[] initiatorTrustedKey,
                              @Nullable byte[] expectedServerKey,
                              @NonNull Task[] tasks,
                              int pingInterval)
                              throws InvalidKeyException {
        super(saltyRTC, host, port, permanentKey, sslContext, initiatorTrustedKey, expectedServerKey,
              SignalingRole.Responder, tasks, pingInterval);
        if (initiatorTrustedKey != null) {
            if (initiatorPublicKey != null || authToken != null) {
                throw new IllegalArgumentException(
                    "Cannot specify both a trusted key and a public key / auth token pair");
            }
            this.initiator = new Initiator(initiatorTrustedKey);
            // If we trust the initiator, don't send a token message
            this.initiator.handshakeState = InitiatorHandshakeState.TOKEN_SENT;
        } else if (initiatorPublicKey != null && authToken != null) {
            this.initiator = new Initiator(initiatorPublicKey);
            this.authToken = new AuthToken(authToken);
        } else {
            throw new IllegalArgumentException(
                "You must specify either a trusted key or a public key / auth token pair");
        }
    }

    /**
     * The responder needs to use the initiator public permanent key as connection path.
     */
    @Override
    protected String getWebsocketPath() {
        return NaCl.asHex(this.initiator.getPermanentKey());
    }

    @Override
    protected Box encryptHandshakeDataForPeer(short receiver, String messageType,
                                              byte[] payload, byte[] nonce)
            throws CryptoFailedException, InvalidKeyException, ProtocolException {
        if (this.isResponderId(receiver)) {
            throw new ProtocolException("Responder may not encrypt messages for other responders: " + receiver);
        } else if (receiver != Signaling.SALTYRTC_ADDR_INITIATOR) {
            throw new ProtocolException("Bad receiver byte: " + receiver);
        }
        switch (messageType) {
            case "token":
                if (this.authToken == null) {
                    throw new ProtocolException(
                        "Cannot encrypt token message for peer: Auth token is null");
                }
                return this.authToken.encrypt(payload, nonce);
            case "key":
                return this.permanentKey.encrypt(payload, nonce, this.initiator.permanentKey);
            default:
                byte[] peerSessionKey = this.getPeerSessionKey();
                if (peerSessionKey == null) {
                    throw new ProtocolException(
                            "Trying to encrypt for peer using session key, but session key is null");
                }
                return this.sessionKey.encrypt(payload, nonce, peerSessionKey);
        }
    }

    @Override
    protected void sendClientHello() throws SignalingException, ConnectionException {
        final ClientHello msg = new ClientHello(this.permanentKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, this.server, false);
        this.getLogger().debug("Sending client-hello");
        this.send(packet, msg);
        this.server.handshakeState = ServerHandshakeState.HELLO_SENT;
    }

    @Override
    protected void handleServerAuth(Message baseMsg, SignalingChannelNonce nonce) throws ProtocolException {
        // Cast to proper subtype
        final ResponderServerAuth msg;
        try {
            msg = (ResponderServerAuth) baseMsg;
        } catch (ClassCastException e) {
            throw new ProtocolException("Could not cast message to ResponderServerAuth");
        }

        // Set proper address
        if (nonce.getDestination() > 0xff || nonce.getDestination() < 0x02) {
            throw new ProtocolException("Invalid nonce destination: " + nonce.getDestination());
        }
        this.address = nonce.getDestination();
        this.getLogger().debug("Server assigned address 0x" + NaCl.asHex(new int[] { this.address }));

        // Validate cookie
        // TODO: Move into validateRepeatedCookie method
        final Cookie repeatedCookie = new Cookie(msg.getYourCookie());
        final Cookie ourCookie = this.server.getCookiePair().getOurs();
        if (!repeatedCookie.equals(ourCookie)) {
            this.getLogger().error("Bad repeated cookie in server-auth message");
            this.getLogger().debug("Their response: " + Arrays.toString(repeatedCookie.getBytes()) +
                    ", our cookie: " + Arrays.toString(ourCookie.getBytes()));
            throw new ProtocolException("Bad repeated cookie in server-auth message");
        }

        // Validate expected server key
        if (this.expectedServerKey != null) {
            try {
                this.validateSignedKeys(msg.getSignedKeys(), nonce, this.expectedServerKey);
            } catch (ValidationError e) {
                this.getLogger().error(e.getMessage());
                throw new ProtocolException("Verification of signed_keys failed", e);
            }
        } else if (msg.getSignedKeys() != null) {
            this.getLogger().warn("Server sent signed keys, but we're not verifying them.");
        }

        // Store whether initiator is connected
        this.initiator.setConnected(msg.isInitiatorConnected());
        this.getLogger().debug("Initiator is " + (msg.isInitiatorConnected() ? "" : "not ") + "connected.");

        // Server handshake is done!
        this.server.handshakeState = ServerHandshakeState.DONE;
    }

    @Override
    protected void initPeerHandshake() throws SignalingException, ConnectionException {
        if (this.initiator.isConnected()) {
            // Only send token if we don't trust the initiator
            if (!this.hasTrustedKey()) {
                this.sendToken();
            }
            this.sendKey();
        }
    }

    /**
     * Send our token to the initiator.
     */
    private void sendToken() throws SignalingException, ConnectionException {
        final Token msg = new Token(this.permanentKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, this.initiator);
        this.getLogger().debug("Sending token");
        this.send(packet, msg);
        this.initiator.handshakeState = InitiatorHandshakeState.TOKEN_SENT;
    }

    /**
     * Send our public session key to the initiator.
     */
    private void sendKey() throws SignalingException, ConnectionException {
        // Generate our own session key
        this.sessionKey = new KeyStore();

        // Send public key to initiator
        final Key msg = new Key(this.sessionKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, this.initiator);
        this.getLogger().debug("Sending key");
        this.send(packet, msg);
        this.initiator.handshakeState = InitiatorHandshakeState.KEY_SENT;
    }

    /**
     * The initiator sends his public session key.
     */
    private void handleKey(Key msg) {
        this.initiator.setSessionKey(msg.getKey());
        this.initiator.handshakeState = InitiatorHandshakeState.KEY_RECEIVED;
    }

    /**
     * Repeat the initiator's cookie and send task list.
     */
    private void sendAuth(SignalingChannelNonce nonce) throws SignalingException, ConnectionException {
        // Ensure that cookies are different
        if (nonce.getCookie().equals(this.initiator.getCookiePair().getOurs())) {
            throw new ProtocolException("Their cookie and our cookie are the same");
        }

        // Send auth
        final ResponderAuth msg;
        try {
            final Map<String, Map<Object, Object>> tasksData = new HashMap<>();
            for (Task task : this.tasks) {
                tasksData.put(task.getName(), task.getData());
            }
            msg = new ResponderAuth(nonce.getCookieBytes(), TaskHelper.getTaskNames(this.tasks), tasksData);
        } catch (ValidationError e) {
            throw new ProtocolException("Invalid task data", e);
        }
        final byte[] packet = this.buildPacket(msg, this.initiator);
        this.getLogger().debug("Sending auth");
        this.send(packet, msg);
        this.initiator.handshakeState = InitiatorHandshakeState.AUTH_SENT;
    }

    /**
     * The initiator repeats our cookie and sends the chosen task.
     */
    private void handleAuth(InitiatorAuth msg, SignalingChannelNonce nonce) throws SignalingException {
        // Validate cookie
        this.validateRepeatedCookie(this.initiator, msg.getYourCookie());

        // Validation of task list and data already happens in the `InitiatorAuth` constructor

        // Find selected task
        final String taskName = msg.getTask();
        Task selectedTask = null;
        for (Task task : this.tasks) {
            if (task.getName().equals(taskName)) {
                this.getLogger().info("Task " + task.getName() + " has been selected");
                selectedTask = task;
                break;
            }
        }

        // Initialize task
        if (selectedTask == null) {
            throw new SignalingException(CloseCode.PROTOCOL_ERROR, "Initiator selected unknown task");
        } else {
            this.initTask(selectedTask, msg.getData().get(selectedTask.getName()));
        }

        // OK!
        this.getLogger().debug("Initiator authenticated");
        this.initiator.getCookiePair().setTheirs(nonce.getCookie());
        this.initiator.handshakeState = InitiatorHandshakeState.AUTH_RECEIVED;
    }

    /**
     * Decrypt messages from the initiator.
     *
     * @param box encrypted box containing message.
     * @return The decrypted message bytes.
     * @throws ProtocolException if decryption fails or when receiving messages in an invalid state.
     */
    private byte[] decryptInitiatorMessage(Box box) throws ProtocolException {
        switch (this.initiator.handshakeState) {
            case NEW:
            case TOKEN_SENT:
            case KEY_RECEIVED:
                throw new ProtocolException(
                    "Received message in " + this.initiator.handshakeState.name() + " state.");
            case KEY_SENT:
                // Expect a key message, encrypted with the permanent keys
                try {
                    return this.permanentKey.decrypt(box, this.initiator.getPermanentKey());
                } catch (CryptoFailedException | InvalidKeyException e) {
                    e.printStackTrace();
                    throw new ProtocolException("Could not decrypt key message");
                }
            case AUTH_SENT:
            case AUTH_RECEIVED:
                // Otherwise, it must be encrypted with the session key.
                try {
                    return this.sessionKey.decrypt(box, this.initiator.getSessionKey());
                } catch (CryptoFailedException | InvalidKeyException e) {
                    e.printStackTrace();
                    throw new ProtocolException("Could not decrypt message using session key");
                }
            default:
                throw new ProtocolException(
                    "Invalid handshake state: " + this.initiator.handshakeState.name());
        }
    }

    @Override
    protected void onPeerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ValidationError, SerializationError,
            InternalException, ConnectionException, SignalingException {

        // Validate nonce destination
        if (nonce.getDestination() != this.address) {
            throw new ProtocolException("Message destination does not match our address");
        }

        final byte[] payload;
        if (nonce.getSource() == SALTYRTC_ADDR_SERVER) {
            // Nonce claims to come from server.
            // Try to decrypt data accordingly.
            try {
                assert this.server.hasSessionKey();
                payload = this.permanentKey.decrypt(box, this.server.getSessionKey());
            } catch (CryptoFailedException | InvalidKeyException e) {
                e.printStackTrace();
                throw new ProtocolException("Could not decrypt server message");
            }

            final Message msg = MessageReader.read(payload);
            if (msg instanceof NewInitiator) {
                this.getLogger().debug("Received new-initiator");
                this.handleNewInitiator((NewInitiator) msg);
            } else if (msg instanceof SendError) {
                this.getLogger().debug("Received send-error");
                this.handleSendError((SendError) msg);
            } else {
                throw new ProtocolException("Got unexpected server message: " + msg.getType());
            }
        } else if (nonce.getSource() == SALTYRTC_ADDR_INITIATOR) {
            // Dispatch message
            payload = this.decryptInitiatorMessage(box);
            final Message msg = MessageReader.read(payload);
            switch (this.initiator.handshakeState) {
                case KEY_SENT:
                    // Expect a key message
                    if (msg instanceof Key) {
                        this.getLogger().debug("Received key");
                        this.handleKey((Key) msg);
                        this.sendAuth(nonce);
                    } else {
                        throw new ProtocolException("Expected key message, but got " + msg.getType());
                    }
                    break;
                case AUTH_SENT:
                    // Expect an auth message
                    if (msg instanceof InitiatorAuth) {
                        this.getLogger().debug("Received auth");
                        this.handleAuth((InitiatorAuth) msg, nonce);
                    } else {
                        throw new ProtocolException("Expected auth message, but got " + msg.getType());
                    }

                    // We're connected!
                    this.setState(SignalingState.TASK);
                    this.getLogger().info("Peer handshake done");
                    this.task.onPeerHandshakeDone();

                    break;
                default:
                    throw new InternalException("Unknown or invalid initiator handshake state");
            }
        } else {
            throw new ProtocolException("Message source is neither the server nor the initiator");
        }
    }

    /**
     * A new initiator replaces the old one.
     */
    private void handleNewInitiator(NewInitiator msg) throws SignalingException, ConnectionException {
        this.initiator = new Initiator(this.initiator.getPermanentKey());
        this.initiator.setConnected(true);
        this.initPeerHandshake();
    }

    @Override
    void handleSendError(short receiver) throws SignalingException {
        if (receiver != Signaling.SALTYRTC_ADDR_INITIATOR) {
            throw new ProtocolException("Outgoing c2c messages must have been sent to the initiator");
        }

        // Notify application
        this.salty.events.signalingConnectionLost.notifyHandlers(new SignalingConnectionLostEvent(receiver));

        // Reset connection
        this.resetConnection(CloseCode.PROTOCOL_ERROR);
        // TODO: Maybe keep ws connection open and wait for reconnect
    }

    @Override
    @Nullable
    protected Peer getPeer() {
        return this.initiator;
    }

    @Override
    @Nullable
    protected byte[] getPeerSessionKey() {
        return this.initiator.sessionKey;
    }

    /**
     * Get the responder instance with the specified id.
     *
     * In contrast to `getPeer()`, this also returns responders that haven't finished the
     * client-to-client handshake.
     */
    @Nullable
    Peer getPeerWithId(short id) throws SignalingException {
        if (id == SALTYRTC_ADDR_SERVER) {
            return this.server;
        } else if (id == SALTYRTC_ADDR_INITIATOR) {
            return this.initiator;
        } else {
            throw new ProtocolException("Invalid peer id: " + id);
        }
    }

}

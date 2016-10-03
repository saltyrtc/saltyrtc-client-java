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
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InternalException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.OverflowException;
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
import org.saltyrtc.client.nonce.CombinedSequence;
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
    private final Initiator initiator;
    @Nullable
    private AuthToken authToken = null;

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger("SaltyRTC.RSignaling");
    }

    /**
     * Create an instance without a trusted key.
     */
    public ResponderSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              byte[] initiatorPublicKey, byte[] authToken,
                              Task[] tasks)
                              throws java.security.InvalidKeyException {
        super(saltyRTC, host, port, permanentKey, sslContext, tasks);
        this.role = SignalingRole.Responder;
        this.initiator = new Initiator(initiatorPublicKey);
        this.authToken = new AuthToken(authToken);
    }

    /**
     * Create an instance with a trusted key.
     */
    public ResponderSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              byte[] initiatorTrustedKey,
                              Task[] tasks)
            throws java.security.InvalidKeyException {
        super(saltyRTC, host, port, permanentKey, sslContext, initiatorTrustedKey, tasks);
        this.role = SignalingRole.Responder;
        this.initiator = new Initiator(initiatorTrustedKey);
        // If we trust the initiator, don't send a token message
        this.initiator.handshakeState = InitiatorHandshakeState.TOKEN_SENT;
    }

    /**
     * The responder needs to use the initiator public permanent key as connection path.
     */
    @Override
    protected String getWebsocketPath() {
        return NaCl.asHex(this.initiator.getPermanentKey());
    }

    @Override
    protected CombinedSequence getNextCsn(short receiver) throws ProtocolException {
        try {
            if (receiver == Signaling.SALTYRTC_ADDR_SERVER) {
                return this.serverCsn.getOurs().next();
            } else if (receiver == Signaling.SALTYRTC_ADDR_INITIATOR) {
                return this.initiator.getCsnPair().getOurs().next();
            } else if (this.isResponderId(receiver)) {
                throw new ProtocolException("Responder may not send messages to other responders: " + receiver);
            } else {
                throw new ProtocolException("Bad receiver byte: " + receiver);
            }
        } catch (OverflowException e) {
            throw new ProtocolException("OverflowException: " + e.getMessage());
        }
    }

    @Override
    protected Box encryptForPeer(short receiver, String messageType, byte[] payload, byte[] nonce)
            throws CryptoFailedException, InvalidKeyException, ProtocolException {
        if (this.isResponderId(receiver)) {
            throw new ProtocolException("Responder may not encrypt messages for other responders: " + receiver);
        } else if (receiver != Signaling.SALTYRTC_ADDR_INITIATOR) {
            throw new ProtocolException("Bad receiver byte: " + receiver);
        }
        switch (messageType) {
            case "token":
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
    protected void sendClientHello() throws ProtocolException, ConnectionException {
        final ClientHello msg = new ClientHello(this.permanentKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_SERVER, false);
        this.getLogger().debug("Sending client-hello");
        this.send(packet, msg);
        this.serverHandshakeState = ServerHandshakeState.HELLO_SENT;
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
        final Cookie cookie = new Cookie(msg.getYourCookie());
        if (!cookie.equals(this.cookie)) {
            this.getLogger().error("Bad repeated cookie in server-auth message");
            this.getLogger().debug("Their response: " + Arrays.toString(msg.getYourCookie()) +
                    ", our cookie: " + Arrays.toString(this.cookie.getBytes()));
            throw new ProtocolException("Bad repeated cookie in server-auth message");
        }

        // Store whether initiator is connected
        this.initiator.setConnected(msg.isInitiatorConnected());
        this.getLogger().debug("Initiator is " + (msg.isInitiatorConnected() ? "" : "not ") + "connected.");

        // Server handshake is done!
        this.serverHandshakeState = ServerHandshakeState.DONE;
    }

    @Override
    protected void initPeerHandshake() throws ProtocolException, ConnectionException {
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
    private void sendToken() throws ProtocolException, ConnectionException {
        final Token msg = new Token(this.permanentKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_INITIATOR);
        this.getLogger().debug("Sending token");
        this.send(packet, msg);
        this.initiator.handshakeState = InitiatorHandshakeState.TOKEN_SENT;
    }

    /**
     * Send our public session key to the initiator.
     */
    private void sendKey() throws ProtocolException, ConnectionException {
        // Generate our own session key
        this.sessionKey = new KeyStore();
        final Key msg = new Key(this.sessionKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, SALTYRTC_ADDR_INITIATOR);
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
    private void sendAuth(SignalingChannelNonce nonce) throws ProtocolException, ConnectionException {
        // Ensure that cookies are different
        if (nonce.getCookie().equals(this.cookie)) {
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
        final byte[] packet = this.buildPacket(msg, SALTYRTC_ADDR_INITIATOR);
        this.getLogger().debug("Sending auth");
        this.send(packet, msg);
        this.initiator.handshakeState = InitiatorHandshakeState.AUTH_SENT;
    }

    /**
     * The initiator repeats our cookie.
     */
    private void handleAuth(InitiatorAuth msg, SignalingChannelNonce nonce) throws ProtocolException, SignalingException {
        // Validate cookie
        this.validateRepeatedCookie(msg.getYourCookie());

        // Validation of task list and data already happens in the `InitiatorAuth` constructor

        // Initialize task
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
        this.initiator.setCookie(nonce.getCookie());
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
            throws ProtocolException, ValidationError, SerializationError,
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
                payload = this.permanentKey.decrypt(box, this.serverKey);
            } catch (CryptoFailedException | InvalidKeyException e) {
                e.printStackTrace();
                throw new ProtocolException("Could not decrypt server message");
            }

            final Message msg = MessageReader.read(payload);
            if (msg instanceof NewInitiator) {
                this.getLogger().debug("Received new-initiator");
                this.handleNewInitiator((NewInitiator) msg);
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
    private void handleNewInitiator(NewInitiator msg) throws ProtocolException, ConnectionException {
        this.initiator.setConnected(true);
        // TODO: Replace old initiator?
        this.initPeerHandshake();
    }

    @Override
    @Nullable
    protected Short getPeerAddress() {
        return this.initiator.getId();
    }

    @Override
    @Nullable
    public Cookie getPeerCookie() {
        return this.initiator.getCookie();
    }

    @Override
    @Nullable
    protected byte[] getPeerSessionKey() {
        return this.initiator.sessionKey;
    }

    /**
     * Validate CSN of the initiator.
     */
    protected void validateSignalingNoncePeerCsn(SignalingChannelNonce nonce) throws ValidationError {
        if (nonce.getSource() == SALTYRTC_ADDR_INITIATOR) {
            this.validateSignalingNonceCsn(nonce, this.initiator.getCsnPair(), "initiator");
        } else {
            throw new ValidationError("Invalid source byte, cannot validate CSN");
        }
    }

}

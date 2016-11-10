/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.SaltyRTC;
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
import org.saltyrtc.client.messages.s2c.DropResponder;
import org.saltyrtc.client.messages.s2c.InitiatorServerAuth;
import org.saltyrtc.client.messages.s2c.NewResponder;
import org.saltyrtc.client.messages.s2c.SendError;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.ResponderHandshakeState;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

public class InitiatorSignaling extends Signaling {

    // Keep track of responders connected to the server
    private final Map<Short, Responder> responders = new HashMap<>();

    // Once the handshake is done, this is the chosen responder
    private Responder responder;

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger("SaltyRTC.ISignaling");
    }

    public InitiatorSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              @Nullable byte[] responderTrustedKey,
                              @Nullable byte[] expectedServerKey,
                              Task[] tasks) {
        super(saltyRTC, host, port, permanentKey, sslContext, responderTrustedKey, expectedServerKey,
              SignalingRole.Initiator, tasks);
        if (responderTrustedKey == null) {
            this.authToken = new AuthToken();
        }
    }

    /**
     * The initiator needs to use its own public permanent key as connection path.
     */
    @Override
    protected String getWebsocketPath() {
        return NaCl.asHex(this.permanentKey.getPublicKey());
    }

    @Override
    protected Box encryptHandshakeDataForPeer(short receiver, String messageType,
                                              byte[] payload, byte[] nonce)
            throws CryptoFailedException, InvalidKeyException, ProtocolException {
        if (receiver == SALTYRTC_ADDR_INITIATOR) {
            throw new ProtocolException("Initiator cannot encrypt messages for initiator");
        } else if (!this.isResponderId(receiver)) {
            throw new ProtocolException("Bad receiver byte: " + receiver);
        }

        // Find correct responder
        final Responder responder;
        if (this.getState() == SignalingState.TASK) {
            assert this.responder != null;
            responder = this.responder;
        } else if (this.responders.containsKey(receiver)) {
            responder = this.responders.get(receiver);
        } else {
            throw new ProtocolException("Unknown responder: " + receiver);
        }

        // Encrypt
        if ("key".equals(messageType)) {
            return this.permanentKey.encrypt(payload, nonce, responder.getPermanentKey());
        } else {
            return responder.getKeyStore().encrypt(payload, nonce, responder.getSessionKey());
        }
    }

    /**
     * Validate a responder id. Throw a ProtocolException if the id is out of range.
     * Cast it to a short otherwise.
     */
    private short validateResponderId(int id) throws ProtocolException {
        // TODO: Do we need to check whether it's in the valid responder range too?
        if (id < 0) {
            throw new ProtocolException("Responder id may not be smaller than 0");
        } else if (id > 0xff) {
            throw new ProtocolException("Responder id may not be larger than 255");
        }
        return (short) id;
    }

    @Override
    protected void sendClientHello() {
        // No-op as initiator.
    }

    @Override
    protected void handleServerAuth(Message baseMsg, SignalingChannelNonce nonce) throws ProtocolException {
        // Cast to proper subtype
        final InitiatorServerAuth msg;
        try {
            msg = (InitiatorServerAuth) baseMsg;
        } catch (ClassCastException e) {
            throw new ProtocolException("Could not cast message to InitiatorServerAuth");
        }

        // Set address
        this.address = SALTYRTC_ADDR_INITIATOR;

        // Validate cookie
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

        // Process responders
        for (int number : msg.getResponders()) {
            final short id = this.validateResponderId(number);
            this.processNewResponder(id);
        }
        this.getLogger().debug(this.responders.size() + " responder(s) connected.");

        // Server handshake is done!
        this.server.handshakeState = ServerHandshakeState.DONE;
    }

    @Override
    protected void initPeerHandshake() {
        // No-op as initiator.
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
            if (msg instanceof NewResponder) {
                this.getLogger().debug("Received new-responder");
                this.handleNewResponder((NewResponder) msg);
            } else if (msg instanceof SendError) {
                this.getLogger().debug("Received send-error");
                this.handleSendError((SendError) msg);
            } else {
                throw new ProtocolException("Got unexpected server message: " + msg.getType());
            }
        } else if (this.isResponderId(nonce.getSource())) {
            // Get responder instance
            final Responder responder = this.responders.get(nonce.getSource());
            if (responder == null) {
                throw new ProtocolException("Unknown message sender: " + nonce.getSource());
            }

            // Dispatch message
            final Message msg;
            switch (responder.handshakeState) {
                case NEW:
                    if (this.hasTrustedKey()) {
                        throw new ProtocolException(
                            "Handshake state is NEW even though a trusted key is available");
                    }

                    // Expect token message, encrypted with authentication token.
                    try {
                        assert this.authToken != null;
                        payload = this.authToken.decrypt(box);
                    } catch (CryptoFailedException e) {
                        this.getLogger().warn("Could not decrypt token message");
                        this.dropResponder(responder, CloseCode.INITIATOR_COULD_NOT_DECRYPT);
                        return;
                    }
                    msg = MessageReader.read(payload);
                    if (msg instanceof Token) {
                        this.getLogger().debug("Received token");
                        this.handleToken((Token) msg, responder);
                    } else {
                        throw new ProtocolException("Expected token message, but got " + msg.getType());
                    }
                    break;
                case TOKEN_RECEIVED:
                    // Expect key message, encrypted with our public permanent key
                    // and responder private permanent key
                    try {
                        final byte[] peerPublicKey = this.hasTrustedKey()
                                                   ? this.peerTrustedKey
                                                   : responder.permanentKey;
                        payload = this.permanentKey.decrypt(box, peerPublicKey);
                    } catch (CryptoFailedException e) {
                        this.getLogger().warn("Could not decrypt key message");
                        this.dropResponder(responder, CloseCode.INITIATOR_COULD_NOT_DECRYPT);
                        return;
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                        throw new ProtocolException("Invalid key when decrypting key message", e);
                    }

                    msg = MessageReader.read(payload);
                    if (msg instanceof Key) {
                        this.getLogger().debug("Received key");
                        this.handleKey((Key) msg, responder);
                        this.sendKey(responder);
                    } else {
                        throw new ProtocolException("Expected key message, but got " + msg.getType());
                    }
                    break;
                case KEY_SENT:
                    // Expect auth message, encrypted with our public session key
                    // and responder private session key
                    try {
                        // Note: The session key related to the responder is
                        // responder.keyStore, not this.sessionKey!
                        payload = responder.getKeyStore().decrypt(box, responder.sessionKey);
                    } catch (CryptoFailedException e) {
                        e.printStackTrace();
                        throw new ProtocolException("Could not decrypt auth message");
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                        throw new ProtocolException("Invalid key when decrypting auth message", e);
                    }

                    msg = MessageReader.read(payload);
                    if (msg instanceof ResponderAuth) {
                        this.getLogger().debug("Received auth");
                        this.handleAuth((ResponderAuth) msg, responder, nonce);
                        this.sendAuth(responder, nonce);
                    } else {
                        throw new ProtocolException("Expected auth message, but got " + msg.getType());
                    }

                    // We're connected!
                    this.responder = responder;
                    this.sessionKey = responder.getKeyStore();

                    // Remove responder from responders list
                    this.responders.remove(responder.getId());

                    // Drop other responders
                    this.dropResponders(CloseCode.DROPPED_BY_INITIATOR);

                    // Peer handshake done
                    this.setState(SignalingState.TASK);
                    this.getLogger().info("Peer handshake done");
                    this.task.onPeerHandshakeDone();

                    break;
                default:
                    throw new InternalException("Unknown or invalid responder handshake state: "
                        + responder.handshakeState.name());
            }
        } else {
            throw new ProtocolException("Message source is neither the server nor a responder");
        }
    }

    /**
     * A new responder wants to connect.
     */
    private void handleNewResponder(NewResponder msg) throws SignalingException {
        // Validate responder id
        final short id = this.validateResponderId(msg.getId());

        // Process responder
        this.processNewResponder(id);
    }

    /**
     * Store a new responder.
     */
    private void processNewResponder(short responderId) {
        // Drop responder if it's already known
        if (this.responders.containsKey(responderId)) {
            this.responders.remove(responderId);
        }

        // Create responder instance
        final Responder responder = new Responder(responderId);

        // If we trust the responder...
        if (this.hasTrustedKey()) {
            // ...don't expect a token message.
            responder.handshakeState = ResponderHandshakeState.TOKEN_RECEIVED;

            // Set the public permanent key.
            responder.setPermanentKey(this.peerTrustedKey);
        }

        // Store responder
        this.responders.put(responderId, responder);
    }

    /**
     * A responder sends his public permanent key.
     */
    private void handleToken(Token msg, Responder responder) {
        responder.setPermanentKey(msg.getKey());
        responder.handshakeState = ResponderHandshakeState.TOKEN_RECEIVED;
    }

    /**
     * A responder sends his public session key.
     */
    private void handleKey(Key msg, Responder responder) {
        responder.setSessionKey(msg.getKey());
        responder.handshakeState = ResponderHandshakeState.KEY_RECEIVED;
    }

    /**
     * Send our public session key to the responder.
     */
    private void sendKey(Responder responder) throws SignalingException, ConnectionException {
        final Key msg = new Key(responder.getKeyStore().getPublicKey());
        final byte[] packet = this.buildPacket(msg, responder);
        this.getLogger().debug("Sending key");
        this.send(packet, msg);
        responder.handshakeState = ResponderHandshakeState.KEY_SENT;
    }

    /**
     * A responder repeats our cookie and sends a list of acceptable tasks.
     */
    private void handleAuth(ResponderAuth msg, Responder responder, SignalingChannelNonce nonce) throws SignalingException {
        // Validate cookie
        this.validateRepeatedCookie(responder, msg.getYourCookie());

        // Validation of task list and data already happens in the `ResponderAuth` constructor

        // Select task
        final Task task = TaskHelper.chooseCommonTask(this.tasks, msg.getTasks());
        if (task == null) {
            throw new SignalingException(CloseCode.NO_SHARED_TASK, "No shared task could be found");
        } else {
            this.getLogger().info("Task " + task.getName() + " has been selected");
        }

        // Initialize task
        this.initTask(task, msg.getData().get(task.getName()));

        // OK!
        this.getLogger().debug("Responder 0x" + NaCl.asHex(new int[] { responder.getId() }) + " authenticated");

        // Store cookie
        responder.getCookiePair().setTheirs(nonce.getCookie());

        // Update state
        responder.handshakeState = ResponderHandshakeState.AUTH_RECEIVED;
    }

    /**
     * Repeat the responder's cookie and choose a task.
     */
    private void sendAuth(Responder responder, SignalingChannelNonce nonce) throws SignalingException, ConnectionException {
        // Send auth
        final InitiatorAuth msg;
        try {
            final Map<String, Map<Object, Object>> tasksData = new HashMap<>();
            tasksData.put(this.task.getName(), this.task.getData());
            msg = new InitiatorAuth(nonce.getCookieBytes(), this.task.getName(), tasksData);
        } catch (ValidationError e) {
            throw new ProtocolException("Invalid task data", e);
        }
        final byte[] packet = this.buildPacket(msg, responder);
        this.getLogger().debug("Sending auth");
        this.send(packet, msg);

        // Update state
        responder.handshakeState = ResponderHandshakeState.AUTH_SENT;
    }

    /**
     * Drop specific responder.
     */
    private void dropResponder(Responder responder, @Nullable Integer reason) throws SignalingException, ConnectionException {
        final DropResponder msg = new DropResponder(responder.getId(), reason);
        final byte[] packet = this.buildPacket(msg, responder);
        this.getLogger().debug("Sending drop-responder " + responder.getId());
        this.send(packet, msg);
        this.responders.remove(responder.getId());
    }

    /**
     * Drop all responders.
     */
    private void dropResponders(@Nullable Integer reason) throws SignalingException, ConnectionException {
        this.getLogger().debug("Dropping " + this.responders.size() + " other responders");
        for (Responder responder : this.responders.values()) {
            this.dropResponder(responder, reason);
        }
    }

    @Override
    synchronized void handleSendError(short receiver) throws SignalingException {
        // Validate receiver byte
        if (!this.isResponderId(receiver)) {
            throw new ProtocolException("Outgoing c2c messages must have been sent to a responder");
        }

        boolean notify = false;
        if (this.responder == null) { // We're not yet authenticated
            // Get responder
            final Responder responder = this.responders.get(receiver);
            if (responder == null) {
                this.getLogger().warn("Got send-error message for unknown responder " + receiver);
            } else {
                notify = true;
                // Drop information about responder
                this.responders.remove(receiver);
            }
        } else { // We're authenticated
            if (this.responder.getId() == receiver) {
                notify = true;
                this.resetConnection(CloseCode.PROTOCOL_ERROR);
                // TODO: Maybe keep ws connection open and wait for reconnect
            } else {
                this.getLogger().warn("Got send-error message for unknown responder " + receiver);
            }
        }

        // Notify user application if relevant
        if (notify) {
            this.salty.events.signalingConnectionLost.notifyHandlers(new SignalingConnectionLostEvent(receiver));
        }
    }

	/**
     * Get the chosen responder instance.
     *
     * This will return null as long as the client-to-client handshake has not been completed.
     */
    @Override
    @Nullable
    protected Peer getPeer() {
        return this.responder;
    }

    @Override
    @Nullable
    protected byte[] getPeerSessionKey() {
        if (this.responder != null) {
            return this.responder.sessionKey;
        }
        return null;
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
        } else if (this.isResponderId(id)) {
            //noinspection ConstantConditions
            if (this.getState() == SignalingState.TASK && this.responder != null & this.responder.getId() == id) {
                return this.responder;
            } else if (this.responders.containsKey(id)) {
                return this.responders.get(id);
            }
            return null;
        } else {
            throw new ProtocolException("Invalid peer id: " + id);
        }
    }

}

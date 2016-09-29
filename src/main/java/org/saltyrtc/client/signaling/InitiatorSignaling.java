/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import com.neilalexander.jnacl.NaCl;

import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InternalException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.OverflowException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SerializationError;
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
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.ResponderHandshakeState;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tasks.Task;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

    /**
     * Create an instance without a trusted key.
     */
    public InitiatorSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              Task[] tasks) {
        super(saltyRTC, host, port, permanentKey, sslContext, tasks);
        this.role = SignalingRole.Initiator;
        this.authToken = new AuthToken();
    }

    /**
     * Create an instance with a trusted key.
     */
    public InitiatorSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              byte[] responderTrustedKey,
                              Task[] tasks) {
        super(saltyRTC, host, port, permanentKey, sslContext, responderTrustedKey, tasks);
        this.role = SignalingRole.Initiator;
        this.authToken = null;
    }

    /**
     * The initiator needs to use its own public permanent key as connection path.
     */
    @Override
    protected String getWebsocketPath() {
        return NaCl.asHex(this.permanentKey.getPublicKey());
    }

    @Override
    protected CombinedSequence getNextCsn(short receiver) throws ProtocolException {
        try {
            if (receiver == SALTYRTC_ADDR_SERVER) {
                return this.serverCsn.getOurs().next();
            } else if (receiver == SALTYRTC_ADDR_INITIATOR) {
                throw new ProtocolException("Initiator cannot send messages to initiator");
            } else if (this.isResponderId(receiver)) {
                if (this.getState() == SignalingState.OPEN) {
                    assert this.responder != null;
                    return this.responder.getCsnPair().getOurs().next();
                } else if (this.responders.containsKey(receiver)) {
                    return this.responders.get(receiver).getCsnPair().getOurs().next();
                } else {
                    throw new ProtocolException("Unknown responder: " + receiver);
                }
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
        if (receiver == SALTYRTC_ADDR_INITIATOR) {
            throw new ProtocolException("Initiator cannot encrypt messages for initiator");
        } else if (!this.isResponderId(receiver)) {
            throw new ProtocolException("Bad receiver byte: " + receiver);
        }

        // Find correct responder
        final Responder responder;
        if (this.getState() == SignalingState.OPEN) {
            assert this.responder != null;
            responder = this.responder;
        } else if (this.responders.containsKey(receiver)) {
            responder = this.responders.get(receiver);
        } else {
            throw new ProtocolException("Unknown responder: " + receiver);
        }

        // Encrypt
        if (messageType.equals("key")) {
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
        if (id < 0) {
            throw new ProtocolException("Responder id may not be smaller than 0");
        } else if (id > 0xff) {
            throw new ProtocolException("Responder id may not be larger than 255");
        }
        return (short) id;
    }

    /**
     * Validate CSN of the responder.
     */
    protected void validateSignalingNoncePeerCsn(SignalingChannelNonce nonce) throws ValidationError {
        final short source = nonce.getSource();
        if (this.isResponderId(source)) {
            final Responder responder = this.getResponder(source);
            if (responder == null) {
                throw new ValidationError("Unknown responder: " + source);
            }
            this.validateSignalingNonceCsn(nonce, responder.getCsnPair(), "responder (" + source + ")");
        } else {
            throw new ValidationError("Invalid source byte, cannot validate CSN");
        }
    }

    @Override
    protected void sendClientHello() {
        // No-op as initiator.
    }

    @Override
    protected void handleServerAuth(Message baseMsg, SignalingChannelNonce nonce)
            throws ProtocolException, ConnectionException {
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
        final Cookie cookie = new Cookie(msg.getYourCookie());
        if (!cookie.equals(this.cookie)) {
            this.getLogger().error("Bad repeated cookie in server-auth message");
            this.getLogger().debug("Their response: " + Arrays.toString(msg.getYourCookie()) +
                              ", our cookie: " + Arrays.toString(this.cookie.getBytes()));
            throw new ProtocolException("Bad repeated cookie in server-auth message");
        }

        // Process responders
        for (int number : msg.getResponders()) {
            final short id = this.validateResponderId(number);
            this.processNewResponder(id);
        }
        this.getLogger().debug(this.responders.size() + " responder(s) connected.");

        // Server handshake is done!
        this.serverHandshakeState = ServerHandshakeState.DONE;
    }

    @Override
    protected void initPeerHandshake() {
        // No-op as initiator.
    }

    @Override
    protected void onPeerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ProtocolException, ValidationError, SerializationError,
            InternalException, ConnectionException {

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
            if (msg instanceof NewResponder) {
                this.getLogger().debug("Received new-responder");
                this.handleNewResponder((NewResponder) msg);
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
                        this.dropResponder(responder.getId());
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
                        this.dropResponder(responder.getId());
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
                    this.dropResponders();

                    // Peer handshake done
                    this.setState(SignalingState.OPEN);
                    this.getLogger().info("Peer handshake done");

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
    private void handleNewResponder(NewResponder msg) throws ProtocolException, ConnectionException {
        // Validate responder id
        final short id = this.validateResponderId(msg.getId());

        // Check whether responder is already known
        if (this.responders.containsKey(id)) {
            throw new ProtocolException("Got new-responder message for an " +
                                        "already known responder (" + id + ")");
        }

        // Process responder
        this.processNewResponder(id);
    }

    /**
     * Store a new responder.
     *
     * If we trust the responder, send our session key.
     */
    private void processNewResponder(short responderId) throws ConnectionException, ProtocolException {
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
    private void sendKey(Responder responder) throws ProtocolException, ConnectionException {
        final Key msg = new Key(responder.getKeyStore().getPublicKey());
        final byte[] packet = this.buildPacket(msg, responder.getId());
        this.getLogger().debug("Sending key");
        this.send(packet, msg);
        responder.handshakeState = ResponderHandshakeState.KEY_SENT;
    }

    /**
     * A responder repeats our cookie.
     */
    private void handleAuth(ResponderAuth msg, Responder responder, SignalingChannelNonce nonce) throws ProtocolException {
        // Validate cookie
        this.validateRepeatedCookie(msg.getYourCookie());

        // Validation of task list and data already happens in the `ResponderAuth` constructor

        // Select task
        this.task = TaskHelper.chooseCommonTask(this.tasks, msg.getTasks());

        // Remove all other entries from tasks data
        for(Iterator<Map.Entry<String, Map<Object, Object>>> it = this.tasksData.entrySet().iterator(); it.hasNext(); ) {
            if (!it.next().getKey().equals(this.task.getName())) {
                it.remove();
            }
        }

        // OK!
        this.getLogger().debug("Responder 0x" + NaCl.asHex(new int[] { responder.getId() }) + " authenticated");

        // Store cookie
        if (nonce.getCookie().equals(this.cookie)) {
            throw new ProtocolException("Local and remote cookies are equal");
        }
        responder.setCookie(nonce.getCookie());

        // Update state
        responder.handshakeState = ResponderHandshakeState.AUTH_RECEIVED;
    }

    /**
     * Repeat the responder's cookie and choose a task.
     */
    private void sendAuth(Responder responder, SignalingChannelNonce nonce) throws ProtocolException, ConnectionException {
        // Ensure that cookies are different
        if (nonce.getCookie().equals(this.cookie)) {
            throw new ProtocolException("Their cookie and our cookie are the same");
        }

        // Send auth
        final InitiatorAuth msg;
        try {
            msg = new InitiatorAuth(nonce.getCookieBytes(), this.task.getName(), this.tasksData);
        } catch (ValidationError e) {
            throw new ProtocolException("Invalid task data", e);
        }
        final byte[] packet = this.buildPacket(msg, responder.getId());
        this.getLogger().debug("Sending auth");
        this.send(packet, msg);

        // Update state
        responder.handshakeState = ResponderHandshakeState.AUTH_SENT;
    }

    /**
     * Drop specific responder.
     */
    private void dropResponder(short responderId) throws ProtocolException, ConnectionException {
        final DropResponder msg = new DropResponder(responderId);
        final byte[] packet = this.buildPacket(msg, responderId);
        this.getLogger().debug("Sending drop-responder " + responderId);
        this.send(packet, msg);
        this.responders.remove(responderId);
    }

    /**
     * Drop all responders.
     */
    private void dropResponders() throws ProtocolException, ConnectionException {
        this.getLogger().debug("Dropping " + this.responders.size() + " other responders");
        final Set<Short> ids = this.responders.keySet();
        for (short id : ids) {
            this.dropResponder(id);
        }
    }

    @Override
    @Nullable
    protected Short getPeerAddress() {
        if (this.responder != null) {
            return this.responder.getId();
        }
        return null;
    }

    @Override
    @Nullable
    public Cookie getPeerCookie() {
        if (this.responder != null) {
            return this.responder.getCookie();
        }
        return null;
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
     * Return the responder with the specified id, or null if it could not be found.
     */
    @Nullable
    private Responder getResponder(short id) {
        if (this.getState() == SignalingState.OPEN && this.responder != null && this.responder.getId() == id) {
            return this.responder;
        } else if (this.responders.containsKey(id)) {
            return this.responders.get(id);
        }
        return null;
    }

}

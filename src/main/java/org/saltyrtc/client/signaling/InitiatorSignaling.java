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
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.Auth;
import org.saltyrtc.client.messages.DropResponder;
import org.saltyrtc.client.messages.InitiatorServerAuth;
import org.saltyrtc.client.messages.Key;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.NewResponder;
import org.saltyrtc.client.messages.Token;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.ResponderHandshakeState;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

public class InitiatorSignaling extends Signaling {

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger("SaltyRTC.ISignaling");
    }

    // Keep track of responders connected to the server
    protected final Map<Short, Responder> responders = new HashMap<>();

    // Once the handshake is done, this is the chosen responder
    protected Responder responder;

    public InitiatorSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext) {
        super(saltyRTC, host, port, permanentKey, sslContext);
        this.role = SignalingRole.Initiator;
        this.authToken = new AuthToken();
    }

    public InitiatorSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              byte[] responderTrustedKey) {
        super(saltyRTC, host, port, permanentKey, sslContext, responderTrustedKey);
        this.role = SignalingRole.Initiator;
        this.authToken = new AuthToken();
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
            } else if (isResponderId(receiver)) {
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
        } else if (!isResponderId(receiver)) {
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
    protected short validateResponderId(int id) throws ProtocolException {
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
        if (isResponderId(source)) {
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
        final Cookie cookie = new Cookie(msg.getYourCookie());
        if (!cookie.equals(this.cookie)) {
            getLogger().error("Bad repeated cookie in server-auth message");
            getLogger().debug("Their response: " + Arrays.toString(msg.getYourCookie()) +
                              ", our cookie: " + Arrays.toString(this.cookie.getBytes()));
            throw new ProtocolException("Bad repeated cookie in server-auth message");
        }

        // Store responders
        for (int number : msg.getResponders()) {
            final short id = this.validateResponderId(number);
            this.responders.put(id, new Responder(id));
        }
        getLogger().debug(this.responders.size() + " responder(s) connected.");

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
                getLogger().debug("Received new-responder");
                handleNewResponder((NewResponder) msg);
            } else {
                throw new ProtocolException("Got unexpected server message: " + msg.getType());
            }
        } else if (isResponderId(nonce.getSource())) {
            // Get responder instance
            final Responder responder = this.responders.get(nonce.getSource());
            if (responder == null) {
                throw new ProtocolException("Unknown message sender: " + nonce.getSource());
            }

            // Dispatch message
            final Message msg;
            switch (responder.handshakeState) {
                case NEW:
                    // Expect token message, encrypted with authentication token
                    try {
                        payload = this.authToken.decrypt(box);
                    } catch (CryptoFailedException e) {
                        e.printStackTrace();
                        throw new ProtocolException("Could not decrypt token message");
                    }
                    msg = MessageReader.read(payload);
                    if (msg instanceof Token) {
                        getLogger().debug("Received token");
                        handleToken((Token) msg, responder);
                        sendKey(responder);
                    } else {
                        throw new ProtocolException("Expected token message, but got " + msg.getType());
                    }
                    break;
                case TOKEN_RECEIVED:
                    // Expect key message, encrypted with our public permanent key
                    // and responder private permanent key
                    try {
                        payload = this.permanentKey.decrypt(box, responder.permanentKey);
                    } catch (CryptoFailedException | InvalidKeyException e) {
                        e.printStackTrace();
                        throw new ProtocolException("Could not decrypt key message");
                    }

                    msg = MessageReader.read(payload);
                    if (msg instanceof Key) {
                        getLogger().debug("Received key");
                        handleKey((Key) msg, responder);
                        sendAuth(responder, nonce);
                    } else {
                        throw new ProtocolException("Expected key message, but got " + msg.getType());
                    }
                    break;
                case KEY_RECEIVED:
                    // Expect auth message, encrypted with our public session key
                    // and responder private session key
                    try {
                        // Note: The session key related to the responder is
                        // responder.keyStore, not this.sessionKey!
                        payload = responder.getKeyStore().decrypt(box, responder.sessionKey);
                    } catch (CryptoFailedException | InvalidKeyException e) {
                        e.printStackTrace();
                        throw new ProtocolException("Could not decrypt auth message");
                    }

                    msg = MessageReader.read(payload);
                    if (msg instanceof Auth) {
                        getLogger().debug("Received auth");
                        handleAuth((Auth) msg, responder, nonce);
                        dropResponders();
                    } else {
                        throw new ProtocolException("Expected auth message, but got " + msg.getType());
                    }

                    // We're connected!
                    this.setState(SignalingState.OPEN);
                    getLogger().info("Peer handshake done");

                    break;
                default:
                    throw new InternalException("Unknown responder handshake state");
            }
        } else {
            throw new ProtocolException("Message source is neither the server nor a responder");
        }
    }

    /**
     * A new responder wants to connect.
     */
    protected void handleNewResponder(NewResponder msg) throws ProtocolException {
        // Validate responder id
        final short id = this.validateResponderId(msg.getId());

        // Check whether responder is already known
        if (this.responders.containsKey(id)) {
            throw new ProtocolException("Got new-responder message for an " +
                                        "already known responder (" + id + ")");
        }

        // Store responder
        this.responders.put(id, new Responder(id));
    }

    /**
     * A responder sends his public permanent key.
     */
    protected void handleToken(Token msg, Responder responder) {
        responder.setPermanentKey(msg.getKey());
        responder.handshakeState = ResponderHandshakeState.TOKEN_RECEIVED;
    }

    /**
     * Send our public session key to the responder.
     */
    protected void sendKey(Responder responder) throws ProtocolException, ConnectionException {
        final Key msg = new Key(responder.getKeyStore().getPublicKey());
        final byte[] packet = this.buildPacket(msg, responder.getId());
        getLogger().debug("Sending key");
        this.send(packet, msg);
    }

    /**
     * A responder sends his public session key.
     */
    protected void handleKey(Key msg, Responder responder) {
        responder.setSessionKey(msg.getKey());
        responder.handshakeState = ResponderHandshakeState.KEY_RECEIVED;
    }

    /**
     * Repeat the responder's cookie.
     */
    protected void sendAuth(Responder responder, SignalingChannelNonce nonce) throws ProtocolException, ConnectionException {
        // Ensure that cookies are different
        if (nonce.getCookie().equals(this.cookie)) {
            throw new ProtocolException("Their cookie and our cookie are the same");
        }

        // Send auth
        final Auth msg = new Auth(nonce.getCookieBytes());
        final byte[] packet = this.buildPacket(msg, responder.getId());
        getLogger().debug("Sending auth");
        this.send(packet, msg);
    }

    /**
     * A responder repeats our cookie.
     */
    protected void handleAuth(Auth msg, Responder responder, SignalingChannelNonce nonce) throws ProtocolException {
        // Validate cookie
        validateRepeatedCookie(msg);

        // OK!
        getLogger().debug("Responder 0x" + NaCl.asHex(new int[] { responder.getId() }) + " authenticated");

        // Store responder details and session key
        this.responder = responder;
        this.sessionKey = responder.getKeyStore();

        // Store cookie
        if (nonce.getCookie().equals(this.cookie)) {
            throw new ProtocolException("Local and remote cookies are equal");
        }
        this.responder.setCookie(nonce.getCookie());

        // Remove responder from responders list
        this.responders.remove(responder.getId());
    }

    /**
     * Drop specific responder.
     */
    protected void dropResponder(short responderId) throws ProtocolException, ConnectionException {
        final DropResponder msg = new DropResponder(responderId);
        final byte[] packet = this.buildPacket(msg, responderId);
        getLogger().debug("Sending drop-responder " + responderId);
        this.send(packet, msg);
        this.responders.remove(responderId);
    }

    /**
     * Drop all responders.
     */
    protected void dropResponders() throws ProtocolException, ConnectionException {
        getLogger().debug("Dropping " + this.responders.size() + " other responders");
        final Set<Short> ids = this.responders.keySet();
        for (short id : ids) {
            dropResponder(id);
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
    @Nullable protected Responder getResponder(short id) {
        if (this.getState() == SignalingState.OPEN && this.responder != null && this.responder.getId() == id) {
            return this.responder;
        } else if (this.responders.containsKey(id)) {
            return this.responders.get(id);
        }
        return null;
    }

}

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
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InternalServerException;
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
import org.saltyrtc.client.messages.ClientHello;
import org.saltyrtc.client.messages.Key;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.NewInitiator;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.Token;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.InitiatorHandshakeState;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;

import java.util.Arrays;

import javax.net.ssl.SSLContext;

public class ResponderSignaling extends Signaling {

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger("SaltyRTC.RSignaling");
    }

    private Initiator initiator;
    private AuthToken authToken;

    public ResponderSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              byte[] initiatorPublicKey, byte[] authToken)
                              throws java.security.InvalidKeyException {
        super(saltyRTC, host, port, permanentKey, sslContext);
        this.initiator = new Initiator(initiatorPublicKey);
        this.authToken = new AuthToken(authToken);
    }

    /**
     * The responder needs to use the initiator public permanent key as connection path.
     */
    protected String getWebsocketPath() {
        return NaCl.asHex(this.initiator.getPermanentKey());
    }

    @Override
    protected CombinedSequence getNextCsn(short receiver) throws ProtocolException {
        try {
            if (receiver == Signaling.SALTYRTC_ADDR_SERVER) {
                return this.serverCsn.next();
            } else if (receiver == Signaling.SALTYRTC_ADDR_INITIATOR) {
                return this.initiator.getCsn().next();
            } else if (isResponderId(receiver)) {
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
        if (isResponderId(receiver)) {
            throw new ProtocolException("Bad receiver byte: " + receiver);
        } else if (receiver != Signaling.SALTYRTC_ADDR_INITIATOR) {
            throw new ProtocolException("Responder may not encrypt messages for other responders: " + receiver);
        }
        switch (messageType) {
            case "token":
                return this.authToken.encrypt(payload, nonce);
            case "key":
                return this.permanentKey.encrypt(payload, nonce, this.initiator.permanentKey);
            default:
                byte[] peerSessionKey = getPeerSessionKey();
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
        getLogger().debug("Sending client-hello");
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
        // TODO: validate nonce
        if (nonce.getDestination() > 0xff || nonce.getDestination() < 0x02) {
            throw new ProtocolException("Invalid nonce destination: " + nonce.getDestination());
        }
        this.address = nonce.getDestination();
        getLogger().debug("Server assigned address 0x" + NaCl.asHex(new int[] { this.address }));

        // Validate cookie
        final Cookie cookie = new Cookie(msg.getYourCookie());
        if (!cookie.equals(this.cookiePair.getOurs())) {
            getLogger().error("Bad repeated cookie in server-auth message");
            getLogger().debug("Their response: " + Arrays.toString(msg.getYourCookie()) +
                    ", our cookie: " + Arrays.toString(this.cookiePair.getOurs().getBytes()));
            throw new ProtocolException("Bad repeated cookie in server-auth message");
        }

        // Store whether initiator is connected
        this.initiator.setConnected(msg.isInitiatorConnected());
        getLogger().debug("Initiator is " + (msg.isInitiatorConnected() ? "" : "not ") + "connected.");

        // Server handshake is done!
        this.serverHandshakeState = ServerHandshakeState.DONE;
    }

    @Override
    protected void initPeerHandshake() throws ProtocolException, ConnectionException {
        if (this.initiator.isConnected()) {
            this.sendToken();
        }
    }

    protected void sendToken() throws ProtocolException, ConnectionException {
        final Token msg = new Token(this.permanentKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_INITIATOR);
        getLogger().debug("Sending token");
        this.send(packet, msg);
        this.initiator.handshakeState = InitiatorHandshakeState.TOKEN_SENT;
    }

    @Override
    protected void onPeerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ProtocolException, ValidationError, SerializationError,
                   InternalServerException, ConnectionException {

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
                getLogger().debug("Received new-initiator");
                handleNewInitiator((NewInitiator) msg);
            } else {
                throw new ProtocolException("Got unexpected server message: " + msg.getType());
            }
        } else if (nonce.getSource() == SALTYRTC_ADDR_INITIATOR) {
            // Decrypt. The key messages are encrypted with a different key than the rest.
            if (this.initiator.handshakeState == InitiatorHandshakeState.TOKEN_SENT) {
                // Expect a key message, encrypted with the permanent keys
                try {
                    payload = this.permanentKey.decrypt(box, this.initiator.getPermanentKey());
                } catch (CryptoFailedException | InvalidKeyException e) {
                    e.printStackTrace();
                    throw new ProtocolException("Could not decrypt key message");
                }
            } else {
                // Otherwise, it must be encrypted with the session key.
                try {
                    payload = this.sessionKey.decrypt(box, this.initiator.getSessionKey());
                } catch (CryptoFailedException | InvalidKeyException e) {
                    e.printStackTrace();
                    throw new ProtocolException("Could not decrypt message using session key");
                }
            }

            // Dispatch message
            final Message msg = MessageReader.read(payload);
            switch (this.initiator.handshakeState) {
                case NEW:
                    throw new ProtocolException("Unexpected " + msg.getType() + " message");
                case TOKEN_SENT:
                    // Expect a key message
                    if (msg instanceof Key) {
                        getLogger().debug("Received key");
                        handleKey((Key) msg);
                        sendKey();
                    } else {
                        throw new ProtocolException("Expected key message, but got " + msg.getType());
                    }
                    break;
                case KEY_SENT:
                    // Expect an auth message
                    if (msg instanceof Auth) {
                        getLogger().debug("Received auth");
                        handleAuth((Auth) msg);
                        sendAuth(nonce);
                    } else {
                        throw new ProtocolException("Expected auth message, but got " + msg.getType());
                    }

                    // We're connected!
                    this.setState(SignalingState.OPEN);
                    getLogger().info("Peer handshake done");

                    break;
                default:
                    throw new InternalServerException("Unknown initiator handshake state");
            }
        } else {
            throw new ProtocolException("Message source is neither the server nor the initiator");
        }
    }

    /**
     * A new responder wants to connect.
     */
    protected void handleNewInitiator(NewInitiator msg) throws ProtocolException, ConnectionException {
        // Initiator changed, send token
        this.sendToken();
    }

    /**
     * The initiator sends his public session key.
     */
    protected void handleKey(Key msg) {
        this.initiator.setSessionKey(msg.getKey());
    }

    /**
     * Send our public session key to the initiator.
     */
    protected void sendKey() throws ProtocolException, ConnectionException {
        // Generate our own session key
        this.sessionKey = new KeyStore();
        final Key msg = new Key(this.sessionKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, SALTYRTC_ADDR_INITIATOR);
        getLogger().debug("Sending key");
        this.send(packet, msg);
        this.initiator.handshakeState = InitiatorHandshakeState.KEY_SENT;
    }

    /**
     * The initiator repeats our cookie.
     */
    protected void handleAuth(Auth msg) throws ProtocolException {
        // Validate cookie
        validateRepeatedCookie(msg);

        // OK!
        getLogger().debug("Initiator authenticated");
    }

    /**
     * Repeat the initiator's cookie.
     */
    protected void sendAuth(SignalingChannelNonce nonce) throws ProtocolException, ConnectionException {
        // Ensure that cookies are different
        if (nonce.getCookie().equals(this.cookiePair.getOurs())) {
            throw new ProtocolException("Their cookie and our cookie are the same");
        }

        // Send auth
        final Auth msg = new Auth(nonce.getCookieBytes());
        final byte[] packet = this.buildPacket(msg, SALTYRTC_ADDR_INITIATOR);
        getLogger().debug("Sending auth");
        this.send(packet, msg);
    }

    @Override
    protected Short getPeerAddress() {
        if (this.initiator != null) {
            return this.initiator.getId();
        }
        return null;
    }

    @Override
    protected byte[] getPeerSessionKey() {
        if (this.initiator != null) {
            return this.initiator.sessionKey;
        }
        return null;
    }


}

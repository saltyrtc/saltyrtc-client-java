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
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InternalServerException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.OverflowException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.ClientHello;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.Token;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.InitiatorHandshakeState;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.slf4j.Logger;

import java.util.Arrays;

import javax.net.ssl.SSLContext;

public class ResponderSignaling extends Signaling {

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger(ResponderSignaling.class);
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
                if (this.initiator.sessionKey == null) {
                    throw new ProtocolException(
                            "Trying to encrypt for initiator using session key, but session key is null");
                }
                return this.permanentKey.encrypt(payload, nonce, this.initiator.sessionKey);
        }
    }

    @Override
    protected void sendClientHello() throws ProtocolException {
        final ClientHello msg = new ClientHello(this.permanentKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_SERVER, false);
        getLogger().debug("Sending client-hello");
        this.ws.send(packet);
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
        getLogger().debug("Server assigned address " + NaCl.asHex(new int[] { this.address }));

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
    protected void initPeerHandshake() throws ProtocolException {
        if (this.initiator.isConnected()) {
            this.sendToken();
        }
    }

    protected void sendToken() throws ProtocolException {
        final Token msg = new Token(this.permanentKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_INITIATOR);
        getLogger().debug("Sending token");
        this.ws.send(packet);
        this.initiator.handshakeState = InitiatorHandshakeState.TOKEN_SENT;
    }

    @Override
    protected void onPeerHandshakeMessage(Box box, SignalingChannelNonce nonce)
            throws ProtocolException, ValidationError, SerializationError, InternalServerException {

    }

}
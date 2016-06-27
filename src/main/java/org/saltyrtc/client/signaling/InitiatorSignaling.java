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
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.OverflowException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.InitiatorServerAuth;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLContext;

public class InitiatorSignaling extends Signaling {

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger(InitiatorSignaling.class);
    }

    // Keep track of responders connected to the server
    private Map<Short, Responder> responders = new HashMap<>();

    // Once the handshake is done, this is the chosen responder
    private Responder responder;

    public InitiatorSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext) {
        super(saltyRTC, host, port, permanentKey, sslContext);
        this.authToken = new AuthToken();
    }

    /**
     * The initiator needs to use its own public permanent key as connection path.
     */
    protected String getWebsocketPath() {
        return NaCl.asHex(this.permanentKey.getPublicKey());
    }

    @Override
    protected CombinedSequence getNextCsn(short receiver) throws ProtocolException {
        try {
            if (receiver == Signaling.SALTYRTC_ADDR_SERVER) {
                return this.serverCsn.next();
            } else if (receiver == Signaling.SALTYRTC_ADDR_INITIATOR) {
                throw new ProtocolException("Initiator cannot send messages to initiator");
            } else if (isResponderByte(receiver)) {
                if (this.state == SignalingState.OPEN) { // TODO maybe use peerHandshakeState instead
                    assert this.responder != null;
                    return this.responder.getCsn().next();
                } else if (this.responders.containsKey(receiver)) {
                    return this.responders.get(receiver).getCsn().next();
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
        if (receiver == Signaling.SALTYRTC_ADDR_INITIATOR) {
            throw new ProtocolException("Initiator cannot encrypt messages for initiator");
        } else if (!isResponderByte(receiver)) {
            throw new ProtocolException("Bad receiver byte: " + receiver);
        }

        // Find correct responder
        final Responder responder;
        if (this.state == SignalingState.OPEN) { // TODO maybe use peerHandshakeState instead
            assert this.responder != null;
            responder = this.responder;
        } else if (this.responders.containsKey(receiver)) {
            responder = this.responders.get(receiver);
        } else {
            throw new ProtocolException("Unknown responder: " + receiver);
        }

        // Encrypt
        if (Objects.equals(messageType, "key")) {
            return this.permanentKey.encrypt(payload, nonce, responder.getPermanentKey());
        } else {
            return responder.getKeyStore().encrypt(payload, nonce, responder.getSessionKey());
        }
    }

    @Override
    protected void sendClientHello() throws ProtocolException {
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

        // Set proper address
        this.address = Signaling.SALTYRTC_ADDR_INITIATOR;
        // TODO: validate nonce

        // Validate cookie
        final Cookie cookie = new Cookie(msg.getYourCookie());
        if (!cookie.equals(this.cookiePair.getOurs())) {
            getLogger().error("Bad repeated cookie in server-auth message");
            getLogger().debug("Their response: " + Arrays.toString(msg.getYourCookie()) +
                              ", our cookie: " + Arrays.toString(this.cookiePair.getOurs().getBytes()));
            throw new ProtocolException("Bad repeated cookie in server-auth message");
        }

        // Store responders
        for (int id : msg.getResponders()) {
            if (id < 0) {
                throw new ProtocolException("Responder id may not be smaller than 0");
            } else if (id > 0xff) {
                throw new ProtocolException("Responder id may not be larger than 255");
            }
            this.responders.put((short) id, new Responder((short) id));
        }
        getLogger().debug(this.responders.size() + " responder(s) connected.");

        // Server handshake is done!
        this.serverHandshakeState = ServerHandshakeState.DONE;
    }

    @Override
    protected void initPeerHandshake() {
        // No-op as initiator.
    }

}
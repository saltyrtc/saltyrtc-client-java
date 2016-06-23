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
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.OverflowException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.messages.ClientHello;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.slf4j.Logger;

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
    protected void sendClientHello() throws ProtocolException {
        final ClientHello msg = new ClientHello(this.permanentKey.getPublicKey());
        final byte[] packet = this.buildPacket(msg, Signaling.SALTYRTC_ADDR_SERVER, false);
        getLogger().debug("Sending client-hello");
        this.ws.send(packet);
    }

    @Override
    protected CombinedSequence getNextCsn(short receiver) throws ProtocolException {
        try {
            if (receiver == Signaling.SALTYRTC_ADDR_SERVER) {
                return this.serverCsn.next();
            } else if (receiver == Signaling.SALTYRTC_ADDR_INITIATOR) {
                return this.initiator.getCsn().next();
            } else if (isResponderByte(receiver)) {
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
        if (isResponderByte(receiver)) {
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

}
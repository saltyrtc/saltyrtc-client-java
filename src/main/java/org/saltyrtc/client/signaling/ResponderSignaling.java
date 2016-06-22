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
import org.saltyrtc.client.keystore.KeyStore;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;

public class ResponderSignaling extends Signaling {

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger(ResponderSignaling.class);
    }

    private Initiator initiator;
    private byte[] authToken;

    public ResponderSignaling(SaltyRTC saltyRTC, String host, int port,
                              KeyStore permanentKey, SSLContext sslContext,
                              byte[] initiatorPublicKey, byte[] authToken) {
        super(saltyRTC, host, port, permanentKey, sslContext);
        this.initiator = new Initiator(initiatorPublicKey);
        this.authToken = authToken;
    }

    /**
     * The responder needs to use the initiator public permanent key as connection path.
     */
    protected String getWebsocketPath() {
        return NaCl.asHex(this.initiator.getPermanentKey());
    }

}
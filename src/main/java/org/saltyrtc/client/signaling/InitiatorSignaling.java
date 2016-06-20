/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.keystore.KeyStore;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class InitiatorSignaling extends Signaling {

    // Logging
    protected Logger getLogger() {
        return org.slf4j.LoggerFactory.getLogger(InitiatorSignaling.class);
    }

    // Keep track of responders connected to the server
    private Map<Integer, Responder> responders = new HashMap<>();

    // Once the handshake is done, this is the chosen responder
    private Responder responder;

    public InitiatorSignaling(SaltyRTC saltyRTC, String host, int port, KeyStore permanentKey) {
        super(saltyRTC, host, port, permanentKey);
    }
}
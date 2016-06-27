/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.state.ResponderHandshakeState;

/**
 * Information about a responder. Used by initiator during handshake.
 */
public class Responder extends Peer {
    private short id;
    private KeyStore keyStore;
    public ResponderHandshakeState handshakeState;

    public Responder(short id) {
        super();
        this.id = id;
        this.keyStore = new KeyStore();
        this.handshakeState = ResponderHandshakeState.NEW;
    }

    public short getId() {
        return id;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

}

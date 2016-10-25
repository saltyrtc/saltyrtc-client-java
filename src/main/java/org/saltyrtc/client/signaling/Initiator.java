/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.signaling.state.InitiatorHandshakeState;

/**
 * Information about the initiator. Used by responder during handshake.
 */
public class Initiator extends Peer {
    private static short ID = 0x01;

    private boolean connected;

    public InitiatorHandshakeState handshakeState;

    public Initiator(byte[] permanentKey) {
        super(Initiator.ID, permanentKey);
        this.connected = false;
        this.handshakeState = InitiatorHandshakeState.NEW;
    }

    public short getId() {
        return 0x01;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}

/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling.peers;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.signaling.state.ResponderHandshakeState;

/**
 * Information about a responder. Used by initiator during handshake.
 */
public class Responder extends Peer {
    public ResponderHandshakeState handshakeState;
    private int counter;

    /**
     * Create a new `Responder` object.
     *
     * @param id The responder ID as specified by the SaltyRTC protocol.
     * @param counter A counter used to identify the oldest responder during the path cleaning procedure.
     */
    public Responder(short id, int counter) {
        super(id);
        this.counter = counter;
        this.handshakeState = ResponderHandshakeState.NEW;
    }

    @NonNull
    @Override
    public String getName() {
        return "Responder " + this.getId();
    }

    public int getCounter() {
        return this.counter;
    }
}

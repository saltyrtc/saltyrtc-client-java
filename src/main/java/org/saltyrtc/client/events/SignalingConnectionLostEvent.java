/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

/**
 * The signaling server lost connection to the specified peer.
 */
public class SignalingConnectionLostEvent implements Event {
    private short receiver;

    public SignalingConnectionLostEvent(short receiver) {
        this.receiver = receiver;
    }

    public short getReceiver() {
        return receiver;
    }
}

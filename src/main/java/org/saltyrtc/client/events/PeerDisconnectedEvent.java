/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

/**
 * A previously authenticated peer has disconnected from the server.
 */
public class PeerDisconnectedEvent implements Event {
    private short peerId;

    public PeerDisconnectedEvent(short id) {
        this.peerId = id;
    }

    public short getPeerId() {
        return peerId;
    }
}

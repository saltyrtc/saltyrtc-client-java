/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling.state;

public enum SignalingState {
    NEW,
    WS_CONNECTING,
    SERVER_HANDSHAKE,
    PEER_HANDSHAKE,
    TASK,
    CLOSING,
    CLOSED,
    ERROR,
}

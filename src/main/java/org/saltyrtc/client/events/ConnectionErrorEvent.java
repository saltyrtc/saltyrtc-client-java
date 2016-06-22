/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

import org.saltyrtc.client.signaling.SignalingChannel;

/**
 * A connection error occured
 */
public class ConnectionErrorEvent implements Event {

    private SignalingChannel channel;

    public SignalingChannel getChannel() {
        return channel;
    }

    public ConnectionErrorEvent(SignalingChannel channel) {
        this.channel = channel;
    }

}

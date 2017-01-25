/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

import org.saltyrtc.client.signaling.state.SignalingState;

/**
 * The signaling state has changed.
 */
public class SignalingStateChangedEvent implements Event {

    private final SignalingState state;

    public SignalingStateChangedEvent(SignalingState state) {
        this.state = state;
    }

    public SignalingState getState() {
        return this.state;
    }
}

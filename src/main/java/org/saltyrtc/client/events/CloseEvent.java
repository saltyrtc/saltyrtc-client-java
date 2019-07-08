/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

import org.saltyrtc.client.signaling.CloseCode;

/**
 * The connection is closed.
 */
public class CloseEvent implements Event {

    private final CloseCode reason;

    public CloseEvent(CloseCode reason) {
        this.reason = reason;
    }

    public CloseCode getReason() {
        return this.reason;
    }
}

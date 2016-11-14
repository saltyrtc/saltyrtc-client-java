/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

/**
 * Application data was received.
 */
public class ApplicationDataEvent implements Event {

    private final Object data;

    public ApplicationDataEvent(Object data) {
        this.data = data;
    }

    public Object getData() {
        return this.data;
    }

}

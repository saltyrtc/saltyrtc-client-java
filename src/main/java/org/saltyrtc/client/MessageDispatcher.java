/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

public class MessageDispatcher<ML> {
    protected ML listener = null;

    protected ML getListener() {
        return listener;
    }

    // Ex protected
    public void setListener(ML messageListener) {
        this.listener = messageListener;
    }

    public void removeListener(ML messageListener) {
        if (this.listener == messageListener) {
            this.listener = null;
        }
    }
}

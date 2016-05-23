/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.json.JSONObject;

public class DataChannelMessageDispatcher extends MessageDispatcher<DataChannel.MessageListener> {
    protected void message(final JSONObject message) {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onMessage(message);
                }
            }
        });
    }
}
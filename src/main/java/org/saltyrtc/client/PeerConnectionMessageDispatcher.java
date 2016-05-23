/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class PeerConnectionMessageDispatcher extends MessageDispatcher<PeerConnection.MessageListener> {
    protected void answer(final SessionDescription description) {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onAnswer(description);
                }
            }
        });
    }

    protected void candidate(final IceCandidate candidate) {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onCandidate(candidate);
                }
            }
        });
    }
}

package org.saltyrtc.client;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class SignalingMessageDispatcher extends MessageDispatcher<Signaling.MessageListener> {
    protected void reset() {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onReset();
                }
            }
        });
    }

    protected void sendError() {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onSendError();
                }
            }
        });
    }

    protected void offer(
            final SessionDescription description,
            final String offer
    ) {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    // Note: This is a workaround to prevent message reordering. Do not move the next
                    //       line of code somewhere else unless you know EXACTLY what you are doing.
                    Session.set(offer);
                    listener.onOffer(description);
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

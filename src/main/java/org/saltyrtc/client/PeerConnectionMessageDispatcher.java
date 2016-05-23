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

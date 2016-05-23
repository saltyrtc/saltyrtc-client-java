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

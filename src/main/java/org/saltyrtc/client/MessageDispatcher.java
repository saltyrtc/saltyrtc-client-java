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

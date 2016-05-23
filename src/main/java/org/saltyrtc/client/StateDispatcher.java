package org.saltyrtc.client;

import java.util.HashSet;

public class StateDispatcher {
    protected final HashSet<InternalStateListener> listeners = new HashSet<>();

    protected void state(final String state) {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                for (InternalStateListener listener : listeners) {
                    listener.handleState(state);
                }
            }
        });
    }

    protected void error(final String state, final String error) {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                for (InternalStateListener listener : listeners) {
                    listener.handleError(state, error);
                }
            }
        });
    }

    // Ex protected
    public void addListener(InternalStateListener listener) {
        this.listeners.add(listener);
    }

    // Ex protected
    public void removeListener(InternalStateListener listener) {
        this.listeners.remove(listener);
    }
}

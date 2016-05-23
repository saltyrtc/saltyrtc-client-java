package org.saltyrtc.client;

import java.util.HashSet;

/**
 * States have a name, type and value. This class will also notify state listeners.
 * Note: Public methods can be used safely from any thread.
 */
public class State {
    public final String name;
    protected int type;
    protected String value;
    protected final HashSet<StateListener> listeners = new HashSet<>();

    public State(String name) {
        this.name = name;
    }

    // Ex protected
    public void addListener(StateListener listener) {
        this.listeners.add(listener);
        // Send initial values (if any)
        this.notifyListener(listener);
    }

    // Ex protected
    public void removeListener(StateListener listener) {
        this.listeners.remove(listener);
    }

    // Ex protected
    public void notifyListener(final StateListener listener) {
       this.notify(listener);
    }

    // Ex protected
    public void notifyListeners() {
        this.notify(null);
    }

    protected void notify(final StateListener listener_) {
        if (this.value == null) {
            return;
        }

        final int type = this.type;
        final String value = this.value;

        // Broadcast
        Handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener_ == null) {
                    // Notify all listeners
                    for (StateListener listener : listeners) {
                        listener.onState(type, value);
                    }
                } else {
                    // Notify a single listener
                    listener_.onState(type, value);
                }
            }
        });
    }
}

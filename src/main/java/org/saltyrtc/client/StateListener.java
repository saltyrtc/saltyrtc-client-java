package org.saltyrtc.client;

/**
 * Listener for state change events of an abstract state instance.
 */
public interface StateListener {
    void onState(int type, String value);
}

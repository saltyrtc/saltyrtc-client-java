package org.saltyrtc.client;

import java.util.HashMap;

/**
 * Summarises various general states to simplify recognition of the current state.
 */
public class ClientState extends State {
    public class StateValue {
        public final static String RESET = "reset";
        public final static String TIMEOUT = "timeout";
        public final static String LOST = "lost";
        public final static String CONNECTED = "connected";
        public final static String UNSTABLE = "unstable";
        public final static String DISCONNECTED = "disconnected";
    }

    public ClientState(String name) {
        super(name);
        this.type = StateType.DANGER;
        this.value = StateValue.DISCONNECTED;
    }

    public void update(int type, String value) {
        this.type = type;
        this.value = value;

        // Broadcast
        this.notifyListeners();
    }

    public void update(HashMap<String, InternalState> states) {
        int weight = 0;

        // Calculate client state type and value
        for (InternalState state : states.values()) {
            weight += state.type;
        }

        // Data channel open and PC at most unstable: Force warning if danger
        if (weight >= StateType.DANGER
                && states.get("dc").type == StateType.SUCCESS
                && states.get("pc").type != StateType.DANGER) {
            weight = StateType.WARNING;
        }

        // Calculate state type
        if (weight < StateType.WARNING) {
            this.type = StateType.SUCCESS;
            this.value = StateValue.CONNECTED;
        } else if (weight < StateType.DANGER) {
            this.type = StateType.WARNING;
            this.value = StateValue.UNSTABLE;
        } else {
            this.type = StateType.DANGER;
            this.value = StateValue.DISCONNECTED;
        }

        // Broadcast
        this.notifyListeners();
    }
}

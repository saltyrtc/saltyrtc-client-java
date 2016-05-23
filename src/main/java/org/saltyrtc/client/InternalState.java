package org.saltyrtc.client;

import android.util.Log;

import java.util.HashMap;

/**
 * Contains type and value of a specific state instance.
 * Note: Public methods can be used safely from any thread.
 */
public class InternalState extends State {
    protected final HashMap<String, Integer> rules = new HashMap<>();

    public InternalState(String name) {
        super(name);
        this.reset();
    }

    protected void reset() {
        this.type = StateType.DANGER;
        this.value = "unknown";
    }

    protected void addRules(HashMap<String, Integer> rules) {
        this.rules.putAll(rules);
    }

    protected void update(String value) {
        // Find state type for value in rules
        Integer type = this.rules.get(value);
        if (type == null) {
            Log.w(name, "Unknown state '" + value + "' for " + this.toString());
            Log.d(name, "Rules: " + this.rules.toString());
            type = StateType.DANGER;
            value = "unknown";
        }

        // Update type and value
        this.type = type;
        this.value = value;

        // Broadcast
        this.notifyListeners();
    }
}

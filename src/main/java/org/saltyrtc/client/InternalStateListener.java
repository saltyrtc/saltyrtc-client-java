package org.saltyrtc.client;

import android.util.Log;

import java.util.HashMap;

/**
 * Listener for state change events.
 */
public abstract class InternalStateListener {
    protected static final String NAME = "InternalStateListener";
    protected final HashMap<String, StateHandler> stateHandler = new HashMap<>();
    protected final HashMap<String, ErrorStateHandler> errorHandler = new HashMap<>();

    /**
     * Will be called for all state changes.
     * @param state A state.
     */
    public abstract void onState(final String state);

    /**
     * Will always be called for all error states.
     * @param state An error state.
     * @param error An error message.
     */
    public abstract void onError(final String state, final String error);

    /**
     * Will pass the state to onState first.
     * Checks if a state handler for a specific state exists and invokes it.
     */
    protected void handleState(String state) {
        this.onState(state);
        if (this.stateHandler != null && this.stateHandler.containsKey(state)) {
            Log.d(NAME, "Calling handler for state: " + state);
            final StateHandler handler = this.stateHandler.get(state);

            // Handle state in runnable
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    handler.handle();
                }
            });
        }
    }

    /**
     * Will pass the error state to onError first.
     * Checks if a error state handler for a specific error state exists and invokes it.
     */
    protected void handleError(String state, final String error) {
        this.onError(state, error);
        if (this.errorHandler != null && this.errorHandler.containsKey(state)) {
            Log.d(NAME, "Calling handler for error state: " + state);
            final ErrorStateHandler handler = this.errorHandler.get(state);

            // Handle error state in runnable
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    handler.handle(error);
                }
            });
        }
    }

    /**
     * Add a state handler. State handlers will be executed in the event looper.
     * @param state The state the handler will be applied on.
     * @param handler State handler instance.
     */
    protected void addStateHandler(String state, StateHandler handler) {
        this.stateHandler.put(state, handler);
    }

    protected void removeStateHandler(String state) {
        this.stateHandler.remove(state);
    }

    /**
     * Add an error state handler. Error state handlers will be executed in the event looper.
     * @param state The error state the handler will be applied on.
     * @param handler Error state handler instance.
     */
    protected void addErrorHandler(String state, ErrorStateHandler handler) {
        this.errorHandler.put(state, handler);
    }

    protected void removeErrorHandler(String state) {
        this.errorHandler.remove(state);
    }
}

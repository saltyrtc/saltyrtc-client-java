package org.saltyrtc.client.tests;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.tasks.Task;

import java.util.Map;

/**
 * A dummy task that doesn't do anything.
 */
public class DummyTask implements Task {

    public boolean initialized = false;
    public Map<Object, Object> peerData;

    @Override
    public void init(Map<Object, Object> data) {
        this.peerData = data;
        this.initialized = true;
    }

    @Override
    public void onTaskMessage(Map<String, Object> message) {
        // Do nothing
    }

    @Override
    public void sendSignalingMessage(byte[] payload) {
        // Do nothing
    }

    @NonNull
    @Override
    public String getName() {
        return "dummy.tasks.saltyrtc.org";
    }

    @Nullable
    @Override
    public Map<Object, Object> getData() {
        return null;
    }

}

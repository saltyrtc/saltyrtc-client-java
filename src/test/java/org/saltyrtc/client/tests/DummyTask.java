package org.saltyrtc.client.tests;

import org.saltyrtc.client.tasks.Task;

import java.util.Map;

/**
 * A dummy task that doesn't do anything.
 */
public class DummyTask implements Task {

    @Override
    public void onTaskMessage(Map<String, Object> message) {
    }

    @Override
    public void sendSignalingMessage(byte[] payload) {
    }

}

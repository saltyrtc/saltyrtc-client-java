package org.saltyrtc.client.tests;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.signaling.Signaling;
import org.saltyrtc.client.signaling.SignalingInterface;
import org.saltyrtc.client.tasks.Task;
import org.slf4j.Logger;

import java.util.Map;

/**
 * A dummy task that doesn't do anything.
 */
public class DummyTask implements Task {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.DummyTask");

    public boolean initialized = false;
    public Map<Object, Object> peerData;
    private SignalingInterface signaling;

    @Override
    public void init(SignalingInterface signaling, Map<Object, Object> data) {
        this.peerData = data;
        this.initialized = true;
        this.signaling = signaling;
    }

    @Override
    public void onTaskMessage(Map<String, Object> message) {
        LOG.info("Got new task message");
    }

    @Override
    public void sendSignalingMessage(byte[] payload) {
        LOG.info("Sending signaling message (" + payload.length + " bytes)");
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

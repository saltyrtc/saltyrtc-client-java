package org.saltyrtc.client.tests;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SignalingException;
import org.saltyrtc.client.messages.c2c.TaskMessage;
import org.saltyrtc.client.signaling.SignalingInterface;
import org.saltyrtc.client.signaling.SignalingRole;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingPongTask extends DummyTask {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.PingPongTask");

    public boolean sentPong = false;
    public boolean receivedPong = false;

    public PingPongTask() {
        this.name = "pingpong.tasks.saltyrtc.org";
    }

    @Override
    public void init(SignalingInterface signaling, Map<Object, Object> data) {
        super.init(signaling, data);
    }

    @Override
    public void onPeerHandshakeDone() {
        if (this.signaling.getRole() == SignalingRole.Initiator) {
            this.sendPing();
        }
    }

    @NonNull
    @Override
    public List<String> getSupportedMessageTypes() {
        final List<String> types = new ArrayList<>();
        types.add("ping");
        types.add("pong");
        return types;
    }

    private void sendPing() {
        LOG.info("Sending ping");
        TaskMessage msg = new TaskMessage("ping", new HashMap<String, Object>());
        try {
            this.signaling.sendTaskMessage(msg);
        } catch (ProtocolException | SignalingException | ConnectionException e) {
            e.printStackTrace();
            // TODO: Handling for task send errors
        }
    }

    private void sendPong() {
        LOG.info("Sending pong");
        TaskMessage msg = new TaskMessage("pong", new HashMap<String, Object>());
        try {
            this.signaling.sendTaskMessage(msg);
        } catch (ProtocolException | SignalingException | ConnectionException e) {
            e.printStackTrace();
            // TODO: Handling for task send errors
        }
        this.sentPong = true;
    }

    @Override
    public void onTaskMessage(TaskMessage message) {
        if (message.getType().equals("ping")) {
            LOG.info("Received ping");
            LOG.info("Sending pong");
            this.sendPong();
        } else if (message.getType().equals("pong")) {
            LOG.info("Received pong");
            this.receivedPong = true;
        }
    }
}

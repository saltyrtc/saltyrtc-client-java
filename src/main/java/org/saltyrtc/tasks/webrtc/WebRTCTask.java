package org.saltyrtc.tasks.webrtc;

import org.saltyrtc.client.tasks.Task;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * WebRTC Task.
 *
 * This task uses the end-to-end encryption techniques of SaltyRTC to set up a secure WebRTC
 * peer-to-peer connection. It also adds another security layer for data channels that are available
 * to users. The signalling channel will persist after being handed over to a dedicated data channel
 * once the peer-to-peer connection has been set up. Therefore, further signalling communication
 * between the peers does not require a dedicated WebSocket connection over a SaltyRTC server.
 */
public class WebRTCTask implements Task {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.WebRTC");

    // Constants as defined by the specification
    private static final String PROTOCOL_NAME = "v0.webrtc.tasks.saltyrtc.org";
    private static final Integer MAX_PACKET_SIZE = 16384;

    // Data fields
    private static final String FIELD_EXCLUDE = "exclude";
    private static final String FIELD_MAX_PACKET_SIZE = "max_packet_size";

    @Override
    public void onTaskMessage(Map<String, Object> message) {
        LOG.info("New task message arrived");
    }

    @Override
    public void sendSignalingMessage(byte[] payload) {
        LOG.info("TODO: Send signaling message");
    }

    @Override
    public String getName() {
        return WebRTCTask.PROTOCOL_NAME;
    }

    @Override
    public Map<Object, Object> getData() {
        final Map<Object, Object> map = new HashMap<>();
        map.put(WebRTCTask.FIELD_EXCLUDE, new ArrayList<Integer>());
        map.put(WebRTCTask.FIELD_MAX_PACKET_SIZE, WebRTCTask.MAX_PACKET_SIZE);
        return map;
    }
}

package org.saltyrtc.tasks.webrtc;

import org.saltyrtc.client.tasks.Task;
import org.slf4j.Logger;

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

    @Override
    public void onTaskMessage(Map<String, Object> message) {
        LOG.info("New task message arrived");
    }

    @Override
    public void sendSignalingMessage(byte[] payload) {
        LOG.info("TODO: Send signaling message");
    }

}

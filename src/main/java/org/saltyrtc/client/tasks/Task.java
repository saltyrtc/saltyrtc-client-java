package org.saltyrtc.client.tasks;

import java.util.Map;

/**
 * A SaltyRTC task is a protocol extension to this protocol that will be negotiated during the
 * client-to-client authentication phase. Once a task has been negotiated and the authentication is
 * complete, the task protocol defines further procedures, messages, etc.
 *
 * All tasks need to implement this interface.
 */
public interface Task {

    /**
     * This method is called by SaltyRTC when a task related message
     * arrives through the WebSocket.
     *
     * @param message The deserialized MessagePack message.
     */
    void onTaskMessage(Map<String, Object> message);

	/**
     * Send bytes through the task signaling channel.
     *
     * This method should only be called after the handover.
     */
    void sendSignalingMessage(byte[] payload);

    /**
     * Return the task protocol name.
     */
    String getName();

	/**
	 * Return the task data used for negotiation in the `auth` message.
     */
    Map<Object, Object> getData();

}

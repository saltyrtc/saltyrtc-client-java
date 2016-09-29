package org.saltyrtc.client.tasks;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.exceptions.ValidationError;

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
     * Initialize the task with the task data from the peer.
     *
     * The task should keep track internally whether it has been initialized or not.
     *
     * @param data The data sent by the peer in the 'auth' message.
     */
    void init(Map<Object, Object> data) throws ValidationError;

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
    @NonNull
    String getName();

    /**
	 * Return the task data used for negotiation in the `auth` message.
     */
    @Nullable
    Map<Object, Object> getData();

}

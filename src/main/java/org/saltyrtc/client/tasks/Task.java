package org.saltyrtc.client.tasks;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.exceptions.SignalingException;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.messages.c2c.TaskMessage;
import org.saltyrtc.client.signaling.SignalingInterface;

import java.util.List;
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
    void init(SignalingInterface signaling, Map<Object, Object> data) throws ValidationError;

    /**
     * Used by the signaling class to notify task that the peer handshake is over.
     *
     * This is the point where the task can take over.
     *
     * TODO: Could this be combined with init?
     */
    void onPeerHandshakeDone();

    /**
     * This method is called by SaltyRTC when a task related message
     * arrives through the WebSocket.
     *
     * @param message The deserialized MessagePack message.
     */
    void onTaskMessage(TaskMessage message);

    /**
     * Send bytes through the task signaling channel.
     *
     * This method should only be called after the handover.
     *
     * @throws SignalingException if the signaling state is not OPEN or if the handover hasn't
     *                            taken place yet.
     */
    void sendSignalingMessage(byte[] payload) throws SignalingException;

    /**
     * Return the task protocol name.
     */
    @NonNull
    String getName();

    /**
     * Return the list of supported message types.
     *
     * Incoming messages with this type will be passed to the task.
     */
    @NonNull
    List<String> getSupportedMessageTypes();

    /**
     * Return the task data used for negotiation in the `auth` message.
     */
    @Nullable
    Map<Object, Object> getData();

	/**
	 * This method is called by the signaling class when sending and receiving 'close' messages.
     */
    void close(int reason);

}

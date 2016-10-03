package org.saltyrtc.client.signaling;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SignalingException;
import org.saltyrtc.client.messages.c2c.TaskMessage;
import org.saltyrtc.client.signaling.state.SignalingState;

/**
 * This interface should contain all methods that may be called by tasks.
 */
public interface SignalingInterface {

	/**
     * Return the current signaling state.
     */
    SignalingState getState();

	/**
	 * Return the current signaling channel.
     */
    SignalingChannel getChannel();

	/**
	 * Return the signaling role.
     */
    @NonNull
    SignalingRole getRole();

	/**
	 * Send a task message through the websocket.
     *
     * TODO: Get rid of all exceptions but SignalingException and ConnectionException.
     */
    void sendTaskMessage(TaskMessage msg) throws ProtocolException, SignalingException, ConnectionException;

}

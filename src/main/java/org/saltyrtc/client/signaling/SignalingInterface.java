package org.saltyrtc.client.signaling;

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
    SignalingChannel getSignalingChannel();

}

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.ProtocolException;
import org.saltyrtc.client.exceptions.SignalingException;
import org.saltyrtc.client.keystore.Box;
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

    /**
     * Encrypt data for the peer.
     *
     * @param data The bytes to be encrypted.
     * @param nonce The bytes to be used as NaCl nonce.
     * @return The encrypted box.
     * @throws CryptoFailedException if encryption fails for some reason.
     */
    Box encryptForPeer(byte[] data, byte[] nonce) throws CryptoFailedException;

    /**
     * Decrypt data from the peer.
     *
     * @param box The encrypted box.
     * @return The decrypted bytes.
     * @throws CryptoFailedException if decryption fails for some reason.
     */
    byte[] decryptFromPeer(Box box) throws CryptoFailedException;

}

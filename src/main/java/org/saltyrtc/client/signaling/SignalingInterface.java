package org.saltyrtc.client.signaling;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.crypto.CryptoException;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.SignalingException;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.messages.c2c.TaskMessage;
import org.saltyrtc.client.signaling.state.HandoverState;
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
     * Set the signaling state.
     */
    void setState(SignalingState state);

    /**
     * Get the handover state.
     */
    HandoverState getHandoverState();

    /**
     * Return the signaling role.
     */
    @NonNull
    SignalingRole getRole();

    /**
     * Send a task message through the websocket.
     */
    void sendTaskMessage(TaskMessage msg) throws SignalingException, ConnectionException;

    /**
     * Encrypt data for the peer.
     *
     * @param data The bytes to be encrypted.
     * @param nonce The bytes to be used as NaCl nonce.
     * @return The encrypted box.
     * @throws CryptoException if encryption fails for some reason.
     */
    Box encryptForPeer(byte[] data, byte[] nonce) throws CryptoException;

    /**
     * Decrypt data from the peer.
     *
     * @param box The encrypted box.
     * @return The decrypted bytes.
     * @throws CryptoException if decryption fails for some reason.
     */
    byte[] decryptFromPeer(Box box) throws CryptoException;

    /**
     * Handle incoming signaling messages from the peer.
     *
     * This method can be used by tasks to pass in messages that arrived through their signaling channel.
     *
     * @param decryptedBytes The decrypted message bytes.
     */
    void onSignalingPeerMessage(byte[] decryptedBytes);

    /**
     * Send a close message to the peer.
     *
     * This method may only be called once the client-to-client handshakes has been completed.
     *
     * Note that sending a close message does not reset the connection. To do that,
     * `resetConnection` needs to be called explicitly.
     *
     * @param reason The close code. See `CloseCode` class for possible values.
     */
    void sendClose(CloseCode reason);

    /**
     * Close and reset the connection with the specified close code.
     *
     * If the reason passed in is `null`, then this will be treated as a quiet
     * reset - no listeners will be notified.
     *
     * @param reason The close code to use.
     */
    void resetConnection(@Nullable CloseCode reason);

}

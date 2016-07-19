package org.saltyrtc.client.datachannel;

import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.nonce.DataChannelNonce;
import org.saltyrtc.client.signaling.Signaling;
import org.slf4j.Logger;
import org.webrtc.DataChannel;

import java.nio.ByteBuffer;

/**
 * A wrapper for a DataChannel that will encrypt and decrypt data on the fly.
 *
 * It should match the API of the WebRTC `DataChannel`.
 *
 * Unfortunately, the `DataChannel` class does not provide an interface that we could implement.
 */
public class SecureDataChannel {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.SecureDataChannel");

    private final DataChannel dc;
    private final Signaling signaling;

    public SecureDataChannel(DataChannel dc, Signaling signaling) {
        this.dc = dc;
        this.signaling = signaling;
    }

    public void registerObserver(final DataChannel.Observer observer) {
        this.dc.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                observer.onBufferedAmountChange(l);
            }

            @Override
            public void onStateChange() {
                observer.onStateChange();
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                LOG.debug("Decrypting incoming data...");

                // Decrypt data
                final Box box = new Box(buffer.data, DataChannelNonce.TOTAL_LENGTH);
                final byte[] data;
                try {
                    data = SecureDataChannel.this.signaling.decryptData(box);
                } catch (CryptoFailedException | InvalidKeyException e) {
                    LOG.error("Could not decrypt incoming data: ", e);
                    return;
                }

                // Pass decrypted data to original observer
                DataChannel.Buffer decryptedBuffer =
                        new DataChannel.Buffer(ByteBuffer.wrap(data), true);
                observer.onMessage(decryptedBuffer);
            }
        });
    }

    public void unregisterObserver() {
        this.dc.unregisterObserver();
    }

    public String label() {
        return this.dc.label();
    }

    public DataChannel.State state() {
        return this.dc.state();
    }

    public long bufferedAmount() {
        return this.dc.bufferedAmount();
    }

    public void close() {
        this.dc.close();
    }

    /**
     * Encrypt and send a message through the data channel.
     *
     * @return a binary flag that indicates whether the message could be sent.
     */
    public boolean send(DataChannel.Buffer buffer) {
        LOG.debug("Encrypting outgoing data...");

        // Encrypt
        final Box box;
        try {
            box = this.signaling.encryptData(buffer.data.array(), this);
        } catch (CryptoFailedException | InvalidKeyException e) {
            LOG.error("Could not decrypt incoming data: ", e);
            return false;
        }

        // Send
        final ByteBuffer encryptedBytes = ByteBuffer.wrap(box.toBytes());
        final DataChannel.Buffer encryptedBuffer = new DataChannel.Buffer(encryptedBytes, true);
        return this.dc.send(encryptedBuffer);
    }

    public void dispose() {
        this.dc.dispose();
    }

}

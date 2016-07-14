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

    public boolean send(DataChannel.Buffer buffer) {
        LOG.debug("Encrypting outgoing data...");
        return this.dc.send(buffer);
    }

    public void dispose() {
        this.dc.dispose();
    }

}

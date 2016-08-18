package org.saltyrtc.client.datachannel;

import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.OverflowException;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.keystore.Box;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.nonce.CombinedSequencePair;
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
 * https://bugs.chromium.org/p/webrtc/issues/detail?id=6221
 */
public class SecureDataChannel {

    // Logger
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger("SaltyRTC.SecureDataChannel");

    private final DataChannel dc;
    private final Signaling signaling;
    private final CombinedSequencePair csnPair;

    public SecureDataChannel(DataChannel dc, Signaling signaling) {
        this.dc = dc;
        this.signaling = signaling;
        this.csnPair = new CombinedSequencePair();
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

                final Box box = new Box(buffer.data, DataChannelNonce.TOTAL_LENGTH);

                // Validate nonce
                try {
                    SecureDataChannel.this.validateNonce(new DataChannelNonce(ByteBuffer.wrap(box.getNonce())));
                } catch (ValidationError e) {
                    LOG.error("Invalid nonce: " + e);
                    LOG.error("Closing data channel");
                    SecureDataChannel.this.close();
                    return;
                }

                // Decrypt data
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
            final byte[] data = buffer.data.array();
            final SecureDataChannel sdc = this;
            final CombinedSequence csn = this.csnPair.getOurs().next();
            box = this.signaling.encryptData(data, sdc, csn);
        } catch (CryptoFailedException | InvalidKeyException e) {
            LOG.error("Could not encrypt outgoing data: ", e);
            return false;
        } catch (OverflowException e) {
            LOG.error("CSN overflow: ", e);
            LOG.error("Closing data channel");
            this.close();
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

    /**
     * Validate the nonce of incoming messages.
     */
    private void validateNonce(DataChannelNonce nonce) throws ValidationError {
        // Validate cookie
        if (nonce.getCookie().equals(this.signaling.getPeerCookie())) {
            throw new ValidationError("Local and remote cookies are equal");
        }
        if (!nonce.getCookie().equals(this.signaling.getPeerCookie())) {
            throw new ValidationError("Remote cookie changed");
        }

        // Validate CSN
        if (this.csnPair.hasTheirs()) {
            final long previous = this.csnPair.getTheirs();
            final long current = nonce.getCombinedSequence();
            if (current < previous) {
                throw new ValidationError("Peer CSN is lower than last time");
            } else if (current == previous) {
                throw new ValidationError("Peer CSN hasn't been incremented");
            } else {
                this.csnPair.setTheirs(current);
            }
        } else {
            if (nonce.getOverflow() != 0) {
                throw new ValidationError("First message from peer must have set the overflow number to 0");
            }
            this.csnPair.setTheirs(nonce.getCombinedSequence());
        }

        // TODO: Ensure that the data channel id in the nonce matches the actual id
        // Blocked by https://bugs.chromium.org/p/webrtc/issues/detail?id=6106
    }

}

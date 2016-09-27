package org.saltyrtc.client.datachannel;

import org.saltyrtc.chunkedDc.Chunker;
import org.saltyrtc.chunkedDc.Unchunker;
import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
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
import java.util.concurrent.atomic.AtomicInteger;

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

    // Chunking
    private static final int CHUNK_SIZE = 16384;
    private static final int CHUNK_COUNT_GC = 32;
    private static final int CHUNK_MAX_AGE = 60000;
    private final AtomicInteger messageNumber = new AtomicInteger(0);
    private final AtomicInteger chunkCount = new AtomicInteger(0);
    private final Unchunker unchunker = new Unchunker();

    @NonNull
    private final DataChannel dc;
    @NonNull
    private final Signaling signaling;
    @NonNull
    private final CombinedSequencePair csnPair;
    @Nullable
    private DataChannel.Observer observer;

    public SecureDataChannel(@NonNull DataChannel dc, @NonNull Signaling signaling) {
        this.dc = dc;
        this.signaling = signaling;
        this.csnPair = new CombinedSequencePair();

        // Register a message listener for the unchunker
        this.unchunker.onMessage(new Unchunker.MessageListener() {
            @Override
            public void onMessage(ByteBuffer buffer) {
                SecureDataChannel.this.onMessage(buffer);
            }
        });
    }

    /**
     * Register a new data channel observer.
     *
     * It will be notified when something changes, e.g. when a new message
     * arrives or if the state changes.
     */
    public void registerObserver(final DataChannel.Observer observer) {
        this.observer = observer;
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
                LOG.debug("Received chunk");

                // Register the chunk. Once the message is complete, the original
                // observer will be called in the `onMessage` method.
                SecureDataChannel.this.unchunker.add(buffer.data);

                // Clean up old chunks regularly
                if (SecureDataChannel.this.chunkCount.getAndIncrement() > CHUNK_COUNT_GC) {
                    SecureDataChannel.this.unchunker.gc(CHUNK_MAX_AGE);
                    SecureDataChannel.this.chunkCount.set(0);
                }
            }
        });
    }

    public void unregisterObserver() {
        this.observer = null;
        this.dc.unregisterObserver();
    }

    private void onMessage(ByteBuffer buffer) {
        LOG.debug("Decrypting incoming data...");

        final Box box = new Box(buffer, DataChannelNonce.TOTAL_LENGTH);

        // Validate nonce
        try {
            this.validateNonce(new DataChannelNonce(ByteBuffer.wrap(box.getNonce())));
        } catch (ValidationError e) {
            LOG.error("Invalid nonce: " + e);
            LOG.error("Closing data channel");
            this.close();
            return;
        }

        // Decrypt data
        final byte[] data;
        try {
            data = this.signaling.decryptData(box);
        } catch (CryptoFailedException | InvalidKeyException e) {
            LOG.error("Could not decrypt incoming data: ", e);
            return;
        }

        // Pass decrypted data to original observer
        DataChannel.Buffer decryptedBuffer =
                new DataChannel.Buffer(ByteBuffer.wrap(data), true);
        if (this.observer != null) {
            this.observer.onMessage(decryptedBuffer);
        } else {
            // TODO: Cache message?
            LOG.warn("Received new message, but no observer is configured.");
        }
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
        final ByteBuffer encryptedBytes = ByteBuffer.wrap(box.toBytes());

        // Chunkify
        final int msgId = this.messageNumber.getAndIncrement();
        Chunker chunker = new Chunker(msgId, encryptedBytes, CHUNK_SIZE);

        // Send chunks
        while (chunker.hasNext()) {
            final DataChannel.Buffer out = new DataChannel.Buffer(chunker.next(), true);
            if (!this.dc.send(out)) {
                return false;
            }
        }
        return true;
    }

    public void dispose() {
        this.dc.dispose();
    }

    /**
     * Validate the nonce of incoming messages.
     */
    private void validateNonce(DataChannelNonce nonce) throws ValidationError {
        // Validate cookie
        if (nonce.getCookie().equals(this.signaling.getCookie())) {
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

package org.saltyrtc.client;

import org.saltyrtc.client.exceptions.InvalidChunkException;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Unchunkifies and raises an event when a mesage is complete.
 */
public class Unchunkifier {
    private final Events events;
    private ArrayList<byte[]> chunks;
    private int length = 0;

    public interface Events {
        void onCompletedMessage(ByteBuffer buffer);
    }

    public Unchunkifier(Events events) {
        this.events = events;
        this.reset();
    }

    public synchronized void add(ByteBuffer buffer) throws InvalidChunkException {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        this.add(bytes);
    }

    public synchronized void add(byte[] bytes) throws InvalidChunkException {
        if (bytes.length == 0) {
            return;
        }

        // Add to list
        this.chunks.add(bytes);
        this.length += (bytes.length - 1);

        // More chunks?
        int moreChunks = bytes[0] & 0xFF;
        if (moreChunks == 0) {
            this.done();
        } else if (moreChunks != 1) {
            throw new InvalidChunkException(moreChunks);
        }
    }

    protected synchronized void reset() {
        this.chunks = new ArrayList<>();
        this.length = 0;
    }

    protected synchronized void done() {
        ByteBuffer message = this.merge();
        this.reset();
        this.events.onCompletedMessage(message);
    }

    protected synchronized ByteBuffer merge() {
        // Note: 'allocateDirect' does NOT work, DO NOT CHANGE!
        ByteBuffer buffer = ByteBuffer.allocate(this.length);

        // Add all chunks apart from the first byte
        for (byte[] chunk : this.chunks) {
            buffer.put(chunk, 1, chunk.length - 1);
        }

        // Flip offset and remaining length for reading
        buffer.flip();
        return buffer;
    }
}

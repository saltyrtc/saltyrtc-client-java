package org.saltyrtc.client.exceptions;

public class InvalidChunkException extends Exception {
    public final int moreChunks;

    public InvalidChunkException(int moreChunks) {
        super();
        this.moreChunks = moreChunks;
    }
}

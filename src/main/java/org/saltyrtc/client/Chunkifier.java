/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import java.nio.ByteBuffer;
import java.util.Iterator;

/**
 * Iterates over chunks of bytes and injects a 'more' flag byte.
 */
public class Chunkifier implements Iterable<ByteBuffer> {
    private final byte[] bytes;
    private final int chunkSize; // First byte is being used as a 'more' flag

    public Chunkifier(byte[] bytes, int chunkSize) {
        this.bytes = bytes;
        this.chunkSize = chunkSize;
    }

    @Override
    public Iterator<ByteBuffer> iterator() {
        return new Iterator<ByteBuffer>() {
            private int index = 0;

            public int offset() {
                return this.offset(this.index);
            }

            public int offset(int index) {
                return index * (chunkSize - 1);
            }

            @Override
            public boolean hasNext() {
                return offset() < bytes.length;
            }

            public boolean hasNext(int index) {
                return offset(index) < bytes.length;
            }

            @Override
            public ByteBuffer next() {
                // More chunks?
                byte moreChunks;
                if (hasNext(this.index + 1)) {
                    moreChunks = (byte) 0x01;
                } else {
                    moreChunks = (byte) 0x00;
                }

                // Put more chunks indicator and bytes into buffer
                // Note: 'allocateDirect' does NOT work, DO NOT CHANGE!
                int offset = offset();
                int length = Math.min(chunkSize, bytes.length + 1 - offset);
                ByteBuffer buffer = ByteBuffer.allocate(length);
                buffer.put(moreChunks);
                buffer.put(bytes, offset, length - 1);

                // Flip offset and remaining length for reading
                buffer.flip();
                this.index += 1;
                return buffer;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}

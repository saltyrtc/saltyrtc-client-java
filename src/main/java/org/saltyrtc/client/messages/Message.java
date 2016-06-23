/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack.PackerConfig;
import org.msgpack.core.MessagePacker;

import java.io.IOException;

/**
 * All messages sent through SaltyRTC extend this class.
 */
public abstract class Message {

    /**
     * Return its own message type.
     */
    public abstract String getType();

    /**
     * Write itself to the MessagePacker.
     */
    public abstract void write(MessagePacker msgPacker) throws IOException;

    /**
     * Return messagepacked byte array.
     */
    public byte[] toBytes() {
        final MessageBufferPacker packer = new PackerConfig()
                .newBufferPacker();
        try {
            this.write(packer);
        } catch (IOException e) {
            // This shouldn't happen, as we're writing to a buffer, not to a stream
            throw new RuntimeException("IOException while writing to MessageBufferPacker", e);
        }
        final byte[] data = packer.toByteArray();
        try {
            packer.close();
        } catch (IOException e) {
            throw new RuntimeException("IOException while closing MessageBufferPacker", e);
        }
        return data;
    }
}
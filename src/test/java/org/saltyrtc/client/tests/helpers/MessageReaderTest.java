/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.helpers;

import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.MessageReader;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MessageReaderTest {

    @Test(expected=SerializationError.class)
    public void testEmptyBytes() throws SerializationError, ValidationError {
        MessageReader.read(new byte[] { });
    }

    @Test(expected=SerializationError.class)
    public void testInvalidBytes() throws SerializationError, ValidationError {
        MessageReader.read(new byte[] { 0x01 });
    }

    @Test
    public void testEmptyDict() throws ValidationError {
        try {
            MessageReader.read(new byte[]{(byte) (0x80 & 0xf0)});
        } catch (SerializationError e) {
            assertEquals("Message does not contain a type field", e.getMessage());
        }
    }

    @Test
    public void testUnknownType() throws IOException, SerializationError {
        MessageBufferPacker packer = new MessagePack.PackerConfig().newBufferPacker();
        packer.packMapHeader(1).packString("type").packString("hack-the-planet");
        try {
            MessageReader.read(packer.toByteArray());
        } catch (ValidationError e) {
            assertEquals("Unknown message type: hack-the-planet", e.getMessage());
        }
    }

    @Test
    public void testInvalidType() throws IOException, ValidationError {
        MessageBufferPacker packer = new MessagePack.PackerConfig().newBufferPacker();
        packer.packMapHeader(1).packString("type").packInt(1);
        try {
            MessageReader.read(packer.toByteArray());
        } catch (SerializationError e) {
            assertEquals("Message type must be a string", e.getMessage());
        }
    }

}

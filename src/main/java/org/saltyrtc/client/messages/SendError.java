/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages;

import org.msgpack.core.MessagePacker;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;

import java.io.IOException;
import java.util.Map;

public class SendError extends Message {

    public static String TYPE = "send-error";

    private byte[] hash;

    public SendError(byte[] hash) {
        this.hash = hash;
    }

    public SendError(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        this.hash = ValidationHelper.validateByteArray(map.get("hash"), 32, "Hash");
    }

    public byte[] getHash() {
        return hash;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(2)
                .packString("type")
                    .packString(TYPE)
                .packString("hash")
                    .packBinaryHeader(this.hash.length)
                    .writePayload(this.hash);
    }

    @Override
    public String getType() {
        return TYPE;
    }

}
/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages.s2c;

import org.msgpack.core.MessagePacker;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;
import org.saltyrtc.client.messages.Message;

import java.io.IOException;
import java.util.Map;

public class SendError extends Message {

    public static final String TYPE = "send-error";

    private byte[] id;

    public SendError(byte[] id) {
        this.id = id;
    }

    public SendError(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        this.id = ValidationHelper.validateByteArray(map.get("id"), 8, "id");
    }

    public byte[] getId() {
        return id;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(2)
                .packString("type")
                    .packString(TYPE)
                .packString("id")
                    .packBinaryHeader(this.id.length)
                    .writePayload(this.id);
    }

    @Override
    public String getType() {
        return TYPE;
    }

}

/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages.c2c;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.core.MessagePacker;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;
import org.saltyrtc.client.messages.Message;

import java.io.IOException;
import java.util.Map;

/**
 * Application message.
 *
 * This message type is special, as the `data` field may contain arbitrary data.
 * It is modeled as an opaque `Object`. That means that a round-trip serialization
 * and deserialization will not result in exactly the same objects.
 */
public class Application extends Message {

    public static final String TYPE = "application";

    private Object data;

    public Application(Object data) {
        this.data = data;
    }

    public Application(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        if (!map.containsKey("data")) {
            throw new ValidationError("Message is missing the 'data' key");
        }
        this.data = map.get("data");
    }

    public Object getData() {
        return this.data;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(2)
                .packString("type")
                    .packString(TYPE)
                .packString("data");
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        packer.writePayload(objectMapper.writeValueAsBytes(this.data));
    }

    @Override
    public String getType() {
        return TYPE;
    }

}

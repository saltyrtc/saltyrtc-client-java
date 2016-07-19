/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.core.MessagePacker;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;

import java.io.IOException;
import java.util.Map;

/**
 * Data message.
 *
 * This message type is special, as the `data` field may contain arbitrary data.
 * It is modeled as an opaque `Object`. That means that a round-trip serialization
 * and deserialization will not result in exactly the same objects.
 */
public class Data extends Message {

    public static String TYPE = "data";

    private String dataType; // Optional, may be null
    private Object data;

    public Data(String dataType, Object data) {
        this.dataType = dataType;
        this.data = data;
    }

    public Data(Object data) {
        this.data = data;
    }

    public Data(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        if (map.containsKey("data_type")) {
            this.dataType = ValidationHelper.validateString(map.get("data_type"), "data_type");
        }
        this.data = map.get("data");
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        if (this.dataType == null) {
            packer.packMapHeader(2);
        } else {
            packer.packMapHeader(3);
        }
        packer.packString("type").packString(TYPE);
        if (this.dataType != null) {
            packer.packString("data_type").packString(this.dataType);
        }
        // Serialize data using object mapper
        packer.packString("data");
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        packer.writePayload(objectMapper.writeValueAsBytes(this.data));
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public String getDataType() {
        return dataType;
    }

    public Object getData() {
        return data;
    }

}

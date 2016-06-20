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

public class DropResponder extends Message {

    public static String TYPE = "drop-responder";

    private Integer id;

    public DropResponder(Integer id) {
        this.id = id;
    }

    public DropResponder(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        this.id = ValidationHelper.validateInteger(map.get("id"), 0x00, 0xff, "id");
    }

    public Integer getId() {
        return id;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(2)
                .packString("type")
                    .packString(TYPE)
                .packString("id")
                    .packInt(this.id);
    }
}
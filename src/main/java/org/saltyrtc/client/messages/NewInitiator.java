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

public class NewInitiator extends Message {

    public static String TYPE = "new-initiator";

    public NewInitiator() { }

    public NewInitiator(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(1)
                .packString("type")
                    .packString(TYPE);
    }
}
/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages;

import com.neilalexander.jnacl.NaCl;

import org.msgpack.core.MessagePacker;
import org.saltyrtc.client.exceptions.ValidationError;

import java.io.IOException;
import java.util.Map;

public class ServerHello extends Message {

    public static String TYPE = "server-hello";

    private byte[] key;

    public ServerHello(byte[] key) {
        this.key = key;
    }

    public ServerHello(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        this.key = ValidationHelper.validateByteArray(map.get("key"), NaCl.PUBLICKEYBYTES, "Key");
    }

    public byte[] getKey() {
        return key;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(2)
                .packString("type")
                    .packString("server-hello")
                .packString("key")
                    .packBinaryHeader(this.key.length)
                    .writePayload(this.key);
    }
}
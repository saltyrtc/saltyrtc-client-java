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
import org.saltyrtc.client.helpers.ValidationHelper;

import java.io.IOException;
import java.util.Map;

public class Auth extends Message {

    public static String TYPE = "auth";

    private byte[] yourCookie;

    public Auth(byte[] key) {
        this.yourCookie = key;
    }

    public Auth(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        this.yourCookie = ValidationHelper.validateByteArray(map.get("your_cookie"), NaCl.PUBLICKEYBYTES, "Cookie");
    }

    public byte[] getYourCookie() {
        return yourCookie;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(2)
                .packString("type")
                    .packString(TYPE)
                .packString("your_cookie")
                    .packBinaryHeader(this.yourCookie.length)
                    .writePayload(this.yourCookie);
    }
}
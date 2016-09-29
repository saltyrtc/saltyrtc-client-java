/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
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
import java.util.List;
import java.util.Map;

public class ClientAuth extends Message {

    public static String TYPE = "client-auth";

    private byte[] yourCookie;
    private List<String> subprotocols;

    public ClientAuth(byte[] yourCookie, List<String> subprotocols) {
        this.yourCookie = yourCookie;
        this.subprotocols = subprotocols;
    }

    public ClientAuth(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        final int COOKIE_LENGTH = 16;
        this.yourCookie = ValidationHelper.validateByteArray(map.get("your_cookie"), COOKIE_LENGTH, "your_cookie");
        this.subprotocols = ValidationHelper.validateTypedList(map.get("subprotocols"), String.class, "subprotocols");
    }

    public byte[] getYourCookie() {
        return yourCookie;
    }

    public List<String> getSubprotocols() {
        return this.subprotocols;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(3)
                .packString("type")
                    .packString("client-auth")
                .packString("your_cookie")
                    .packBinaryHeader(this.yourCookie.length)
                    .writePayload(this.yourCookie);
        packer.packString("subprotocols").packArrayHeader(this.subprotocols.size());
        for (String subprotocol : this.subprotocols) {
            packer.packString(subprotocol);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

}

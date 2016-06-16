/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages;

import org.msgpack.core.MessagePacker;

import java.io.IOException;
import java.util.Map;

public class ResponderServerAuth extends Message {

    public static String TYPE = "server-auth";

    private byte[] yourCookie;
    private boolean initiatorConnected;

    public ResponderServerAuth(byte[] yourCookie, boolean initiatorConnected) {
        this.yourCookie = yourCookie;
        this.initiatorConnected = initiatorConnected;
    }

    public ResponderServerAuth(Map<String, Object> map) {
        ValidationHelper.validateType(map.get("type"), String.class, TYPE);

        // TODO: more validation
        this.yourCookie = (byte[])map.get("your_cookie");
        this.initiatorConnected = (boolean)map.get("initiator_connected");
    }

    public byte[] getYourCookie() {
        return yourCookie;
    }

    public boolean isInitiatorConnected() {
        return initiatorConnected;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(3)
                .packString("type")
                    .packString("server-auth")
                .packString("your_cookie")
                    .packBinaryHeader(this.yourCookie.length)
                    .writePayload(this.yourCookie)
                .packString("initiator_connected")
                    .packBoolean(this.initiatorConnected);
    }

}
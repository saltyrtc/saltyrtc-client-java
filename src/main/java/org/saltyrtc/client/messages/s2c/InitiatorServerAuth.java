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

public class InitiatorServerAuth extends Message {

    public static String TYPE = "server-auth";

    private byte[] yourCookie;
    private List<Integer> responders;

    public InitiatorServerAuth(byte[] yourCookie, List<Integer> responders) {
        this.yourCookie = yourCookie;
        this.responders = responders;
    }

    public InitiatorServerAuth(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        final int COOKIE_LENGTH = 16;
        this.yourCookie = ValidationHelper.validateByteArray(map.get("your_cookie"), COOKIE_LENGTH, "your_cookie");
        this.responders = ValidationHelper.validateIntegerList(map.get("responders"), Integer.class, "responders");
    }

    public byte[] getYourCookie() {
        return yourCookie;
    }

    public List<Integer> getResponders() {
        return responders;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        packer.packMapHeader(3)
                .packString("type")
                    .packString(TYPE)
                .packString("your_cookie")
                    .packBinaryHeader(this.yourCookie.length)
                    .writePayload(this.yourCookie)
                .packString("responders")
                    .packArrayHeader(this.responders.size());
        for (int responder : this.responders) {
            packer.packInt(responder);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

}

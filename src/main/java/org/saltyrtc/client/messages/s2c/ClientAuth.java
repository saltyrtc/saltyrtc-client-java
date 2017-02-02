/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages.s2c;

import org.msgpack.core.MessagePacker;
import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;
import org.saltyrtc.client.messages.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClientAuth extends Message {

    public static final String TYPE = "client-auth";

    @NonNull
    private byte[] yourCookie;
    @Nullable
    private byte[] yourKey = null;
    @NonNull
    private List<String> subprotocols;
    @NonNull
    private int pingInterval;

    public ClientAuth(byte[] yourCookie, List<String> subprotocols, int pingInterval) {
        this.yourCookie = yourCookie;
        this.subprotocols = subprotocols;
        this.pingInterval = pingInterval;
    }

    public ClientAuth(byte[] yourCookie, byte[] yourKey, List<String> subprotocols, int pingInterval) {
        this(yourCookie, subprotocols, pingInterval);
        this.yourKey = yourKey;
    }

    public ClientAuth(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        final int COOKIE_LENGTH = 16;
        final int YOUR_KEY_LENGTH = 32;
        this.yourCookie = ValidationHelper.validateByteArray(map.get("your_cookie"), COOKIE_LENGTH, "your_cookie");
        this.subprotocols = ValidationHelper.validateTypedList(map.get("subprotocols"), String.class, "subprotocols");
        this.pingInterval = ValidationHelper.validateInteger(map.get("ping_interval"), 0, Integer.MAX_VALUE, "ping_interval");
        if (map.containsKey("your_key") && map.get("your_key") != null) {
            this.yourKey = ValidationHelper.validateByteArray(map.get("your_key"), YOUR_KEY_LENGTH, "your_key");
        }
    }

    @NonNull
    public byte[] getYourCookie() {
        return this.yourCookie;
    }

    @Nullable
    public byte[] getYourKey() {
        return this.yourKey;
    }

    @NonNull
    public List<String> getSubprotocols() {
        return this.subprotocols;
    }

    @NonNull
    public int getPingInterval() {
        return pingInterval;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        final boolean hasKey = this.yourKey != null;
        packer.packMapHeader(hasKey ? 5 : 4)
              .packString("type")
                  .packString("client-auth")
              .packString("your_cookie")
                  .packBinaryHeader(this.yourCookie.length)
                  .writePayload(this.yourCookie);
        if (hasKey) {
            packer.packString("your_key")
                  .packBinaryHeader(this.yourKey.length)
                  .writePayload(this.yourKey);
        }
        packer.packString("ping_interval")
              .packInt(this.pingInterval);
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

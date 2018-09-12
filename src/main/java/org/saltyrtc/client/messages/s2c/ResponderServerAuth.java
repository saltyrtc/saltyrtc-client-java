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
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;
import org.saltyrtc.client.messages.Message;

import java.io.IOException;
import java.util.Map;

public class ResponderServerAuth extends Message {

    public static final String TYPE = "server-auth";

    @NonNull private byte[] yourCookie;
    @Nullable private byte[] signedKeys;
    private boolean initiatorConnected;

    public ResponderServerAuth(@NonNull byte[] yourCookie,
                               @Nullable byte[] signedKeys,
                               boolean initiatorConnected) {
        this.yourCookie = yourCookie;
        this.signedKeys = signedKeys;
        this.initiatorConnected = initiatorConnected;
    }

    public ResponderServerAuth(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        final int COOKIE_LENGTH = 16;
        final int SIGNED_KEYS_LENGTH = CryptoProvider.PUBLICKEYBYTES * 2 + CryptoProvider.BOXOVERHEAD;
        this.yourCookie = ValidationHelper.validateByteArray(map.get("your_cookie"), COOKIE_LENGTH, "your_cookie");
        this.initiatorConnected = ValidationHelper.validateBoolean(map.get("initiator_connected"), "initiator_connected");
        if (map.containsKey("signed_keys") && map.get("signed_keys") != null) {
            this.signedKeys = ValidationHelper.validateByteArray(map.get("signed_keys"), SIGNED_KEYS_LENGTH, "signed_keys");
        }
    }

    @NonNull
    public byte[] getYourCookie() {
        return yourCookie;
    }

    @Nullable
    public byte[] getSignedKeys() {
        return signedKeys;
    }

    public boolean isInitiatorConnected() {
        return initiatorConnected;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        final boolean hasSignedKeys = this.signedKeys != null;
        packer.packMapHeader(hasSignedKeys ? 4 : 3)
                .packString("type")
                    .packString(TYPE)
                .packString("your_cookie")
                    .packBinaryHeader(this.yourCookie.length)
                    .writePayload(this.yourCookie)
                .packString("initiator_connected")
                    .packBoolean(this.initiatorConnected);
        if (hasSignedKeys) {
            packer.packString("signed_keys")
                .packBinaryHeader(this.signedKeys.length)
                .writePayload(this.signedKeys);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

}

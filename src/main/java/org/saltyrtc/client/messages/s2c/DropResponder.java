/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
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
import org.saltyrtc.client.signaling.CloseCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DropResponder extends Message {

    public static final String TYPE = "drop-responder";

    @NonNull
    private Integer id;
    @Nullable
    private Integer reason;

    public DropResponder(@NonNull Integer id) {
        this.id = id;
    }

    public DropResponder(@NonNull Integer id, @Nullable Integer reason) {
        this(id);
        this.reason = reason;
    }

    public DropResponder(short id) {
        this.id = (int) id;
    }

    public DropResponder(short id, @Nullable Integer reason) {
        this(id);
        this.reason = reason;
    }

    public DropResponder(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        this.id = ValidationHelper.validateInteger(map.get("id"), 0x00, 0xff, "id");
        if (map.containsKey("reason")) {
            List<Integer> validRange = new ArrayList<>();
            for (int i = 0; i < CloseCode.CLOSE_CODES_DROP_RESPONDER.length; i++) {
                validRange.add(CloseCode.CLOSE_CODES_DROP_RESPONDER[i]);
            }
            this.reason = ValidationHelper.validateInteger(map.get("reason"), validRange, "reason");
        }
    }

    @NonNull
    public Integer getId() {
        return id;
    }

    @Nullable
    public Integer getReason() {
        return reason;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        final boolean hasReason = this.reason != null;
        packer.packMapHeader(hasReason ? 3 : 2)
                .packString("type")
                    .packString(TYPE)
                .packString("id")
                    .packInt(this.id);
        if (hasReason) {
            packer.packString("reason")
                .packInt(this.reason);
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

}

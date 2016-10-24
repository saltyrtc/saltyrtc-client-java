/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages.c2c;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.core.MessagePacker;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;
import org.saltyrtc.client.messages.Message;

import java.io.IOException;
import java.util.Map;

public class InitiatorAuth extends Message {

    public static final String TYPE = "auth";

    private byte[] yourCookie;
    private String task;
    private Map<String, Map<Object, Object>> data;

    public InitiatorAuth(byte[] yourCookie, String task, Map<String, Map<Object, Object>> data) throws ValidationError {
        this.yourCookie = yourCookie;
        this.validateTaskData(task, data);
        this.task = task;
        this.data = data;
    }

    public InitiatorAuth(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        final int COOKIE_LENGTH = 16;
        this.yourCookie = ValidationHelper.validateByteArray(map.get("your_cookie"), COOKIE_LENGTH, "Cookie");
        this.task = ValidationHelper.validateString(map.get("task"), "task");
        this.data = ValidationHelper.validateStringMapMap(map.get("data"), "data");
        this.validateTaskData(this.task, this.data);
    }

    /**
     * Validate that task name and task data are both set, and that they match.
     * @param task Task name
     * @param data Task data
     * @throws ValidationError if validation fails
     */
    private void validateTaskData(String task, Map<String, Map<Object, Object>> data) throws ValidationError {
        if (task.isEmpty()) {
            throw new ValidationError("Task name must not be empty");
        }
        if (data.size() < 1) {
            throw new ValidationError("Task data must not be empty");
        }
        if (data.size() > 1) {
            throw new ValidationError("Task data must contain exactly 1 key");
        }
        if (!data.containsKey(task)) {
            throw new ValidationError("Task data must contain an entry for the chosen task");
        }
    }

    public byte[] getYourCookie() {
        return yourCookie;
    }

    public String getTask() {
        return this.task;
    }

    public Map<String, Map<Object, Object>> getData() {
        return this.data;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        final byte[] dataBytes = objectMapper.writeValueAsBytes(this.data);
        packer.packMapHeader(4)
                .packString("type")
                    .packString(TYPE)
                .packString("your_cookie")
                    .packBinaryHeader(this.yourCookie.length)
                    .writePayload(this.yourCookie)
                .packString("task")
                    .packString(this.task)
                .packString("data")
                    .writePayload(dataBytes);
    }

    @Override
    public String getType() {
        return TYPE;
    }

}

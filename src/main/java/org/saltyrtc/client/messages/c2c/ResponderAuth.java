/*
 * Copyright (c) 2016-2017 Threema GmbH
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
import java.util.List;
import java.util.Map;

public class ResponderAuth extends Message {

    public static final String TYPE = "auth";

    private byte[] yourCookie;
    private List<String> tasks;
    private Map<String, Map<Object, Object>> data;

    public ResponderAuth(byte[] yourCookie, List<String> tasks, Map<String, Map<Object, Object>> data) throws ValidationError {
        this.yourCookie = yourCookie;
        this.validateTasksData(tasks, data);
        this.tasks = tasks;
        this.data = data;
    }

    public ResponderAuth(Map<String, Object> map) throws ValidationError {
        ValidationHelper.validateType(map.get("type"), TYPE);
        final int COOKIE_LENGTH = 16;
        this.yourCookie = ValidationHelper.validateByteArray(map.get("your_cookie"), COOKIE_LENGTH, "Cookie");
        this.tasks = ValidationHelper.validateTypedList(map.get("tasks"), String.class, "tasks");
        this.data = ValidationHelper.validateStringMapMap(map.get("data"), "data");
        this.validateTasksData(this.tasks, this.data);
    }

    /**
     * Validate that task names and task data are both set, and that they match.
     * @param tasks Task names
     * @param data Task data
     * @throws ValidationError if validation fails
     */
    private void validateTasksData(List<String> tasks, Map<String, Map<Object, Object>> data) throws ValidationError {
        if (tasks.size() < 1) {
            throw new ValidationError("Task names must not be empty");
        }
        if (data.size() < 1) {
            throw new ValidationError("Task data must not be empty");
        }
        if (data.size() != tasks.size()) {
            throw new ValidationError("Task data must contain an entry for every task");
        }
        for (String task : tasks) {
            if (!data.containsKey(task)) {
                throw new ValidationError("Task data must contain an entry for every task");
            }
        }
    }

    public byte[] getYourCookie() {
        return yourCookie;
    }

    public List<String> getTasks() {
        return this.tasks;
    }

    public Map<String, Map<Object, Object>> getData() {
        return this.data;
    }

    @Override
    public void write(MessagePacker packer) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());

        // Pack basic information
        packer.packMapHeader(4)
                .packString("type")
                    .packString(TYPE)
                .packString("your_cookie")
                    .packBinaryHeader(this.yourCookie.length)
                    .writePayload(this.yourCookie);

        // Pack tasks list
        packer.packString("tasks").packArrayHeader(this.tasks.size());
        for (String task : this.tasks) {
            packer.packString(task);
        }

        // Pack data
        final byte[] dataBytes = objectMapper.writeValueAsBytes(this.data);
        packer.packString("data").writePayload(dataBytes);
    }

    @Override
    public String getType() {
        return TYPE;
    }

}

/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.messages.c2c.Close;
import org.saltyrtc.client.messages.c2c.Auth;
import org.saltyrtc.client.messages.s2c.ClientAuth;
import org.saltyrtc.client.messages.s2c.ClientHello;
import org.saltyrtc.client.messages.s2c.DropResponder;
import org.saltyrtc.client.messages.s2c.InitiatorServerAuth;
import org.saltyrtc.client.messages.c2c.Key;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.s2c.NewInitiator;
import org.saltyrtc.client.messages.s2c.NewResponder;
import org.saltyrtc.client.messages.s2c.ResponderServerAuth;
import org.saltyrtc.client.messages.s2c.SendError;
import org.saltyrtc.client.messages.s2c.ServerHello;
import org.saltyrtc.client.messages.c2c.Token;

import java.io.IOException;
import java.util.Map;

/**
 * Read msgpack bytes, create corresponding message.
 */
public class MessageReader {

    /**
     * Read MessagePack bytes, return a Message subclass instance.
     * @param bytes Messagepack bytes.
     * @return Message subclass instance.
     * @throws SerializationError Thrown if deserialization fails.
     * @throws ValidationError Thrown if message can be deserialized but is invalid.
     */
    public static Message read(byte[] bytes) throws SerializationError, ValidationError {
        // Unpack data into map
        ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
        Map<String, Object> map;
        try {
            map = objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new SerializationError("Deserialization failed", e);
        }

        // Get type value
        if (!map.containsKey("type")) {
            throw new SerializationError("Message does not contain a type field");
        }
        Object typeObj = map.get("type");
        if (!(typeObj instanceof String)) {
            throw new SerializationError("Message type must be a string");
        }
        String type = (String)typeObj;

        // Dispatch message instantiation
        switch (type) {
            case "server-hello":
                return new ServerHello(map);
            case "client-hello":
                return new ClientHello(map);
            case "server-auth":
                if (map.containsKey("initiator_connected")) {
                    return new ResponderServerAuth(map);
                } else if (map.containsKey("responders")) {
                    return new InitiatorServerAuth(map);
                }
            case "client-auth":
                return new ClientAuth(map);
            case "new-initiator":
                return new NewInitiator(map);
            case "new-responder":
                return new NewResponder(map);
            case "drop-responder":
                return new DropResponder(map);
            case "send-error":
                return new SendError(map);
            case "token":
                return new Token(map);
            case "key":
                return new Key(map);
            case "auth":
                return new Auth(map);
            case "close":
                return new Close(map);
            default:
                throw new ValidationError("Unknown message type: " + type);
        }
    }
}

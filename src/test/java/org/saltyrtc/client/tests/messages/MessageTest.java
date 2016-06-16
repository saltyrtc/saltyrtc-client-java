/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.messages;

import org.junit.Test;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.MessageReader;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.ServerHello;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MessageTest {

    @Test
    public void testServerHelloSerializationRoundtrip() throws SerializationError, ValidationError {
        final ServerHello out = new ServerHello(new byte[32]);
        final byte[] bytes = out.toBytes();
        final ServerHello in = (ServerHello) MessageReader.read(bytes);
        assertArrayEquals(out.getKey(), in.getKey());
    }

    @Test
    public void testServerHelloBadMapType() throws SerializationError {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "client-hello");
        try {
            new ServerHello(map);
        } catch (ValidationError e) {
            assertEquals("Type must be 'server-hello'", e.getMessage());
        }
    }

    @Test
    public void testResponderServerAuthSerializationRoundtrip() throws SerializationError, ValidationError {
        final ResponderServerAuth out = new ResponderServerAuth(new byte[16], false);
        final byte[] bytes = out.toBytes();
        final ResponderServerAuth in = (ResponderServerAuth) MessageReader.read(bytes);
        assertArrayEquals(out.getYourCookie(), in.getYourCookie());
        assertFalse(in.isInitiatorConnected());
        assertFalse(out.isInitiatorConnected());
    }

}
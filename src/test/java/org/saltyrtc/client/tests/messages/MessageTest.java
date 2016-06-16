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
import org.saltyrtc.client.helpers.RandomHelper;
import org.saltyrtc.client.messages.ClientAuth;
import org.saltyrtc.client.messages.ClientHello;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.ServerHello;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MessageTest {

    /**
     * Serialize and deserialize a message.
     */
    private <T extends Message> T roundTrip(T original) throws ValidationError, SerializationError {
        final byte[] bytes = original.toBytes();
        //noinspection unchecked
        return (T) MessageReader.read(bytes);
    }

    @Test
    public void testServerHelloRoundtrip() throws SerializationError, ValidationError {
        final ServerHello original = new ServerHello(RandomHelper.pseudoRandomBytes(32));
        final ServerHello returned = roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
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
    public void testClientHelloRoundtrip() throws SerializationError, ValidationError {
        final ClientHello original = new ClientHello(RandomHelper.pseudoRandomBytes(32));
        final ClientHello returned = roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
    }

    @Test
    public void testClientAuthRoundtrip() throws SerializationError, ValidationError {
        final ClientAuth original = new ClientAuth(RandomHelper.pseudoRandomBytes(16));
        final ClientAuth returned = roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
    }

    @Test
    public void testResponderServerAuthRoundtrip() throws SerializationError, ValidationError {
        final ResponderServerAuth original = new ResponderServerAuth(
                RandomHelper.pseudoRandomBytes(16), false);
        final ResponderServerAuth returned = roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertFalse(original.isInitiatorConnected());
        assertFalse(returned.isInitiatorConnected());
    }

}
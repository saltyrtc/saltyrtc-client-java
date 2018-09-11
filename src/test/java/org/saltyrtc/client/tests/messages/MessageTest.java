/*
 * Copyright (c) 2016-2018 Threema GmbH
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
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.c2c.Application;
import org.saltyrtc.client.messages.c2c.Close;
import org.saltyrtc.client.messages.c2c.InitiatorAuth;
import org.saltyrtc.client.messages.c2c.Key;
import org.saltyrtc.client.messages.c2c.ResponderAuth;
import org.saltyrtc.client.messages.c2c.Token;
import org.saltyrtc.client.messages.s2c.*;
import org.saltyrtc.client.signaling.CloseCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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
        final ServerHello returned = this.roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
    }

    @Test
    public void testServerHelloBadMapType() {
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
        final ClientHello returned = this.roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
    }

    @Test
    public void testClientAuthRoundtrip() throws SerializationError, ValidationError {
        final List<String> subprotocols = asList("v1.saltyrtc.org", "some.other.protocol");
        final ClientAuth original = new ClientAuth(RandomHelper.pseudoRandomBytes(16), subprotocols, 3);
        final ClientAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertArrayEquals(original.getYourKey(), returned.getYourKey());
        assertArrayEquals(original.getSubprotocols().toArray(), returned.getSubprotocols().toArray());
        assertEquals(original.getPingInterval(), returned.getPingInterval());
    }

    @Test
    public void testClientAuthRoundtripWithKey() throws SerializationError, ValidationError {
        final List<String> subprotocols = asList("v1.saltyrtc.org", "some.other.protocol");
        final ClientAuth original = new ClientAuth(RandomHelper.pseudoRandomBytes(16),
            RandomHelper.pseudoRandomBytes(32), subprotocols, 3);
        final ClientAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertArrayEquals(original.getYourKey(), returned.getYourKey());
        assertArrayEquals(original.getSubprotocols().toArray(), returned.getSubprotocols().toArray());
        assertEquals(original.getPingInterval(), returned.getPingInterval());
    }

    @Test
    public void testInitiatorServerAuthRoundtrip() throws SerializationError, ValidationError {
        final InitiatorServerAuth original = new InitiatorServerAuth(
                RandomHelper.pseudoRandomBytes(16), null, asList(1, 2, 3));
        final InitiatorServerAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertArrayEquals(original.getResponders().toArray(), returned.getResponders().toArray());
        assertArrayEquals(original.getSignedKeys(), returned.getSignedKeys());
    }

    @Test
    public void testInitiatorServerAuthRoundtripWithSignedKeys() throws SerializationError, ValidationError {
        final InitiatorServerAuth original = new InitiatorServerAuth(
            RandomHelper.pseudoRandomBytes(16), RandomHelper.pseudoRandomBytes(80), asList(1, 2, 3));
        final InitiatorServerAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertArrayEquals(original.getResponders().toArray(), returned.getResponders().toArray());
        assertArrayEquals(original.getSignedKeys(), returned.getSignedKeys());
    }

    @Test
    public void testResponderServerAuthRoundtrip() throws SerializationError, ValidationError {
        final ResponderServerAuth original = new ResponderServerAuth(
                RandomHelper.pseudoRandomBytes(16), null, false);
        final ResponderServerAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertFalse(original.isInitiatorConnected());
        assertFalse(returned.isInitiatorConnected());
        assertArrayEquals(original.getSignedKeys(), returned.getSignedKeys());
    }

    @Test
    public void testResponderServerAuthRoundtripWithSignedKeys() throws SerializationError, ValidationError {
        final ResponderServerAuth original = new ResponderServerAuth(
            RandomHelper.pseudoRandomBytes(16), RandomHelper.pseudoRandomBytes(80), false);
        final ResponderServerAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertFalse(original.isInitiatorConnected());
        assertFalse(returned.isInitiatorConnected());
        assertArrayEquals(original.getSignedKeys(), returned.getSignedKeys());
    }

    @Test
    public void testNewInitiatorRoundtrip() throws SerializationError, ValidationError {
        final NewInitiator original = new NewInitiator();
        // There are no real fields in this message, so let's just ensure that there are no exceptions
        this.roundTrip(original);
    }

    @Test
    public void testNewResponderRoundtrip() throws SerializationError, ValidationError {
        final NewResponder original = new NewResponder(42);
        final NewResponder returned = this.roundTrip(original);
        assertEquals(original.getId(), returned.getId());
    }

    @Test
    public void testNewResponderValidation() throws SerializationError {
        final NewResponder original = new NewResponder(0xff + 1);
        try {
            this.roundTrip(original);
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("id must be < 255", e.getMessage());
        }
    }

    @Test
    public void testDropResponderRoundtrip() throws SerializationError, ValidationError {
        final DropResponder original = new DropResponder(42);
        final DropResponder returned = this.roundTrip(original);
        assertEquals(original.getId(), returned.getId());
        assertNull(original.getReason());
        assertNull(returned.getReason());
    }

    @Test
    public void testDropResponderRoundtripWithReason() throws SerializationError, ValidationError {
        final DropResponder original = new DropResponder(42, 3001);
        final DropResponder returned = this.roundTrip(original);
        assertEquals(original.getId(), returned.getId());
        assertEquals(original.getReason(), returned.getReason());
    }

    @Test
    public void testDropResponderIdValidation() throws SerializationError {
        final DropResponder original = new DropResponder(0xff + 1);
        try {
            this.roundTrip(original);
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("id must be < 255", e.getMessage());
        }
    }

    @Test
    public void testDropResponderReasonValidation() throws SerializationError {
        final DropResponder original = new DropResponder(0xff, 6000);
        try {
            this.roundTrip(original);
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("reason is not valid", e.getMessage());
        }
    }

    @Test
    public void testSendErrorRoundtrip() throws SerializationError, ValidationError {
        final SendError original = new SendError(RandomHelper.pseudoRandomBytes(32));
        final SendError returned = this.roundTrip(original);
        assertArrayEquals(original.getId(), returned.getId());
    }

    @Test
    public void testTokenRoundtrip() throws SerializationError, ValidationError {
        final Token original = new Token(RandomHelper.pseudoRandomBytes(32));
        final Token returned = this.roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
    }

    @Test
    public void testKeyRoundtrip() throws SerializationError, ValidationError {
        final Key original = new Key(RandomHelper.pseudoRandomBytes(32));
        final Key returned = this.roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
    }

    @Test
    public void testInitiatorAuthRoundtrip() throws SerializationError, ValidationError {
        final String task = "dummytask";
        final Map<String, Map<Object, Object>> data = new HashMap<>();
        final Map<Object, Object> dummytaskData = new HashMap<>();
        dummytaskData.put("do_something", "yes");
        data.put(task, dummytaskData);

        final InitiatorAuth original = new InitiatorAuth(RandomHelper.pseudoRandomBytes(16), task, data);
        final InitiatorAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
    }

    @Test
    public void testResponderAuthRoundtrip() throws SerializationError, ValidationError {
        final List<String> tasks = asList("dummytask", "alternative");
        final Map<String, Map<Object, Object>> data = new HashMap<>();
        final Map<Object, Object> dummytaskData = new HashMap<>();
        dummytaskData.put("do_something", "yes");
        data.put(tasks.get(0), dummytaskData);
        data.put(tasks.get(1), null);

        final ResponderAuth original = new ResponderAuth(RandomHelper.pseudoRandomBytes(16), tasks, data);
        final ResponderAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
    }

    @Test
    public void testCloseValidation() throws SerializationError {
        final Close original = new Close(4000);
        try {
            this.roundTrip(original);
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("reason must be a valid close code", e.getMessage());
        }
    }

    @Test
    public void testCloseRoundtrip() throws SerializationError, ValidationError {
        final Close original = new Close(CloseCode.PROTOCOL_ERROR);
        final Close returned = this.roundTrip(original);
        assertEquals(original.getReason(), returned.getReason());
    }

    @Test
    public void testGetType() {
        final Key auth = new Key(RandomHelper.pseudoRandomBytes(32));
        assertEquals("key", auth.getType());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testApplicationPojo() throws ValidationError, SerializationError {
        @SuppressWarnings("WeakerAccess")
        class Pojo {
            public Integer number;
            public List<String> list;
            public Pojo(Integer number, List<String> list) {
                this.number = number;
                this.list = list;
            }
        }

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        final Pojo pojo = new Pojo(42, list);

        final Application original = new Application(pojo);
        final Application returned = this.roundTrip(original);
        assertEquals("application", returned.getType());

        Map<String, Object> data = (HashMap<String, Object>) returned.getData();
        assertEquals(Integer.class, data.get("number").getClass());
        assertEquals(42, data.get("number"));
        assertEquals(ArrayList.class, data.get("list").getClass());
        assertEquals(2, ((ArrayList)data.get("list")).size());
    }

    @Test
    public void testGenericApplicationData() throws ValidationError, SerializationError {
        Integer number = 42;
        final Application original = new Application(number);
        final Application returned = this.roundTrip(original);
        assertEquals("application", returned.getType());
        assertEquals(number, returned.getData());
    }

    @Test
    public void testDisconnectedRoundtrip() throws SerializationError, ValidationError {
        final Disconnected original = new Disconnected((short) 13);
        final Disconnected returned = this.roundTrip(original);
        assertEquals(original.getId(), returned.getId());
    }

}

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
import org.saltyrtc.client.messages.Auth;
import org.saltyrtc.client.messages.ClientAuth;
import org.saltyrtc.client.messages.ClientHello;
import org.saltyrtc.client.messages.Data;
import org.saltyrtc.client.messages.DropResponder;
import org.saltyrtc.client.messages.InitiatorServerAuth;
import org.saltyrtc.client.messages.Key;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.NewInitiator;
import org.saltyrtc.client.messages.NewResponder;
import org.saltyrtc.client.messages.ResponderServerAuth;
import org.saltyrtc.client.messages.Restart;
import org.saltyrtc.client.messages.SendError;
import org.saltyrtc.client.messages.ServerHello;
import org.saltyrtc.client.messages.Token;

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
    public void testInitiatorServerAuthRoundtrip() throws SerializationError, ValidationError {
        final InitiatorServerAuth original = new InitiatorServerAuth(
                RandomHelper.pseudoRandomBytes(16), asList(1, 2, 3));
        final InitiatorServerAuth returned = roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertArrayEquals(original.getResponders().toArray(), returned.getResponders().toArray());
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

    @Test
    public void testNewInitiatorRoundtrip() throws SerializationError, ValidationError {
        final NewInitiator original = new NewInitiator();
        // There are no real fields in this message, so let's just ensure that there are no exceptions
        roundTrip(original);
    }

    @Test
    public void testNewResponderRoundtrip() throws SerializationError, ValidationError {
        final NewResponder original = new NewResponder(42);
        final NewResponder returned = roundTrip(original);
        assertEquals(original.getId(), returned.getId());
    }

    @Test
    public void testNewResponderValidation() throws SerializationError, ValidationError {
        final NewResponder original = new NewResponder(0xff + 1);
        try {
            roundTrip(original);
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("id must be < 255", e.getMessage());
        }
    }

    @Test
    public void testDropResponderRoundtrip() throws SerializationError, ValidationError {
        final DropResponder original = new DropResponder(42);
        final DropResponder returned = roundTrip(original);
        assertEquals(original.getId(), returned.getId());
    }

    @Test
    public void testDropResponderValidation() throws SerializationError, ValidationError {
        final DropResponder original = new DropResponder(0xff + 1);
        try {
            roundTrip(original);
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("id must be < 255", e.getMessage());
        }
    }

    @Test
    public void testSendErrorRoundtrip() throws SerializationError, ValidationError {
        final SendError original = new SendError(RandomHelper.pseudoRandomBytes(32));
        final SendError returned = roundTrip(original);
        assertArrayEquals(original.getHash(), returned.getHash());
    }

    @Test
    public void testTokenRoundtrip() throws SerializationError, ValidationError {
        final Token original = new Token(RandomHelper.pseudoRandomBytes(32));
        final Token returned = roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
    }

    @Test
    public void testKeyRoundtrip() throws SerializationError, ValidationError {
        final Key original = new Key(RandomHelper.pseudoRandomBytes(32));
        final Key returned = roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
    }

    @Test
    public void testAuthRoundtrip() throws SerializationError, ValidationError {
        final Auth original = new Auth(RandomHelper.pseudoRandomBytes(16));
        final Auth returned = roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
    }

    @Test
    public void testRestartRoundtrip() throws SerializationError, ValidationError {
        final Restart original = new Restart();
        // There are no real fields in this message, so let's just ensure that there are no exceptions
        roundTrip(original);
    }

    @Test
    public void testGetType() {
        final Auth auth = new Auth(RandomHelper.pseudoRandomBytes(32));
        assertEquals("auth", auth.getType());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDataPojo() throws ValidationError, SerializationError {

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
        Pojo pojo = new Pojo(42, list);

        final Data original = new Data(pojo);
        final Data returned = roundTrip(original);
        assertEquals("data", returned.getType());
        assertNull(returned.getDataType());

        Map<String, Object> data = (HashMap<String, Object>) returned.getData();
        assertEquals(Integer.class, data.get("number").getClass());
        assertEquals(42, data.get("number"));
        assertEquals(ArrayList.class, data.get("list").getClass());
        assertEquals(2, ((ArrayList)data.get("list")).size());
    }

    @Test
    public void testGenericData() throws ValidationError, SerializationError {
        Integer number = 42;
        final Data original = new Data(number);
        final Data returned = roundTrip(original);
        assertEquals("data", returned.getType());
        assertNull(returned.getDataType());
        assertEquals(number, returned.getData());
    }
}

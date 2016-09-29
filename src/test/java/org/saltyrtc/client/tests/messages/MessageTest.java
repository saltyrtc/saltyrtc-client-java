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
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.c2c.Close;
import org.saltyrtc.client.messages.c2c.InitiatorAuth;
import org.saltyrtc.client.messages.c2c.Key;
import org.saltyrtc.client.messages.c2c.ResponderAuth;
import org.saltyrtc.client.messages.c2c.Token;
import org.saltyrtc.client.messages.s2c.ClientAuth;
import org.saltyrtc.client.messages.s2c.ClientHello;
import org.saltyrtc.client.messages.s2c.DropResponder;
import org.saltyrtc.client.messages.s2c.InitiatorServerAuth;
import org.saltyrtc.client.messages.s2c.NewInitiator;
import org.saltyrtc.client.messages.s2c.NewResponder;
import org.saltyrtc.client.messages.s2c.ResponderServerAuth;
import org.saltyrtc.client.messages.s2c.SendError;
import org.saltyrtc.client.messages.s2c.ServerHello;
import org.saltyrtc.client.signaling.CloseCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        final ClientHello returned = this.roundTrip(original);
        assertArrayEquals(original.getKey(), returned.getKey());
    }

    @Test
    public void testClientAuthRoundtrip() throws SerializationError, ValidationError {
        final List<String> subprotocols = asList("v1.saltyrtc.org", "some.other.protocol");
        final ClientAuth original = new ClientAuth(RandomHelper.pseudoRandomBytes(16), subprotocols);
        final ClientAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertArrayEquals(original.getSubprotocols().toArray(), returned.getSubprotocols().toArray());
    }

    @Test
    public void testInitiatorServerAuthRoundtrip() throws SerializationError, ValidationError {
        final InitiatorServerAuth original = new InitiatorServerAuth(
                RandomHelper.pseudoRandomBytes(16), asList(1, 2, 3));
        final InitiatorServerAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertArrayEquals(original.getResponders().toArray(), returned.getResponders().toArray());
    }

    @Test
    public void testResponderServerAuthRoundtrip() throws SerializationError, ValidationError {
        final ResponderServerAuth original = new ResponderServerAuth(
                RandomHelper.pseudoRandomBytes(16), false);
        final ResponderServerAuth returned = this.roundTrip(original);
        assertArrayEquals(original.getYourCookie(), returned.getYourCookie());
        assertFalse(original.isInitiatorConnected());
        assertFalse(returned.isInitiatorConnected());
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
    public void testNewResponderValidation() throws SerializationError, ValidationError {
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
        final SendError returned = this.roundTrip(original);
        assertArrayEquals(original.getHash(), returned.getHash());
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
    public void testCloseValidation() throws SerializationError, ValidationError {
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

}

/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.helpers;

import org.junit.Test;
import org.saltyrtc.client.helpers.MessageHistory;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.s2c.ClientHello;
import org.saltyrtc.client.messages.s2c.DropResponder;
import org.saltyrtc.client.messages.s2c.ServerHello;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class MessageHistoryTest {

    private static byte[] sha256sum(byte[] bytes) {
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            fail();
            return null;
        }
        md.update(bytes);
        return md.digest();
    }

    @Test
    public void testBytesHex() {
        byte[] b = new byte[] { 1, 2, 31 };
        Message m = new DropResponder(1);
        MessageHistory h = new MessageHistory(10);
        h.store(m, b);
        assertEquals(m, h.find(sha256sum(b)));
        assertEquals(m, h.find(NaCl.asHex(sha256sum(b))));
    }

    @Test
    public void testLimit() {
        byte[] a = new byte[] { 1, 2 };
        byte[] b = new byte[] { 3, 4 };
        byte[] c = new byte[] { 5, 6 };

        Message m1 = new ClientHello(new byte[] { 7 });
        Message m2 = new ServerHello(new byte[] { 8 });
        Message m3 = new DropResponder(9);

        MessageHistory history = new MessageHistory(2);
        assertEquals(0, history.size());

        // Store first entry
        history.store(m1, a);
        assertEquals(m1, history.find(sha256sum(a)));
        assertNull(history.find(sha256sum(b)));
        assertEquals(1, history.size());

        // Store second entry
        history.store(m2, b);
        assertEquals(m1, history.find(sha256sum(a)));
        assertEquals(m2, history.find(sha256sum(b)));
        assertEquals(2, history.size());

        // Store third entry
        history.store(m3, c);
        assertNull(history.find(sha256sum(a)));
        assertEquals(m2, history.find(sha256sum(b)));
        assertEquals(m3, history.find(sha256sum(c)));
        assertEquals(2, history.size());
    }

    /**
     * If two messages with the same hash are stored, only the latter will be returned.
     */
    @Test
    public void testCollision() {
        byte[] a = new byte[] { 1, 2 };
        byte[] b = new byte[] { 1, 2 };
        Message m1 = new DropResponder(1);
        Message m2 = new DropResponder(2);

        MessageHistory history = new MessageHistory(2);
        assertEquals(0, history.size());

        history.store(m1, a);
        history.store(m2, b);
        assertEquals(1, history.size());
        assertEquals(m2, history.find(sha256sum(a)));
        assertEquals(m2, history.find(sha256sum(b)));
    }

}

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
import org.saltyrtc.client.helpers.RandomHelper;
import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.messages.s2c.ClientHello;
import org.saltyrtc.client.messages.s2c.DropResponder;
import org.saltyrtc.client.messages.s2c.ServerHello;
import org.saltyrtc.client.nonce.SignalingChannelNonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MessageHistoryTest {

    @Test
    public void testBytesHex() {
        Message m = new DropResponder(1);
        Short source = 0x13;
        Short destination = 0xff;
        SignalingChannelNonce nonce = new SignalingChannelNonce(RandomHelper.pseudoRandomBytes(16), source, destination, 0, 2147483648L);
        MessageHistory h = new MessageHistory(10);
        h.store(m, nonce);
        byte[] key = MessageHistory.getMessageKey(nonce);
        String keyString = "13ff000080000000";
        assertEquals(m, h.find(key));
        assertEquals(m, h.find(keyString));
    }

    @Test
    public void testLimit() {
        SignalingChannelNonce a = new SignalingChannelNonce(RandomHelper.pseudoRandomBytes(16), (short)1, (short)2, 3, 4);
        SignalingChannelNonce b = new SignalingChannelNonce(RandomHelper.pseudoRandomBytes(16), (short)1, (short)3, 4, 5);
        SignalingChannelNonce c = new SignalingChannelNonce(RandomHelper.pseudoRandomBytes(16), (short)4, (short)5, 5, 6);

        Message m1 = new ClientHello(new byte[] { 7 });
        Message m2 = new ServerHello(new byte[] { 8 });
        Message m3 = new DropResponder(9);

        MessageHistory history = new MessageHistory(2);
        assertEquals(0, history.size());

        // Store first entry
        history.store(m1, a);
        assertEquals(m1, history.find(MessageHistory.getMessageKey(a)));
        assertNull(history.find(MessageHistory.getMessageKey(b)));
        assertEquals(1, history.size());

        // Store second entry
        history.store(m2, b);
        assertEquals(m1, history.find(MessageHistory.getMessageKey(a)));
        assertEquals(m2, history.find(MessageHistory.getMessageKey(b)));
        assertEquals(2, history.size());

        // Store third entry
        history.store(m3, c);
        assertNull(history.find(MessageHistory.getMessageKey(a)));
        assertEquals(m2, history.find(MessageHistory.getMessageKey(b)));
        assertEquals(m3, history.find(MessageHistory.getMessageKey(c)));
        assertEquals(2, history.size());
    }

    /**
     * If two messages with the same hash are stored, only the latter will be returned.
     */
    @Test
    public void testCollision() {
        byte[] cookie = RandomHelper.pseudoRandomBytes(16);
        SignalingChannelNonce a = new SignalingChannelNonce(cookie, (short)1, (short)2, 3, 4);
        SignalingChannelNonce b = new SignalingChannelNonce(cookie, (short)1, (short)2, 3, 4);

        Message m1 = new DropResponder(1);
        Message m2 = new DropResponder(2);

        MessageHistory history = new MessageHistory(2);
        assertEquals(0, history.size());

        history.store(m1, a);
        history.store(m2, b);
        assertEquals(1, history.size());
        assertEquals(m2, history.find(MessageHistory.getMessageKey(a)));
        assertEquals(m2, history.find(MessageHistory.getMessageKey(b)));
    }

}

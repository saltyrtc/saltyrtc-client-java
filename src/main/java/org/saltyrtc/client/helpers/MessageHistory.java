/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

import org.saltyrtc.client.messages.Message;
import org.saltyrtc.client.nonce.SignalingChannelNonce;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This is a fixed-size FIFO linked hash map that only keeps a limited number of entries.
 *
 * It is used to "remember" the sent messages.
 */
public class MessageHistory {

    private final int maxSize;
    // TODO: The forced synchronization could probably be removed by using a synchronized map.
    private final Map<String, Message> history;

    private class LimitedHashMap<K, V> extends LinkedHashMap<K, V> {
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return this.size() > maxSize;
        }
    }

    public MessageHistory(int maxSize) {
        this.maxSize = maxSize;
        this.history = new LimitedHashMap<>();
    }

    /**
     * Store the message in the history.
     */
    public synchronized void store(Message message, SignalingChannelNonce nonce) {
        final byte[] key = MessageHistory.getMessageKey(nonce);
        this.history.put(NaCl.asHex(key).toLowerCase(), message);
    }

    /**
     * Return the message key according to the specification:
     *
     * The concatenation of the source address, the destination address, the overflow number and the
     * sequence number of the nonce section from the original message.
     */
    public static byte[] getMessageKey(SignalingChannelNonce nonce) {
        final ByteBuffer buf = ByteBuffer.allocate(8);
        buf.put(UnsignedHelper.getUnsignedByte(nonce.getSource()));
        buf.put(UnsignedHelper.getUnsignedByte(nonce.getDestination()));
        buf.putShort(UnsignedHelper.getUnsignedShort(nonce.getOverflow()));
        buf.putInt(UnsignedHelper.getUnsignedInt(nonce.getSequence()));
        return buf.array();
    }

    /**
     * Look up a sent message type by message key.
     *
     * If message is not found, null is returned.
     */
    public synchronized Message find(byte[] key) {
        return this.history.get(NaCl.asHex(key).toLowerCase());
    }

    /**
     * Look up a sent message by message key hex string.
     */
    public synchronized Message find(String key) {
        return this.history.get(key.toLowerCase());
    }

    public synchronized int size() {
        return this.history.size();
    }
}

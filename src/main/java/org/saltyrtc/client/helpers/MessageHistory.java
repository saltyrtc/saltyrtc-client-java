/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

import com.neilalexander.jnacl.NaCl;

import org.saltyrtc.client.messages.Message;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
     *
     * You also need to pass in the actually sent message bytes
     * in order for this implementation to calculate the hash sum.
     */
    public synchronized void store(Message message, byte[] sentData) {
        // Calculate SHA256 over encrypted bytes
        final MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 digest algorithm not available", e);
        }
        md.update(sentData);
        final byte[] sha256sum = md.digest();

        // Store message
        this.history.put(NaCl.asHex(sha256sum).toLowerCase(), message);
    }

    /**
     * Look up a sent message type by SHA256 hash.
     *
     * If message is not found, null is returned.
     */
    public synchronized Message find(byte[] sha256Sum) {
        return this.history.get(NaCl.asHex(sha256Sum).toLowerCase());
    }

    /**
     * Look up a sent message by SHA256 hash hex string.
     */
    public synchronized Message find(String sha256Sum) {
        return this.history.get(sha256Sum.toLowerCase());
    }

    public synchronized int size() {
        return this.history.size();
    }
}

/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import com.neilalexander.jnacl.NaCl;

import java.nio.ByteBuffer;

/**
 * A SaltyRTC nonce.
 *
 * Nonce structure:
 *
 * |CCCCCCCCCCCCCCCC|II|OO|QQQQ|
 *
 * - C: Cookie (16 byte)
 * - I: Data channel ID (2 byte)
 * - O: Overflow number (2 byte)
 * - Q: Sequence number (4 byte)
 */
public class Nonce {

    private byte[] cookie;
    private int channelId;
    private int overflow;
    private long sequence;

    public static int COOKIE_LENGTH = 16;
    public static int TOTAL_LENGTH = NaCl.NONCEBYTES;

    /**
     * Create a new nonce.
     *
     * Note that due to the lack of unsigned data types in Java, we'll use
     * larger signed types. That means that the user must check that the values
     * are in the correct range. If the arguments are out of range, an
     * unsigned `IllegalArgumentException` is thrown.
     *
     * See also: http://stackoverflow.com/a/397997/284318.
     */
    public Nonce(byte[] cookie, int channelId, int overflow, long sequence) {
        validateCookie(cookie);
        validateChannelId(channelId);
        validateOverflow(overflow);
        validateSequence(sequence);
        this.cookie = cookie;
        this.channelId = channelId;
        this.overflow = overflow;
        this.sequence = sequence;
    }

    /**
     * Create a new nonce from raw binary data.
     */
    public Nonce(ByteBuffer buf) {
        if (buf.limit() < TOTAL_LENGTH) {
            throw new IllegalArgumentException("Buffer limit must be at least " + TOTAL_LENGTH);
        }

        final byte[] cookie = new byte[COOKIE_LENGTH];
        buf.get(cookie, 0, COOKIE_LENGTH);
        validateCookie(cookie);

        final int channelId = ((int)buf.getShort()) & 0xFFFF;
        validateChannelId(channelId);

        final int overflow = ((int)buf.getShort()) & 0xFFFF;
        validateOverflow(overflow);

        final long sequence = ((long)buf.getInt()) & 0xFFFFFFFFL;
        validateSequence(sequence);

        this.cookie = cookie;
        this.channelId = channelId;
        this.overflow = overflow;
        this.sequence = sequence;
    }

    /**
     * A cookie should be 16 bytes long.
     */
    private void validateCookie(byte[] cookie) {
        if (cookie.length != COOKIE_LENGTH) {
            throw new IllegalArgumentException("cookie must be " + COOKIE_LENGTH + " bytes long");
        }
    }

    /**
     * A channel id should be an uint16.
     */
    private void validateChannelId(int channelId) {
        if (channelId < 0 || channelId >= (1 << 16)) {
            throw new IllegalArgumentException("channelId must be between 0 and 2**16-1");
        }
    }

    /**
     * An overflow number should be an uint16.
     */
    private void validateOverflow(int overflow) {
        if (overflow < 0 || overflow >= (1 << 16)) {
            throw new IllegalArgumentException("overflow must be between 0 and 2**16-1");
        }
    }

    /**
     * A sequence should be an uint32.
     */
    private void validateSequence(long sequence) {
        if (sequence < 0 || sequence >= (1L << 32)) {
            throw new IllegalArgumentException("sequence must be between 0 and 2**32-1");
        }
    }

    public byte[] getCookie() {
        return this.cookie;
    }

    /**
     * Return the channel id.
     */
    public int getChannelId() {
        return this.channelId;
    }

    /**
     * Return the channel id.
     */
    public int getOverflow() {
        return this.overflow;
    }

    /**
     * Return the sequence number.
     */
    public long getSequence() {
        return this.sequence;
    }

    /**
     * Return the combined sequence number.
     */
    public long getCombinedSequence() {
        long combined = (long)this.overflow << 32 | this.sequence;
        assert combined >= 0 && combined < (1L << 48); // Sanity check
        return combined;
    }

}
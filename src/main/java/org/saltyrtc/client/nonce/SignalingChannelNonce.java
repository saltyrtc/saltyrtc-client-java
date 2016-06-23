/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.nonce;

import org.saltyrtc.client.helpers.UnsignedHelper;

import java.nio.ByteBuffer;

/**
 * A SaltyRTC signaling nonce.
 *
 * Nonce structure:
 *
 * |CCCCCCCCCCCCCCCC|S|D|OO|QQQQ|
 *
 * - C: Cookie (16 byte)
 * - S: Source byte (1 byte)
 * - D: Destination byte (1 byte)
 * - O: Overflow number (2 byte)
 * - Q: Sequence number (4 byte)
 */
public class SignalingChannelNonce extends Nonce {

    private short source;
    private short destination;

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
    public SignalingChannelNonce(byte[] cookie, short source, short destination, int overflow, long sequence) {
        validateCookie(cookie);
        validateSource(source);
        validateDestination(destination);
        validateOverflow(overflow);
        validateSequence(sequence);
        this.cookie = cookie;
        this.source = source;
        this.destination = destination;
        this.overflow = overflow;
        this.sequence = sequence;
    }

    /**
     * Create a new nonce from raw binary data.
     */
    public SignalingChannelNonce(ByteBuffer buf) {
        if (buf.limit() < TOTAL_LENGTH) {
            throw new IllegalArgumentException("Buffer limit must be at least " + TOTAL_LENGTH);
        }

        final byte[] cookie = new byte[COOKIE_LENGTH];
        buf.get(cookie, 0, COOKIE_LENGTH);
        validateCookie(cookie);

        final short source = UnsignedHelper.readUnsignedByte(buf.get());
        validateSource(source);

        final short destination = UnsignedHelper.readUnsignedByte(buf.get());
        validateDestination(destination);

        final int overflow = UnsignedHelper.readUnsignedShort(buf.getShort());
        validateOverflow(overflow);

        final long sequence = UnsignedHelper.readUnsignedInt(buf.getInt());
        validateSequence(sequence);

        this.cookie = cookie;
        this.source = source;
        this.destination = destination;
        this.overflow = overflow;
        this.sequence = sequence;
    }

    /**
     * A source byte should be an uint8.
     */
    private void validateSource(short source) {
        if (source < 0 || source >= (1 << 8)) {
            throw new IllegalArgumentException("source must be between 0 and 2**8-1");
        }
    }

    /**
     * A destination byte should be an uint8.
     */
    private void validateDestination(short destination) {
        if (destination < 0 || destination >= (1 << 8)) {
            throw new IllegalArgumentException("destination must be between 0 and 2**8-1");
        }
    }

    /**
     * Return the source byte.
     */
    public short getSource() {
        return this.source;
    }

    /**
     * Return the destination byte.
     */
    public short getDestination() {
        return this.destination;
    }

}
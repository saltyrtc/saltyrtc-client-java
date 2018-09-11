/*
 * Copyright (c) 2016-2018 Threema GmbH
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
     * unchecked `IllegalArgumentException` is thrown.
     *
     * See also: http://stackoverflow.com/a/397997/284318.
     */
    public SignalingChannelNonce(byte[] cookie, short source, short destination, int overflow, long sequence) {
        this.validateCookie(cookie);
        this.validateSource(source);
        this.validateDestination(destination);
        this.validateOverflow(overflow);
        this.validateSequence(sequence);
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
        this.validateCookie(cookie);

        final short source = UnsignedHelper.readUnsignedByte(buf.get());
        this.validateSource(source);

        final short destination = UnsignedHelper.readUnsignedByte(buf.get());
        this.validateDestination(destination);

        final int overflow = UnsignedHelper.readUnsignedShort(buf.getShort());
        this.validateOverflow(overflow);

        final long sequence = UnsignedHelper.readUnsignedInt(buf.getInt());
        this.validateSequence(sequence);

        this.cookie = cookie;
        this.source = source;
        this.destination = destination;
        this.overflow = overflow;
        this.sequence = sequence;
    }

    @Override
    public byte[] toBytes() {
        // Pack data
        ByteBuffer buffer = ByteBuffer.allocate(Nonce.TOTAL_LENGTH);
        buffer.put(this.cookie);
        buffer.put(UnsignedHelper.getUnsignedByte(this.source));
        buffer.put(UnsignedHelper.getUnsignedByte(this.destination));
        buffer.putShort(UnsignedHelper.getUnsignedShort(this.overflow));
        buffer.putInt(UnsignedHelper.getUnsignedInt(this.sequence));

        // Return underlying array
        return buffer.array();
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

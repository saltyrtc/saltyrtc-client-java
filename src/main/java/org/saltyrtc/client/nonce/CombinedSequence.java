/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.nonce;

import org.saltyrtc.client.exceptions.OverflowException;

import java.security.SecureRandom;

/**
 * The CombinedSequence class handles the overflow checking of the 48 bit combined sequence number
 * (CSN) consisting of the sequence number and the overflow number.
 */
public class CombinedSequence {
    public static long SEQUENCE_NUMBER_MAX = 0x100000000L; // 1<<32
    public static int OVERFLOW_MAX = 0x100000; // 1<<16

    private long sequenceNumber;
    private int overflow;

    public CombinedSequence() {
        final SecureRandom sr = new SecureRandom();
        this.sequenceNumber = sr.nextLong() & 0xffffffffL;
        this.overflow = 0;
    }

    public CombinedSequence(long sequenceNumber, int overflow) {
        this.sequenceNumber = sequenceNumber;
        this.overflow = overflow;
    }

    /**
     * Return the sequence number.
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Return the overflow number.
     */
    public int getOverflow() {
        return overflow;
    }

    /**
     * Return the combined sequence number.
     */
    public long getCombinedSequence() {
        long combined = (long)this.overflow << 32 | this.sequenceNumber;
        assert combined >= 0 && combined < (1L << 48); // Sanity check
        return combined;
    }

    /**
     * Increment the combined sequence number and return reference to itself.
     *
     * May throw an error if overflow number overflows. This is extremely unlikely and must be
     * treated as a protocol error.
     */
    public CombinedSequence next() throws OverflowException {
        if (this.sequenceNumber + 1 >= CombinedSequence.SEQUENCE_NUMBER_MAX) {
            // Sequence number overflow
            this.sequenceNumber = 0;
            this.overflow += 1;
            if (this.overflow >= CombinedSequence.OVERFLOW_MAX) {
                // Overflow overflow (ha-ha)
                throw new OverflowException("Overflow number overflow");
            }
        } else {
            // Simply increment the sequence number
            this.sequenceNumber += 1;
        }
        return this;
    }
}

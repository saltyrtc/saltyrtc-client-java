/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.nonce;

/**
 * An immutable snapshot of a combined sequence.
 *
 * This type is returned by the .next() method on a CombinedSequence instance.
 */
public class CombinedSequenceSnapshot {
    private final long sequenceNumber;
    private final int overflow;

    CombinedSequenceSnapshot(long sequenceNumber, int overflow) {
        this.sequenceNumber = sequenceNumber;
        this.overflow = overflow;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public int getOverflow() {
        return overflow;
    }

    public long getCombinedSequence() {
        long combined = (long)this.overflow << 32 | this.sequenceNumber;
        assert combined >= 0 && combined < (1L << 48); // Sanity check
        return combined;
    }
}

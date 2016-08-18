/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.nonce;

/**
 * A SaltyRTC CSN pair.
 */
public class CombinedSequencePair {
    private CombinedSequence ours;
    private long theirs;

    public CombinedSequencePair() {
        this.ours = new CombinedSequence();
    }

    public CombinedSequencePair(long theirs) {
        this();
        this.setTheirs(theirs);
    }

    public CombinedSequencePair(CombinedSequence ours, long theirs) {
        this.ours = ours;
        this.theirs = theirs;
    }

    public CombinedSequence getOurs() {
        return ours;
    }

    public long getTheirs() {
        return theirs;
    }

    public void setTheirs(long theirs) {
        this.theirs = theirs;
    }
}

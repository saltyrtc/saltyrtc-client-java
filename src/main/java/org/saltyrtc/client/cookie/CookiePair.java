/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.cookie;

/**
 * A SaltyRTC cookie pair.
 */
public class CookiePair {
    private Cookie ours;
    private Cookie theirs;

    public CookiePair() {
        this.ours = new Cookie();
    }

    public CookiePair(Cookie ours, Cookie theirs) {
        this.ours = ours;
        this.theirs = theirs;
    }

    public Cookie getOurs() {
        return ours;
    }

    public boolean hasTheirs() {
        return this.theirs != null;
    }

    public Cookie getTheirs() {
        return theirs;
    }

    public void setTheirs(Cookie theirs) {
        this.theirs = theirs;
    }
}

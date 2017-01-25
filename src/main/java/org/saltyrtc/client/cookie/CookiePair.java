/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */
package org.saltyrtc.client.cookie;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.exceptions.ProtocolException;

/**
 * A SaltyRTC cookie pair.
 *
 * The implementation ensures that local and peer cookie are never the same.
 */
public class CookiePair {
    private final Cookie ours;
    private Cookie theirs = null;

    public CookiePair() {
        this.ours = new Cookie();
    }

	/**
     * Create a new cookie pair with a predefined peer cookie.
     */
    public CookiePair(Cookie theirs) {
        Cookie cookie;
        do {
            cookie = new Cookie();
        } while (cookie.equals(theirs));
        this.ours = cookie;
        this.theirs = theirs;
    }

	/**
     * Create a new cookie pair.
     *
     * May throw ProtocolException if both cookies are the same.
     */
    public CookiePair(Cookie ours, Cookie theirs) throws ProtocolException {
        if (theirs.equals(ours)) {
            throw new ProtocolException("Their cookie matches our cookie");
        }
        this.ours = ours;
        this.theirs = theirs;
    }

    @NonNull
    public Cookie getOurs() {
        return this.ours;
    }

    public boolean hasTheirs() {
        return this.theirs != null;
    }

    @Nullable
    public Cookie getTheirs() {
        return this.theirs;
    }

	/**
     * Set peer cookie.
     *
     * May throw ProtocolException if peer cookie matches our cookie.
     */
    public void setTheirs(Cookie theirs) throws ProtocolException {
        if (theirs.equals(this.ours)) {
            throw new ProtocolException("Their cookie matches our cookie");
        }
        this.theirs = theirs;
    }
}

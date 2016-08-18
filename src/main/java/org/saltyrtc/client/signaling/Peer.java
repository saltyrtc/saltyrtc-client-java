/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.nonce.CombinedSequencePair;

public abstract class Peer {
    protected byte[] permanentKey;
    protected byte[] sessionKey;
    protected CombinedSequencePair csnPair;
    protected CookiePair cookiePair;

    public Peer() {
        this.csnPair = new CombinedSequencePair();
        this.cookiePair = null;
    }

    public Peer(byte[] permanentKey) {
        this();
        this.permanentKey = permanentKey;
    }

    public byte[] getPermanentKey() {
        return this.permanentKey;
    }

    public void setPermanentKey(byte[] permanentKey) {
        this.permanentKey = permanentKey;
    }

    public byte[] getSessionKey() {
        return this.sessionKey;
    }

    public void setSessionKey(byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public CombinedSequencePair getCsnPair() {
        return this.csnPair;
    }

    public CookiePair getCookiePair() {
        return cookiePair;
    }
}

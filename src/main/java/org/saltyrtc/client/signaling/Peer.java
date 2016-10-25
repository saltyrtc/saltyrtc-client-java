/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.nonce.CombinedSequencePair;

/**
 * Either the server, the initiator or a responder.
 */
public abstract class Peer {
    short id;
    byte[] permanentKey;
    byte[] sessionKey;
    @NonNull
    private final CombinedSequencePair csnPair;
    @NonNull
    private CookiePair cookiePair;

	/**
     * Initialize a peer with just an ID.
     */
    public Peer(short id) {
        this.id = id;
        this.csnPair = new CombinedSequencePair();
        this.cookiePair = new CookiePair();
    }

	/**
     * Initialize a peer with an permanent key.
     */
    public Peer(short id, byte[] permanentKey) {
        this(id);
        this.permanentKey = permanentKey;
    }

	/**
     * Initialize a peer with a cookie pair.
     */
    public Peer(short id, CookiePair cookiePair) {
        this.id = id;
        this.cookiePair = cookiePair;
        this.csnPair = new CombinedSequencePair();
    }

    public short getId() {
        return id;
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

    @NonNull
    public CombinedSequencePair getCsnPair() {
        return this.csnPair;
    }

    /**
     * Return the cookie pair.
     */
    @NonNull
    public CookiePair getCookiePair() {
        return this.cookiePair;
    }
}

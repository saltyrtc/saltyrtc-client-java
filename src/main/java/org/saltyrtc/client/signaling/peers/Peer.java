/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling.peers;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.nonce.CombinedSequencePair;

/**
 * Either the server, the initiator or a responder.
 */
public abstract class Peer {
    // Receiver id
    private short id;

    // Permanent key of the peer
    @Nullable byte[] permanentKey;

    // Session key of the peer
    @Nullable byte[] sessionKey;

    // CSN pair
    @NonNull private final CombinedSequencePair csnPair;

    // Cookie pair
    @NonNull private CookiePair cookiePair;

	/**
     * Initialize a peer with just an ID.
     */
    public Peer(short id) {
        this.id = id;
        this.csnPair = new CombinedSequencePair();
        this.cookiePair = new CookiePair();
    }

	/**
     * Initialize a peer with a cookie pair.
     */
    public Peer(short id, @NonNull CookiePair cookiePair) {
        this.id = id;
        this.csnPair = new CombinedSequencePair();
        this.cookiePair = cookiePair;
    }

    public short getId() {
        return id;
    }

    @Nullable
    public byte[] getPermanentKey() {
        return this.permanentKey;
    }

    public void setPermanentKey(@Nullable byte[] permanentKey) {
        this.permanentKey = permanentKey;
    }

    public boolean hasPermanentKey() {
        return this.permanentKey != null;
    }

    @Nullable
    public byte[] getSessionKey() {
        return this.sessionKey;
    }

    public void setSessionKey(@Nullable byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public boolean hasSessionKey() {
        return this.sessionKey != null;
    }

    @NonNull
    public CombinedSequencePair getCsnPair() {
        return this.csnPair;
    }

    @NonNull
    public abstract String getName();

    /**
     * Return the cookie pair.
     */
    @NonNull
    public CookiePair getCookiePair() {
        return this.cookiePair;
    }
}

/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling.peers;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.annotations.Nullable;
import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.keystore.SharedKeyStore;
import org.saltyrtc.client.nonce.CombinedSequencePair;

/**
 * Either the server, the initiator or a responder.
 */
public abstract class Peer {
    // Receiver id
    private short id;

    // Permanent key of the peer
    @Nullable SharedKeyStore permanentSharedKey;

    // Session key of the peer
    @Nullable SharedKeyStore sessionSharedKey;

    // CSN pair
    @NonNull private final CombinedSequencePair csnPair;

    // Cookie pair
    @NonNull private CookiePair cookiePair;

	/**
     * Initialize a peer with just an ID.
     */
    @SuppressWarnings("WeakerAccess")
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
    public SharedKeyStore getPermanentSharedKey() {
        return this.permanentSharedKey;
    }

    /**
     * Set the permanent shared key for this peer.
     * @param remotePermanentKey The remote public permanent key.
     * @param localPermanentKey The local permanent keystore.
     * @throws InvalidKeyException Thrown if the `remotePermanentKey` is not a valid public key.
     */
    public void setPermanentSharedKey(
        @NonNull byte[] remotePermanentKey,
        @NonNull KeyStore localPermanentKey
    ) throws InvalidKeyException {
        this.permanentSharedKey = localPermanentKey.getSharedKeyStore(remotePermanentKey);
    }

    public boolean hasPermanentSharedKey() {
        return this.permanentSharedKey != null;
    }

    @Nullable
    public SharedKeyStore getSessionSharedKey() {
        return this.sessionSharedKey;
    }

    /**
     * Set the session shared key for this peer.
     * @param remoteSessionKey The remote public session key.
     * @param localSessionKey The local session keystore.
     * @throws InvalidKeyException Thrown if the `remoteSessionKey` is not a valid public key.
     */
    public void setSessionSharedKey(
        @NonNull byte[] remoteSessionKey,
        @NonNull KeyStore localSessionKey
    ) throws InvalidKeyException {
        this.sessionSharedKey = localSessionKey.getSharedKeyStore(remoteSessionKey);
    }

    public boolean hasSessionSharedKey() {
        return this.sessionSharedKey != null;
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

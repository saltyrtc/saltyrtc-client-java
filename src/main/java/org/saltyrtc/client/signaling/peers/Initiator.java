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
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.InvalidStateException;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.keystore.SharedKeyStore;
import org.saltyrtc.client.signaling.state.InitiatorHandshakeState;

/**
 * Information about the initiator. Used by responder during handshake.
 */
public class Initiator extends Peer {
    private static short ID = 0x01;

    private boolean connected;

    public InitiatorHandshakeState handshakeState;

    // This variable is used to temporarily store a local session key.
    // It is used later to create a SharedKeyStore with this responder,
    // but when the initiator sends its 'key' message to the responder
    // (encrypted with this newly created local session key), it does not yet
    // know the responder's public session key, so it cannot yet
    // create the SharedKeyStore.
    @Nullable
    private KeyStore tmpLocalSessionKey;

    public Initiator(
        @NonNull byte[] remotePermanentKey,
        @NonNull KeyStore localPermanentKey
    ) throws InvalidKeyException {
        super(Initiator.ID);
        this.setPermanentSharedKey(remotePermanentKey, localPermanentKey);
        this.connected = false;
        this.handshakeState = InitiatorHandshakeState.NEW;
    }

    @NonNull
    @Override
    public String getName() {
        return "Initiator";
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions") // Set in constructor
    public SharedKeyStore getPermanentSharedKey() {
        return this.permanentSharedKey;
    }

    /**
     * Return the temporary local session key.
     *
     * After calling this function, the temporary local session key will be set to null.
     *
     * @throws InvalidStateException Thrown if no local session key was set.
     */
    @NonNull
    public KeyStore extractTmpLocalSessionKey() throws InvalidStateException {
        final KeyStore ks = this.tmpLocalSessionKey;
        if (ks == null) {
            throw new InvalidStateException("Temporary local session key is null");
        }
        this.tmpLocalSessionKey = null;
        return ks;
    }

    /**
     * Set the temporary local session key for this responder.
     *
     * See class source code for more explanations.
     *
     * @param keystore The local session keystore.
     * @throws InvalidStateException Thrown if a local session keystore already exists.
     */
    public void setTmpLocalSessionKey(@Nullable KeyStore keystore) throws InvalidStateException  {
        if (this.tmpLocalSessionKey != null) {
            throw new InvalidStateException("tmpLocalSessionKey already set");
        }
        this.tmpLocalSessionKey = keystore;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}

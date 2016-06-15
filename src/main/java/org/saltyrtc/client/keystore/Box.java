/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.keystore;

import com.neilalexander.jnacl.NaCl;

import java.nio.ByteBuffer;


/**
 * A NaCl box.
 */
public class Box {
    private final byte[] nonce;
    private final byte[] data;

    /**
     * Create a new box from nonce and data.
     */
    public Box(byte[] nonce, byte[] data) {
        this.nonce = nonce;
        this.data = data;
    }

    /**
     * Create a new box from a ByteBuffer.
     *
     * @param buffer The ByteBuffer containing nonce and data.
     */
    public Box(ByteBuffer buffer) {
        // Unpack nonce
        this.nonce = new byte[NaCl.NONCEBYTES];
        buffer.get(nonce, 0, NaCl.NONCEBYTES);

        // Unpack data
        this.data = new byte[buffer.remaining()];
        buffer.get(data);
    }

    public byte[] getNonce() {
        return nonce;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * Return the size (in bytes) of the box.
     */
    public int getSize() {
        return this.nonce.length + this.data.length;
    }

    /**
     * Return a ByteBuffer containing nonce and data.
     */
    public ByteBuffer toBuffer() {
        // Pack data
        // Note: 'allocateDirect' does NOT work, DO NOT CHANGE!
        ByteBuffer box = ByteBuffer.allocate(this.getSize());
        box.put(this.nonce);
        box.put(this.data);

        // Flip offset and remaining length for reading
        box.flip();

        // Return box as byte buffer
        return box;
    }
}
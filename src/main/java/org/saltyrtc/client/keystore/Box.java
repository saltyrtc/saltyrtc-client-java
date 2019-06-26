/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.keystore;

import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * A NaCl box. It holds encrypted data as well as the corresponding nonce.
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
    public Box(ByteBuffer buffer, int nonceLength) {
        // Unpack nonce
        this.nonce = new byte[nonceLength];
        buffer.get(nonce, 0, nonceLength);

        // Unpack data
        this.data = new byte[buffer.remaining()];
        buffer.get(data);
    }

    /**
     * Return the nonce as byte array.
     */
    public byte[] getNonce() {
        return nonce;
    }

    /**
     * Return the data as byte array.
     */
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
     * Return a byte array containing nonce and data.
     */
    public byte[] toBytes() {
        // Pack data
        // Note: 'allocateDirect' does NOT work, DO NOT CHANGE!
        ByteBuffer box = ByteBuffer.allocate(this.getSize());
        box.put(this.nonce);
        box.put(this.data);

        // Return underlying array
        return box.array();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Box)) {
            return false;
        }
        final Box other = (Box) o;
        return Arrays.equals(this.data, other.getData())
            && Arrays.equals(this.nonce, other.getNonce());
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(nonce);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}

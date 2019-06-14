/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.cookie;

import org.saltyrtc.client.helpers.RandomHelper;

import java.util.Arrays;

/**
 * A SaltyRTC cookie.
 *
 * This data structure is immutable, but it can be cloned
 * using a copy constructor.
 */
public class Cookie {
    public static final int COOKIE_LENGTH = 16;

    private final byte[] bytes;

    public Cookie() {
        this.bytes = RandomHelper.secureRandomBytes(COOKIE_LENGTH);
    }

    public Cookie(byte[] bytes) {
        if (bytes.length != COOKIE_LENGTH) {
            throw new IllegalArgumentException(
                    "Bad cookie length, must be " + COOKIE_LENGTH + " bytes");
        }
        this.bytes = bytes;
    }

    public Cookie(Cookie cookie) {
        this.bytes = cookie.getBytes();
    }

    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Cookie cookie = (Cookie) o;
        return Arrays.equals(bytes, cookie.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}

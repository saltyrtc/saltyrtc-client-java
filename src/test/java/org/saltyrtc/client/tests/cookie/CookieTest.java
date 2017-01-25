/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.cookie;

import org.junit.Test;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.exceptions.SerializationError;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.MessageReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CookieTest {

    @Test
    public void testCookieLength() {
        Cookie cookie = new Cookie();
        assertEquals(cookie.getBytes().length, Cookie.COOKIE_LENGTH);
    }

    @Test
    public void testCookieComparable() {
        Cookie c1 = new Cookie();
        Cookie c2 = new Cookie();
        assertNotEquals(c1, c2);

        Cookie c3 = new Cookie(c2);
        assertEquals(c2, c3);
    }

    @Test
    public void testCookieRandomness() {
        Set<byte[]> cookieBytes = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            cookieBytes.add(new Cookie().getBytes());
        }
        assertEquals(20, cookieBytes.size());
    }

}

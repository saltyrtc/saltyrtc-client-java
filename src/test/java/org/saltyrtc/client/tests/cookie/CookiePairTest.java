/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.cookie;

import org.junit.Test;
import org.saltyrtc.client.cookie.Cookie;
import org.saltyrtc.client.cookie.CookiePair;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CookiePairTest {

    @Test
    public void testConstructorPredefined() {
        final Cookie ours = new Cookie();
        final Cookie theirs = new Cookie();
        final CookiePair pair = new CookiePair(ours, theirs);
        assertEquals(ours, pair.getOurs());
        assertEquals(theirs, pair.getTheirs());
    }

    @Test
    public void testConstructorRandom() {
        final CookiePair pair = new CookiePair();
        assertNotNull(pair.getOurs());
        assertNull(pair.getTheirs());
    }

}

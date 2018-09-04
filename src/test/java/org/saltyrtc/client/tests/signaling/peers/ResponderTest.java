/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.signaling.peers;

import org.junit.Test;
import org.saltyrtc.client.signaling.peers.Responder;

import static org.junit.Assert.assertEquals;

public class ResponderTest {

    @Test
    public void testIdCounter() {
        final Responder responder = new Responder((short) 7, 2);
        assertEquals(7, responder.getId());
        assertEquals(2, responder.getCounter());
    }

}

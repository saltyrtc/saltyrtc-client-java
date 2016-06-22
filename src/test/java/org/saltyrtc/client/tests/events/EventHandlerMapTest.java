/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.events;

import org.junit.Test;
import org.saltyrtc.client.events.EventHandler;
import org.saltyrtc.client.events.EventHandlerMap;
import org.saltyrtc.client.events.EventType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EventHandlerMapTest {

    @Test
    public void testGetDefault() {
        EventHandlerMap map = new EventHandlerMap();
        assertFalse(map.containsKey(EventType.DATA));
        Set<EventHandler> handlers = map.get(EventType.DATA);
        assertNotNull(handlers);
        assertEquals(0, handlers.size());
    }

    @Test
    public void testGetExisting() {
        EventHandlerMap map = new EventHandlerMap();
        assertFalse(map.containsKey(EventType.DATA));
        map.put(EventType.DATA, new HashSet<EventHandler>());
        assertTrue(map.containsKey(EventType.DATA));
        Set<EventHandler> handlers = map.get(EventType.DATA);
        handlers.add(new EventHandler<String>() {
            @Override
            public boolean handle(EventType type, String event) {
                return false;
            }
        });
        assertEquals(1, map.get(EventType.DATA).size());
    }

}

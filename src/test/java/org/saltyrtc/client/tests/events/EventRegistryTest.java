/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.events;

import org.junit.Before;
import org.junit.Test;
import org.saltyrtc.client.events.EventHandler;
import org.saltyrtc.client.events.EventRegistry;
import org.saltyrtc.client.events.EventType;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EventRegistryTest {

    private EventHandler<String> handler1;
    private EventHandler<String> handler2;

    @Before
    public void setUp() throws Exception {
        this.handler1 = new EventHandler<String>() {
            @Override
            public boolean handle(EventType type, String event) {
                return false;
            }
        };
        this.handler2 = new EventHandler<String>() {
            @Override
            public boolean handle(EventType type, String event) {
                return true;
            }
        };
    }

    @Test
    public void testRegister() {
        final EventRegistry registry = new EventRegistry();
        registry.register(EventType.CONNECTED, this.handler1);
        final Set<EventHandler> handlers = registry.get(EventType.CONNECTED);
        assertEquals(1, handlers.size());
        assertTrue(handlers.contains(this.handler1));

        // Re-register same handler
        registry.register(EventType.CONNECTED, this.handler1);
        assertEquals(1, handlers.size());

        // Add another handler
        registry.register(EventType.CONNECTED, this.handler2);
        assertEquals(2, handlers.size());
    }

    @Test
    public void testRegisterMultiple() {
        final EventRegistry registry = new EventRegistry();
        final EventType[] types = new EventType[] { EventType.CONNECTED, EventType.DATA };
        registry.register(types, this.handler1);
        assertEquals(1, registry.get(EventType.CONNECTED).size());
        assertEquals(1, registry.get(EventType.DATA).size());
        assertEquals(0, registry.get(EventType.HANDOVER).size());
    }

    @Test
    public void testUnregister() {
        final EventRegistry registry = new EventRegistry();
        final EventType[] types = new EventType[] { EventType.CONNECTED, EventType.DATA };
        registry.register(types, this.handler1);
        assertEquals(1, registry.get(EventType.CONNECTED).size());
        assertEquals(1, registry.get(EventType.DATA).size());
        assertEquals(0, registry.get(EventType.HANDOVER).size());
        registry.unregister(EventType.DATA, this.handler1);
        assertEquals(1, registry.get(EventType.CONNECTED).size());
        assertEquals(0, registry.get(EventType.DATA).size());
        assertEquals(0, registry.get(EventType.HANDOVER).size());
    }

    @Test
    public void testUnregisterMultiple() {
        final EventRegistry registry = new EventRegistry();
        final EventType[] types = new EventType[] {
                EventType.CONNECTED,
                EventType.DATA,
                EventType.HANDOVER
        };
        registry.register(types, this.handler1);
        assertEquals(1, registry.get(EventType.CONNECTED).size());
        assertEquals(1, registry.get(EventType.DATA).size());
        assertEquals(1, registry.get(EventType.HANDOVER).size());
        registry.unregister(EventType.DATA, this.handler1);
        assertEquals(1, registry.get(EventType.CONNECTED).size());
        assertEquals(0, registry.get(EventType.DATA).size());
        assertEquals(1, registry.get(EventType.HANDOVER).size());
        registry.unregister(types, this.handler1);
        assertEquals(0, registry.get(EventType.CONNECTED).size());
        assertEquals(0, registry.get(EventType.DATA).size());
        assertEquals(0, registry.get(EventType.HANDOVER).size());
    }

    @Test
    public void testClear() {
        final EventRegistry registry = new EventRegistry();
        registry.register(EventType.DATA, this.handler1);
        registry.register(EventType.DATA, this.handler2);
        assertEquals(2, registry.get(EventType.DATA).size());
        registry.clear(EventType.DATA);
        assertEquals(0, registry.get(EventType.DATA).size());
    }

}

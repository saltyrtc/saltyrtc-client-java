/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.events;

import org.junit.Before;
import org.junit.Test;
import org.saltyrtc.client.events.Event;
import org.saltyrtc.client.events.EventHandler;
import org.saltyrtc.client.events.EventRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventRegistryTest {

    private EventRegistry<MessageEvent> registry;
    private List<String> messages;
    private EventHandler<MessageEvent> adder;
    private EventHandler<MessageEvent> clearer;

    static class MessageEvent implements Event {
        private String message;
        String getMessage() { return this.message; }
        MessageEvent(String message) { this.message = message; }
    }

    @Before
    public void setUp() {
        this.registry = new EventRegistry<>();
        this.messages = new ArrayList<>();
        this.adder = event -> {
            messages.add(event.getMessage());
            return false;
        };
        this.clearer = event -> {
            messages.clear();
            return false;
        };
    }

    @Test
    public void testRegistry() {
        this.registry.register(this.adder);
        assertEquals(0, messages.size());
        this.registry.notifyHandlers(new MessageEvent("hello"));
        assertEquals(1, messages.size());
        assertEquals("hello", messages.get(0));
    }

    @Test
    public void testRegisterTwice() {
        // Register same handler twice
        this.registry.register(this.adder);
        this.registry.register(this.adder);

        // Messages should still be empty
        assertEquals(0, messages.size());

        // Call adder twice
        this.registry.notifyHandlers(new MessageEvent("hello"));
        this.registry.notifyHandlers(new MessageEvent("hello"));

        // There should be 2 messages, not 4
        assertEquals(2, messages.size());

        // Add clearer, notify handler
        this.registry.register(this.clearer);
        this.registry.notifyHandlers(new MessageEvent("hello"));

        // The order of the handlers is not guaranteed, so there should be 0 or 1 messages
        assertTrue(messages.size() == 0 || messages.size() == 1);
    }

    @Test
    public void testClear() {
        // Register and call handler
        this.registry.register(this.adder);
        this.registry.notifyHandlers(new MessageEvent("hello"));
        this.registry.notifyHandlers(new MessageEvent("hello"));
        assertEquals(2, messages.size());

        // Clear handlers
        this.registry.clear();
        this.registry.notifyHandlers(new MessageEvent("hello"));
        assertEquals(2, messages.size());
    }

    @Test
    public void testRemoveItself() {
        // Register and call handler
        this.registry.register(new EventHandler<MessageEvent>() {
            private int counter = 0;
            @Override
            public boolean handle(MessageEvent event) {
                EventRegistryTest.this.messages.add("hi");
                return ++counter >= 2;
            }
        });
        // Counter is 1
        this.registry.notifyHandlers(new MessageEvent("hello"));
        // Counter is 2, remove itself
        this.registry.notifyHandlers(new MessageEvent("hello"));
        // Handler is gone
        this.registry.notifyHandlers(new MessageEvent("hello"));
        assertEquals(2, messages.size());
    }
}

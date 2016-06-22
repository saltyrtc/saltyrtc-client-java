/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

import java.util.Set;

/**
 * An event registry for all SaltyRTC related events.
 */
public class EventRegistry {

    private EventHandlerMap handlers = new EventHandlerMap();

    /**
     * Register a new event handler for the specified event type.
     *
     * @param type The event type.
     * @param handler The event handler.
     */
    public void register(EventType type, EventHandler handler) {
        this.handlers.get(type).add(handler);
    }

    /**
     * Register a new event handler for multiple event types.
     *
     * @param types The list of event types.
     * @param handler The event handler.
     */
    public void register(EventType[] types, EventHandler handler) {
        for (EventType type : types) {
            this.handlers.get(type).add(handler);
        }
    }

    /**
     * Unregister an event handler.
     *
     * @param type The event type.
     * @param handler The event handler.
     * @return Boolean indicating whether the handler was previously registered or not.
     */
    public boolean unregister(EventType type, EventHandler handler) {
        return this.handlers.get(type).remove(handler);
    }

    /**
     * Unregister an event handler from multiple events.
     *
     * @param types The list of event types.
     * @param handler The event handler.
     */
    public void unregister(EventType[] types, EventHandler handler) {
        for (EventType type : types) {
            this.handlers.get(type).remove(handler);
        }
    }

    /**
     * Unregister all event handlers for the specified type.
     *
     * @param type The event type.
     * @return The number of event handlers that were removed.
     */
    public int clear(EventType type) {
        final Set handlerSet = this.handlers.get(type);
        final int count = handlerSet.size();
        handlerSet.clear();
        return count;
    }

    /**
     * Get all event handlers for the specified event.
     *
     * @param type The event type.
     * @return The set of event handlers registered for this event.
     */
    public Set<EventHandler> get(EventType type) {
        return this.handlers.get(type);
    }

}

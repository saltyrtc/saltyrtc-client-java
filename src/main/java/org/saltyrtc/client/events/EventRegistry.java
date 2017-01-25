/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A registry for event handlers of a specific type.
 */
public class EventRegistry<E extends Event> {

    private final Set<EventHandler<E>> handlers = new CopyOnWriteArraySet<>();

    /**
     * Register a new event handler.
     */
    public void register(EventHandler<E> handler) {
        this.handlers.add(handler);
    }

    /**
     * Unregister an event handler.
     */
    public void unregister(EventHandler<E> handler) {
        this.handlers.remove(handler);
    }

    /**
     * Unregister all event handlers.
     */
    public void clear() {
        this.handlers.clear();
    }

    /**
     * Notify all handlers about the specified event.
     *
     * @param event Event instance containing more details.
     */
    public void notifyHandlers(E event) {
        for (EventHandler<E> handler : this.handlers) {
            final boolean removeHandler = handler.handle(event);
            if (removeHandler) {
                this.unregister(handler);
            }
        }
    }

}

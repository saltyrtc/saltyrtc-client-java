/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

/**
 * All event handlers need to implement this interface.
 */
public interface EventHandler<E> {

    /**
     * Handle an event.
     * @param type The EventType that caused this handler to be called.
     * @param event Event data.
     * @return If the handler returns `true`, it will be deregistered after returning.
     */
    boolean handle(EventType type, E event);

}

/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

/**
 * All event handlers need to implement this interface.
 */
public interface EventHandler<E extends Event> {
    /**
     * Handle an event.
     *
     * @param event An instance of the event that just happened.
     * @return A boolean flag indicating whether to remove this handler from the event registry.
     */
    boolean handle(E event);
}

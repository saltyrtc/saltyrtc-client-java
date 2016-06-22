/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A HashMap to store event handlers.
 *
 * When calling `map.get(k)` with an unknown `k`, the value will
 * automatically be initialized to an empty set.
 */
public class EventHandlerMap extends HashMap<EventType, HashSet<EventHandler>> {

    @Override
    public HashSet<EventHandler> get(Object key) {
        if (!containsKey(key)) {
            super.put((EventType) key, new HashSet<EventHandler>());
        }
        return super.get(key);
    }

}
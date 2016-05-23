/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import java.util.HashSet;

public class StateDispatcher {
    protected final HashSet<InternalStateListener> listeners = new HashSet<>();

    protected void state(final String state) {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                for (InternalStateListener listener : listeners) {
                    listener.handleState(state);
                }
            }
        });
    }

    protected void error(final String state, final String error) {
        Handler.post(new Runnable() {
            @Override
            public void run() {
                for (InternalStateListener listener : listeners) {
                    listener.handleError(state, error);
                }
            }
        });
    }

    // Ex protected
    public void addListener(InternalStateListener listener) {
        this.listeners.add(listener);
    }

    // Ex protected
    public void removeListener(InternalStateListener listener) {
        this.listeners.remove(listener);
    }
}

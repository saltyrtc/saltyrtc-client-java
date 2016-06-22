/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

/**
 * All possible SaltyRTC event types.
 */
public enum EventType {
    // Handshake has been completed, we're connected
    CONNECTED,
    // Handover to the data channel is done
    HANDOVER,
    // A WebSocket connection error occurred
    WS_CONNECTION_ERROR,
    // The WebSocket connection was closed
    WS_CONNECTION_CLOSED,
    // A new data message was received
    DATA,
}
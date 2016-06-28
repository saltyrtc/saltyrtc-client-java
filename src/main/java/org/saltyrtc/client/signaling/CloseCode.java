/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

/**
 * WebSocket close codes
 */
public class CloseCode {
    /**
     * Normal closing of websocket.
     */
    public static final int CLOSING_NORMAL = 1000;

    /**
     * The endpoint is going away.
     */
    public static final int GOING_AWAY = 1001;

    /**
     * No shared sub-protocol could be found.
     */
    public static final int SUBPROTOCOL_ERROR = 1002;

    /**
     * No free responder byte.
     */
    public static final int PATH_FULL = 3000;

    /**
     * Invalid message, invalid path length, ...
     */
    public static final int PROTOCOL_ERROR = 3001;

    /**
     * Syntax error, ...
     */
    public static final int INTERNAL_ERROR = 3002;

    /**
     * Handover to data channel.
     */
    public static final int HANDOVER = 3003;

    /**
     * Dropped by initiator.
     *
     * For an initiator, that means that another initiator has connected to the path.
     *
     * For a responder, it means that an initiator requested to drop the responder.
     */
    public static final int DROPPED = 3004;
}

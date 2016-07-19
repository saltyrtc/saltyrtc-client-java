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

    /**
     * Explain the close code.
     */
    public static String explain(int code) {
        switch (code) {
            case CLOSING_NORMAL:
                return "Normal closing";
            case GOING_AWAY:
                return "The endpoint is going away";
            case SUBPROTOCOL_ERROR:
                return "No shared subprotocol could be found";
            case PATH_FULL:
                return "NO free responder byte";
            case PROTOCOL_ERROR:
                return "Protocol error";
            case INTERNAL_ERROR:
                return "Internal server error";
            case HANDOVER:
                return "Handover finished";
        }
        return "Unknown";
    }
}

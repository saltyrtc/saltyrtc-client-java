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
    public static final int NO_SHARED_SUBPROTOCOL = 1002;

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
     * Handover of the signaling channel.
     */
    public static final int HANDOVER = 3003;

    /**
     * Dropped by initiator.
     *
     * For an initiator, that means that another initiator has connected to the path.
     *
     * For a responder, it means that an initiator requested to drop the responder.
     */
    public static final int DROPPED_BY_INITIATOR = 3004;

    /**
     * Initiator could not decrypt a message.
     */
    public static final int INITIATOR_COULD_NOT_DECRYPT = 3005;

    /**
     * No shared task was found.
     */
    public static final int NO_SHARED_TASK = 3006;

	/**
	 * Valid close codes for drop-responder messages.
     */
    public static final int[] CLOSE_CODES_DROP_RESPONDER = new int[] {
        PROTOCOL_ERROR, INTERNAL_ERROR, DROPPED_BY_INITIATOR,
        INITIATOR_COULD_NOT_DECRYPT, NO_SHARED_TASK
    };

    /**
     * aLL Valid close codes.
     */
    public static final int[] CLOSE_CODES_ALL = new int[] {
        GOING_AWAY, NO_SHARED_SUBPROTOCOL, PATH_FULL, PROTOCOL_ERROR, INTERNAL_ERROR,
        HANDOVER, DROPPED_BY_INITIATOR, INITIATOR_COULD_NOT_DECRYPT, NO_SHARED_TASK
    };

    /**
     * Explain the close code.
     */
    public static String explain(int code) {
        switch (code) {
            case CLOSING_NORMAL:
                return "Normal closing";
            case GOING_AWAY:
                return "The endpoint is going away";
            case NO_SHARED_SUBPROTOCOL:
                return "No shared subprotocol could be found";
            case PATH_FULL:
                return "NO free responder byte";
            case PROTOCOL_ERROR:
                return "Protocol error";
            case INTERNAL_ERROR:
                return "Internal server error";
            case HANDOVER:
                return "Handover finished";
            case DROPPED_BY_INITIATOR:
                return "Dropped by initiator";
            case INITIATOR_COULD_NOT_DECRYPT:
                return "Initiator could not decrypt a message";
            case NO_SHARED_TASK:
                return "No shared task was found";
        }
        return "Unknown";
    }
}

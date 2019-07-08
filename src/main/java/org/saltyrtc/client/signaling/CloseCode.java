/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.saltyrtc.client.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket close codes
 */

public enum CloseCode {

    /**
     * Normal closing of websocket.
     */
    CLOSING_NORMAL(1000, "Normal closing"),

    /**
     * The endpoint is going away.
     */
    GOING_AWAY(1001, "The endpoint is going away"),

    /**
     * No shared sub-protocol could be found.
     */
    NO_SHARED_SUBPROTOCOL(1002, "No shared subprotocol could be found"),

    /**
     * No free responder byte.
     */
    PATH_FULL(3000, "No free responder byte"),

    /**
     * Invalid message, invalid path length, ...
     */
    PROTOCOL_ERROR(3001, "Protocol error"),

    /**
     * Syntax error, ...
     */
    INTERNAL_ERROR(3002, "Internal error"),

    /**
     * Handover of the signaling channel.
     */
    HANDOVER(3003, "Handover finished"),

    /**
     * Dropped by initiator.
     *
     * For an initiator, that means that another initiator has connected to the path.
     *
     * For a responder, it means that an initiator requested to drop the responder.
     */
    DROPPED_BY_INITIATOR(3004, "Dropped by initiator"),

    /**
     * Initiator could not decrypt a message.
     */
    INITIATOR_COULD_NOT_DECRYPT(3005, "Initiator could not decrypt a message"),

    /**
     * No shared task was found.
     */
    NO_SHARED_TASK(3006, "No shared task was found"),

    /**
     * Invalid key.
     */
    INVALID_KEY(3007, "Invalid key"),

    /**
     * Timeout.
     */
    TIMEOUT(3008, "Timeout");

    /**
     * Valid close codes for drop-responder messages.
     */
    public static final EnumSet<CloseCode> CLOSE_CODES_DROP_RESPONDER = EnumSet.of(
      PROTOCOL_ERROR, INTERNAL_ERROR, DROPPED_BY_INITIATOR, INITIATOR_COULD_NOT_DECRYPT
    );

    private static final Map<Integer, CloseCode> lookup = new HashMap<>();

    static {
        for (CloseCode closeCode : CloseCode.values()) {
            lookup.put(closeCode.code, closeCode);
        }
    }

    public final int code;
    public final String explanation;

    CloseCode(final int code, final String explanation) {
        this.code = code;
        this.explanation = explanation;
    }

    @Nullable
    public static CloseCode getByCode(final int code) {
        return lookup.get(code);
    }
}

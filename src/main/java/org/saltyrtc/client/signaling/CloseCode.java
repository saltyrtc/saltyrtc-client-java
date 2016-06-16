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
     * The endpoint is going away.
     */
    public static int GoingAway = 1001;

    /**
     * No shared sub-protocol could be found.
     */
    public static int SubprotocolError = 1002;

    /**
     * No free responder byte.
     */
    public static int PathFull = 3000;

    /**
     * Invalid message, invalid path length, ...
     */
    public static int ProtocolError = 3001;

    /**
     * Syntax error, ...
     */
    public static int InternalError = 3002;

    /**
     * Handover to data channel.
     */
    public static int Handover = 3003;

    /**
     * Dropped by initiator.
     *
     * For an initiator, that means that another initiator has connected to the path.
     *
     * For a responder, it means that an initiator requested to drop the responder.
     */
    public static int Dropped = 3004;
}

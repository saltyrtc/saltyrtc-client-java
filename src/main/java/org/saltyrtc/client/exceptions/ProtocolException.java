/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.exceptions;

import org.saltyrtc.client.signaling.CloseCode;

/**
 * A SaltyRTC protocol error.
 *
 * This should always result in a connection reset.
 */
public class ProtocolException extends SignalingException {

    public ProtocolException(String message) {
        super(CloseCode.PROTOCOL_ERROR, message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(CloseCode.PROTOCOL_ERROR, message, cause);
    }

    public ProtocolException(Throwable cause) {
        super(CloseCode.PROTOCOL_ERROR, cause);
    }
}

/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.exceptions;

import org.saltyrtc.client.signaling.CloseCode;

/**
 * A SaltyRTC signaling error.
 *
 * It will result in the connection closing with the specified error code.
 */
public class SignalingException extends Exception {

    private final CloseCode closeCode;

    public SignalingException(CloseCode closeCode, String message) {
        super(message);
        this.closeCode = closeCode;
    }

    public SignalingException(CloseCode closeCode, String message, Throwable cause) {
        super(message, cause);
        this.closeCode = closeCode;
    }

    public SignalingException(CloseCode closeCode, Throwable cause) {
        super(cause);
        this.closeCode = closeCode;
    }

    public CloseCode getCloseCode() {
        return this.closeCode;
    }
}

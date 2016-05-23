/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.exceptions;

public class CryptoException extends Exception {
    protected final String state;
    protected final String error;

    public CryptoException(final String state, final String error) {
        this.state = state;
        this.error = error;
    }

    public String getState() {
        return state;
    }

    public String getError() {
        return error;
    }
}

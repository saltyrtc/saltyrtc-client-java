/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.exceptions;

/**
 * Thrown if an action (e.g. sending an application message) is requested in an invalid state.
 */
public class InvalidStateException extends Exception {
    public InvalidStateException() {
    }

    public InvalidStateException(String s) {
        super(s);
    }

    public InvalidStateException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InvalidStateException(Throwable throwable) {
        super(throwable);
    }
}

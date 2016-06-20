/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.exceptions;

public class ConnectionException extends Exception {
    public ConnectionException(String s) {
        super(s);
    }

    public ConnectionException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ConnectionException(Throwable throwable) {
        super(throwable);
    }
}
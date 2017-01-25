/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.exceptions;

public class OverflowException extends Exception {
    public OverflowException(String s) {
        super(s);
    }

    public OverflowException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public OverflowException(Throwable throwable) {
        super(throwable);
    }
}

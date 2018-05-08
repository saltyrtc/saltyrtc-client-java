/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.exceptions;

public class ValidationError extends Exception {
    /**
     * If this flag is set, then the validation error
     * will be converted to a protocol error.
     */
    public boolean critical = true;

    public ValidationError() {
    }

    public ValidationError(String s) {
        super(s);
    }

    public ValidationError(String s, boolean critical) {
        this(s);
        this.critical = critical;
    }

    public ValidationError(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ValidationError(Throwable throwable) {
        super(throwable);
    }
}

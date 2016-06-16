/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages;

public class ValidationHelper {

    public static void validateType(Object value, Class type, String expected) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Type field must be a string");
        }
        if (!expected.equals(type.cast(value))) {
            throw new IllegalArgumentException("Type must be '" + expected + "'");
        }
    }

}

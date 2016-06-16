/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.messages;

import org.saltyrtc.client.exceptions.ValidationError;

public class ValidationHelper {

    public static String validateType(Object value, String expected) throws ValidationError {
        if (!(value instanceof String)) {
            throw new ValidationError("Type must be a string");
        }
        final String type = (String) value;
        if (!expected.equals(type)) {
            throw new ValidationError("Type must be '" + expected + "'");
        }
        return type;
    }

    public static byte[] validateByteArray(Object value, int expectedLength, String name) throws ValidationError {
        if (!(value instanceof byte[])) {
            throw new ValidationError(name + " must be a byte array");
        }
        final byte[] key = (byte[]) value;
        if (key.length != expectedLength) {
            throw new ValidationError(
                    name + " must be " + expectedLength + " bytes long, not " + key.length);
        }
        return key;
    }

    public static Boolean validateBoolean(Object value, String name) throws ValidationError {
        if (!(value instanceof Boolean)) {
            throw new ValidationError(name + " must be a boolean");
        }
        return (Boolean) value;
    }
}

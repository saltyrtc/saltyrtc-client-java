/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

import org.saltyrtc.client.exceptions.ValidationError;

import java.util.List;

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

    /**
     * This is suitable for validating messagepack bin format family.
     *
     * @param value The deserialized object.
     * @param expectedLength Expected length for this fixed-length binary data.
     * @param name Name of field, used in error messages.
     * @return Validated data
     * @throws ValidationError if validation fails
     */
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

    /**
     * This is suitable for validating MessagePack array format family.
     *
     * Currently the function only supports integer arrays, but could be made generic in the future.
     *
     * Note that array types in MessagePack don't have a fixed type,
     * so an array is always deserialized as Object[].
     */
    @SuppressWarnings("unchecked")
    public static List<Integer> validateIntegerList(Object values, Class type, String name) throws ValidationError {
        if (!(values instanceof List)) {
            throw new ValidationError(name + " must be a list");
        }
        for (Object element : (List) values) {
            if (!type.isInstance(element)) {
                throw new ValidationError(name + " must be a " + type.getSimpleName() + " list");
            }
        }
        return (List<Integer>) values;
    }

    public static Boolean validateBoolean(Object value, String name) throws ValidationError {
        if (!(value instanceof Boolean)) {
            throw new ValidationError(name + " must be a boolean");
        }
        return (Boolean) value;
    }

    public static Integer validateInteger(Object value, int min, int max, String name) throws ValidationError {
        if (!(value instanceof Integer)) {
            throw new ValidationError(name + " must be an Integer");
        }
        final Integer number = (Integer) value;
        if (number < min) {
            throw new ValidationError(name + " must be > " + min);
        }
        if (number > max) {
            throw new ValidationError(name + " must be < " + max);
        }
        return number;
    }
}

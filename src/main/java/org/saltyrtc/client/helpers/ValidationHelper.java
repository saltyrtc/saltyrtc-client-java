/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.signaling.CloseCode;

import java.util.List;
import java.util.Map;

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
     * Note that array types in MessagePack don't have a fixed type,
     * so an array is always deserialized as Object[].
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> validateTypedList(Object values, Class type, String name) throws ValidationError {
        if (!(values instanceof List)) {
            throw new ValidationError(name + " must be a list");
        }
        for (Object element : (List) values) {
            if (!type.isInstance(element)) {
                throw new ValidationError(name + " must be a " + type.getSimpleName() + " list");
            }
        }
        return (List<T>) values;
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

    public static String validateString(Object value, String name) throws ValidationError {
        if (!(value instanceof String)) {
            throw new ValidationError(name + " must be a String, not " + value.getClass().getName());
        }
        return (String) value;
    }

    public static Integer validateCloseCode(Object value, boolean dropResponder, String name) throws ValidationError {
        if (!(value instanceof Integer)) {
            throw new ValidationError(name + " must be an Integer");
        }
        final Integer number = (Integer) value;
        final int[] codes = dropResponder ? CloseCode.CLOSE_CODES_DROP_RESPONDER : CloseCode.CLOSE_CODES_ALL;
        for (int code : codes) {
            if (code == number) {
                return number;
            }
        }
        throw new ValidationError(name + " must be a valid close code");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> validateStringObjectMap(Object value, String name) throws ValidationError {
        // Check main type
        if (!(value instanceof Map)) {
            throw new ValidationError(name + " must be a Map");
        }
        // Check key types
        for (Object element : ((Map<Object, Object>) value).keySet()) {
            if (!(element instanceof String)) {
                throw new ValidationError(name + " must be a Map with Strings as keys");
            }
        }
        // Cast
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Map<Object, Object>> validateStringMapMap(Object value, String name) throws ValidationError {
        Map<String, Object> map = validateStringObjectMap(value, name);
        // Check value types
        for (Object element : map.values()) {
            if (element != null && !(element instanceof Map)) {
                throw new ValidationError(name + " must be a Map with Maps or null as values");
            }
        }
        // Cast
        return (Map<String, Map<Object, Object>>) value;
    }
}

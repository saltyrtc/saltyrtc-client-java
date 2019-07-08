/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.helpers;

import org.junit.Test;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;
import org.saltyrtc.client.signaling.CloseCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class ValidationHelperTest {

    @Test
    public void testValidateType() throws ValidationError {
        final String validated = ValidationHelper.validateType("foo", "foo");
        assertEquals("foo", validated);
    }

    @Test
    public void testValidateTypeTypeFails() {
        try {
            ValidationHelper.validateType(3, "bar");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Type must be a string", e.getMessage());
        }
    }

    @Test
    public void testValidateTypeNull() {
        try {
            ValidationHelper.validateType(null, "bar");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Type must be a string", e.getMessage());
        }
    }

    @Test
    public void testValidateTypeValueFails() {
        try {
            ValidationHelper.validateType("foo", "bar");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Type must be 'bar'", e.getMessage());
        }
    }

    @Test
    public void testValidateByteArray() throws ValidationError {
        final byte[] validated = ValidationHelper.validateByteArray(new byte[] { 1, 2, 3 }, 3, "Array");
        assertArrayEquals(new byte[] { 1, 2, 3 }, validated);
    }

    @Test
    public void testValidateByteArrayTypeFails() {
        try {
            ValidationHelper.validateByteArray("yo", 2, "Key");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Key must be a byte array", e.getMessage());
        }
    }

    @Test
    public void testValidateByteArrayLengthFails() {
        try {
            ValidationHelper.validateByteArray(new byte[] { 1, 2, 3 }, 2, "Key");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Key must be 2 bytes long, not 3", e.getMessage());
        }
    }

    @Test
    public void testValidateBoolean() throws ValidationError {
        final boolean validated = ValidationHelper.validateBoolean(true, "Condition");
        assertTrue(validated);
    }

    @Test
    public void testValidateBooleanTypeFails() {
        try {
            ValidationHelper.validateBoolean("yo", "Condition");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Condition must be a boolean", e.getMessage());
        }
    }

    @Test
    public void testValidateIntegerList() throws ValidationError {
        // Create an object that is a list of integers
        final List<Object> values = new ArrayList<>();
        values.add(1); values.add(2); values.add(3);

        // Convert
        final List<Integer> validated = ValidationHelper.validateTypedList(values, Integer.class, "IntArray");

        // Verify
        final List<Integer> expected = new ArrayList<>();
        expected.add(1); expected.add(2); expected.add(3);
        assertArrayEquals(expected.toArray(), validated.toArray());
    }

    @Test
    public void testValidateIntegerListWithNull() throws ValidationError {
        // Create an object that is a list of integers
        final List<Object> values = new ArrayList<>();
        values.add(1); values.add(null); values.add(3);

        // Convert
        final List<Integer> validated = ValidationHelper.validateTypedList(values, Integer.class, "IntArray", true);

        // Verify
        final List<Integer> expected = new ArrayList<>();
        expected.add(1); expected.add(null); expected.add(3);
        assertArrayEquals(expected.toArray(), validated.toArray());
    }

    @Test
    public void testValidateStringList() throws ValidationError {
        // Create an object that is a list of integers
        final List<Object> values = new ArrayList<>();
        values.add("a"); values.add("b"); values.add("c");

        // Convert
        final List<String> validated = ValidationHelper.validateTypedList(values, String.class, "StringArray");

        // Verify
        final List<String> expected = new ArrayList<>();
        expected.add("a"); expected.add("b"); expected.add("c");
        assertArrayEquals(expected.toArray(), validated.toArray());
    }

    @Test
    public void testValidateIntegerListOuterTypeFails() {
        final Object value = "hello";
        try {
            ValidationHelper.validateTypedList(value, Integer.class, "IntArray");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("IntArray must be a list", e.getMessage());
        }
    }

    @Test
    public void testValidateIntegerListInnerTypeFails() {
        try {
            ValidationHelper.validateTypedList(asList('y', 'o'), Integer.class, "IntArray");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("IntArray must be a Integer list", e.getMessage());
        }
    }

    @Test
    public void testValidateIntegerListInnerTypeFailsNull() {
        try {
            ValidationHelper.validateTypedList(asList(1, 2, null, 3), Integer.class, "IntArray", false);
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("IntArray may not contain null values", e.getMessage());
        }
    }

    @Test
    public void testValidateStringListInnerTypeFails() {
        try {
            ValidationHelper.validateTypedList(asList('y', 'o'), String.class, "StringArray");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("StringArray must be a String list", e.getMessage());
        }
    }

    @Test
    public void testValidateInteger() throws ValidationError {
        Object value = 42;
        final Integer validated = ValidationHelper.validateInteger(value, 0, 100, "Number");
        assertEquals(Integer.valueOf(42), validated);
    }

    @Test
    public void testValidateIntegerTypeFails() {
        try {
            ValidationHelper.validateInteger("yo", 0, 100, "Number");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Number must be an Integer", e.getMessage());
        }
    }

    @Test
    public void testValidateIntegerRangeMinFails() {
        try {
            ValidationHelper.validateInteger(3, 5, 7, "Number");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Number must be > 5", e.getMessage());
        }
    }

    @Test
    public void testValidateIntegerRangeMaxFails() {
        try {
            ValidationHelper.validateInteger(8, 5, 7, "Number");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Number must be < 7", e.getMessage());
        }
    }

    @Test
    public void testValidateString() throws ValidationError {
        Object value = "hi";
        final String validated = ValidationHelper.validateString(value, "Text");
        assertEquals("hi", validated);
    }

    @Test
    public void testValidateStringTypeFails() {
        try {
            ValidationHelper.validateString(100, "Text");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Text must be a String, not java.lang.Integer", e.getMessage());
        }
    }

    @Test
    public void testValidateCloseCode() throws ValidationError {
        Object closeCode = 1002;
        final CloseCode validated = ValidationHelper.validateCloseCode(closeCode, false, "Number");
        assertEquals(CloseCode.NO_SHARED_SUBPROTOCOL, validated);
    }

    @Test
    public void testValidateCloseCodeDroppedResponder() throws ValidationError {
        Object closeCode = 3004;
        final CloseCode validated = ValidationHelper.validateCloseCode(closeCode, true, "Number");
        assertEquals(CloseCode.DROPPED_BY_INITIATOR, validated);
    }

    @Test
    public void testValidateCloseCodeFails() {
        try {
            ValidationHelper.validateCloseCode(2000, false, "Number");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Number must be a valid close code", e.getMessage());
        }
    }

    @Test
    public void testValidateCloseCodeDroppedResponderFails() {
        try {
            ValidationHelper.validateCloseCode(1002, true, "Number");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("Number must be a valid close code", e.getMessage());
        }
    }

    @Test
    public void testValidateStringMapMap() throws ValidationError {
        // Create an object that is a map of string -> object
        final Map<String, Map<Object, Object>> map = new HashMap<>();
        final Map<Object, Object> inner = new HashMap<>(); inner.put("foo", 1);
        map.put("a", inner); map.put("b", null); map.put("c", null);

        // Convert
        final Map<String, Map<Object, Object>> validated = ValidationHelper.validateStringMapMap(map, "Map");

        // Verify
        final Map<String, Map<Object, Object>> expected = new HashMap<>();
        expected.put("a", inner); expected.put("b", null); expected.put("c", null);
        assertArrayEquals(expected.keySet().toArray(), validated.keySet().toArray());
        assertArrayEquals(expected.values().toArray(), validated.values().toArray());
    }

    @Test
    public void testValidateStringMapMapOuterTypeFails() {
        try {
            ValidationHelper.validateStringMapMap(asList('y', 'o'), "IntArray");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("IntArray must be a Map", e.getMessage());
        }
    }

    @Test
    public void testValidateStringMapMapInnerTypeFails() {
        final Map<Integer, Object> map = new HashMap<>();
        map.put(1, 1); map.put(2, "foo"); map.put(3, 'c');
        try {
            ValidationHelper.validateStringMapMap(map, "IntegerObjectMap");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("IntegerObjectMap must be a Map with Strings as keys", e.getMessage());
        }
    }

    @Test
    public void testValidateStringMapMapInnerInnerTypeFails() {
        final Map<String, Object> map = new HashMap<>();
        map.put("a", 1); map.put("b", "foo"); map.put("c", 'c');
        try {
            ValidationHelper.validateStringMapMap(map, "IntegerObjectMap");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("IntegerObjectMap must be a Map with Maps or null as values", e.getMessage());
        }
    }

}

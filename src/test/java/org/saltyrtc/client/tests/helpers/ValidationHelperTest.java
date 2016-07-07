/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.helpers;

import org.junit.Test;
import org.saltyrtc.client.exceptions.ValidationError;
import org.saltyrtc.client.helpers.ValidationHelper;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        assertEquals(true, validated);
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
    public void testValidateArray() throws ValidationError {
        // Create an object that is a list of integers
        final List<Object> values = new ArrayList<>();
        values.add(1); values.add(2); values.add(3);
        final Object value = values;

        // Convert
        final List<Integer> validated = ValidationHelper.validateIntegerList(value, Integer.class, "IntArray");

        // Verify
        final List<Integer> expected = new ArrayList<>();
        expected.add(1); expected.add(2); expected.add(3);
        assertArrayEquals(expected.toArray(), validated.toArray());
    }

    @Test
    public void testValidateIntegerListOuterTypeFails() throws ValidationError {
        final Object value = "hello";
        try {
            ValidationHelper.validateIntegerList(value, Integer.class, "IntArray");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("IntArray must be a list", e.getMessage());
        }
    }

    @Test
    public void testValidateIntegerListInnerTypeFails() {
        try {
            ValidationHelper.validateIntegerList(asList('y', 'o'), Integer.class, "IntArray");
            fail("No ValidationError thrown");
        } catch (ValidationError e) {
            assertEquals("IntArray must be a Integer list", e.getMessage());
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
            assertEquals("Text must be a String", e.getMessage());
        }
    }

}

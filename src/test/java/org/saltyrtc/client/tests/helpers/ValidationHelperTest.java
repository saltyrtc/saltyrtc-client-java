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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
        } catch (ValidationError e) {
            assertEquals("Type must be a string", e.getMessage());
        }
    }

    @Test
    public void testValidateTypeNull() {
        try {
            ValidationHelper.validateType(null, "bar");
        } catch (ValidationError e) {
            assertEquals("Type must be a string", e.getMessage());
        }
    }

    @Test
    public void testValidateTypeValueFails() {
        try {
            ValidationHelper.validateType("foo", "bar");
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
        } catch (ValidationError e) {
            assertEquals("Key must be a byte array", e.getMessage());
        }
    }

    @Test
    public void testValidateByteArrayLengthFails() {
        try {
            ValidationHelper.validateByteArray(new byte[] { 1, 2, 3 }, 2, "Key");
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
        } catch (ValidationError e) {
            assertEquals("Condition must be a boolean", e.getMessage());
        }
    }

}

/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.helpers;

import org.junit.Test;
import org.saltyrtc.client.helpers.ArrayHelper;

import static org.junit.Assert.assertArrayEquals;

public class ArrayHelperTest {

    @Test
    public void testConcat() {
        byte[] a = new byte[] { 1, 2 };
        byte[] b = new byte[] { 3, 4 };
        byte[] c = ArrayHelper.concat(a, b);
        byte[] d = ArrayHelper.concat(b, a);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, c);
        assertArrayEquals(new byte[] { 3, 4, 1, 2 }, d);
    }
}

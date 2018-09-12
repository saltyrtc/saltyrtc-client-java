/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.helpers;

import org.junit.Test;
import org.saltyrtc.client.helpers.HexHelper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HexHelperTest {

    @Test
    public void testByteArrayToHex() {
        byte[] a = new byte[] { (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef };
        assertEquals(HexHelper.asHex(a), "deadbeef");
    }

    @Test
    public void testHexToByteArray() {
        byte[] a = new byte[] { (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef };
        assertArrayEquals(HexHelper.hexStringToByteArray("deadbeef"), a);
    }

}

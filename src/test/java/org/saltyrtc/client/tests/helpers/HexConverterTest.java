/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.helpers;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.saltyrtc.client.helpers.HexConverter;
import org.saltyrtc.client.helpers.RandomHelper;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class HexConverterTest {

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new byte[]{0x01, 0x02, 0x03}, "010203"},
                {new byte[]{(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef}, "deadbeef"},
                {new byte[]{0x00, 0x00}, "0000"}
        });
    }

    private byte[] bytes;
    private String hexstring;

    public HexConverterTest(byte[] bytes, String hexstring) {
        this.bytes = bytes;
        this.hexstring = hexstring;
    }

    @Test
    public void testHex() {
        Assert.assertEquals(this.hexstring, HexConverter.bytesToHex(this.bytes));
    }

}

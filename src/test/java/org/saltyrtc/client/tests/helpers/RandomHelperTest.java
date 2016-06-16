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
import org.saltyrtc.client.helpers.RandomHelper;

public class RandomHelperTest {

    @Test
    public void testPseudoRandom() {
        final byte[] r1 = RandomHelper.pseudoRandomBytes(10);
        final byte[] r2 = RandomHelper.pseudoRandomBytes(10);
        final byte[] r3 = RandomHelper.pseudoRandomBytes(10);
        Assert.assertThat(r1, IsNot.not(IsEqual.equalTo(r2)));
        Assert.assertThat(r1, IsNot.not(IsEqual.equalTo(r3)));
        Assert.assertThat(r2, IsNot.not(IsEqual.equalTo(r3)));
    }

    @Test
    public void testSecureRandom() {
        final byte[] r1 = RandomHelper.secureRandomBytes(10);
        final byte[] r2 = RandomHelper.secureRandomBytes(10);
        final byte[] r3 = RandomHelper.secureRandomBytes(10);
        Assert.assertThat(r1, IsNot.not(IsEqual.equalTo(r2)));
        Assert.assertThat(r1, IsNot.not(IsEqual.equalTo(r3)));
        Assert.assertThat(r2, IsNot.not(IsEqual.equalTo(r3)));
    }
}

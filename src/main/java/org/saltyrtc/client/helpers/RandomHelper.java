/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

import java.security.SecureRandom;
import java.util.Random;

public class RandomHelper {

    /**
     * Return n random bytes using a PRNG.
     *
     * This function should *never* be used for cryptographic purposes.
     */
    public static byte[] pseudoRandomBytes(int count) {
        final byte[] bytes = new byte[count];
        new Random().nextBytes(bytes);
        return bytes;
    }

    /**
     * Return n random bytes using a CSRNG.
     */
    public static byte[] secureRandomBytes(int count) {
        final byte[] bytes = new byte[count];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return bytes;
    }
}

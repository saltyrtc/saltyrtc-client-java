/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.helpers;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public class RandomHelper {

    /**
     * Return n random bytes using a PRNG.
     */
    public static byte[] pseudoRandomBytes(int count) {
        final byte[] bytes = new byte[count];
        ThreadLocalRandom.current().nextBytes(bytes);
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

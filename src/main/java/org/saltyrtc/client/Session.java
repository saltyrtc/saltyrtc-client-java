/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.exceptions.SessionUnavailableException;
import org.slf4j.Logger;

public class Session {
    protected static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Session.class);
    protected static String id = null;

    protected synchronized static boolean equals(String otherId) {
        return id != null && id.equals(otherId);
    }

    protected synchronized static String get() throws SessionUnavailableException {
        if (id == null) {
            throw new SessionUnavailableException();
        }
        return id;
    }

    protected synchronized static void set(String id) {
        LOG.debug("New: " + id);
        Session.id = id;
    }

    // Ex protected
    public synchronized static void reset() {
        id = null;
        LOG.debug("Reset");
    }
}

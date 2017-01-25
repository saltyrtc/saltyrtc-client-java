/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling.state;

/**
 * Message states, as seen by the initiator.
 */
public enum ResponderHandshakeState {
    NEW,
    // <---handleToken--- (or trusted key)
    TOKEN_RECEIVED,
    // <---handleKey-----
    KEY_RECEIVED,
    // ----sendKey------>
    KEY_SENT,
    // <---handleAuth----
    AUTH_RECEIVED,
    // ----sendAuth----->
    AUTH_SENT,
}

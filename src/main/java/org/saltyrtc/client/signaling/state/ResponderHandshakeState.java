/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling.state;

/**
 * Message states, as seen by the initiator:
 *
 * <pre>
 * NEW
 *
 * <---handleToken---
 *
 * TOKEN_RECEIVED
 *
 * <---handleKey-----
 *
 * KEY_RECEIVED
 *
 * ----sendKey------>
 *
 * KEY_SENT
 *
 * <---handleAuth----
 *
 * AUTH_RECEIVED
 *
 * ----sendAuth----->
 *
 * AUTH_SENT
 * </pre>
 */
public enum ResponderHandshakeState {
    NEW,
    TOKEN_RECEIVED,
    KEY_RECEIVED,
    KEY_SENT,
    AUTH_RECEIVED,
    AUTH_SENT,
}

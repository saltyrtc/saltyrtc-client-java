/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling.state;

/**
 * Message states, as seen by the responder:
 *
 * NEW
 *
 * ----sendToken---->
 *
 * TOKEN_SENT
 *
 * ----sendKey------>
 *
 * KEY_SENT
 *
 * <---handleKey-----
 *
 * KEY_RECEIVED
 *
 * ----sendAuth----->
 *
 * AUTH_SENT
 *
 * <---handleAuth----
 *
 * AUTH_RECEIVED
 */
public enum InitiatorHandshakeState {
    NEW,
    TOKEN_SENT,
    KEY_SENT,
    KEY_RECEIVED,
    AUTH_SENT,
    AUTH_RECEIVED;
}

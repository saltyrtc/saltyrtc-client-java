/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.events;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.messages.s2c.SendError;

/**
 * This event is thrown when a send-error message arrives.
 */
public class SendErrorEvent implements Event {

    @NonNull
    private final SendError error;
    @NonNull
    private final Object originalMessage; // TODO: Use generics

    public SendErrorEvent(@NonNull SendError error, @NonNull Object originalMessage) {
        this.error = error;
        this.originalMessage = originalMessage;
    }

    /**
     * The actual send-error message.
     */
    @NonNull
    public SendError getError() {
        return error;
    }

    /**
     * If it was found in the message history, the data message instance that could not be sent.
     */
    @NonNull
    public Object getOriginalMessage() {
        return originalMessage;
    }
}

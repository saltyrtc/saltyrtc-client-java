/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.signaling;

import org.junit.Test;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.InitiatorSignaling;
import org.saltyrtc.client.signaling.ResponderSignaling;
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.client.tests.Config;
import org.saltyrtc.client.tests.DummyTask;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class SignalingTest {

    @Test
    public void testWsPath() throws Exception {
        // Create signaling instances for initiator and responder
        final InitiatorSignaling initiator = new InitiatorSignaling(
                null, Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, new KeyStore(), null,
                new Task[] { new DummyTask() });
        final ResponderSignaling responder = new ResponderSignaling(
                null, Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, new KeyStore(), null,
                initiator.getPublicPermanentKey(), initiator.getAuthToken(),
                new Task[] { new DummyTask() });

        // Verify WebSocket path
        Method initiatorMeth = InitiatorSignaling.class.getDeclaredMethod("getWebsocketPath");
        Method responderMeth = ResponderSignaling.class.getDeclaredMethod("getWebsocketPath");
        initiatorMeth.setAccessible(true);
        responderMeth.setAccessible(true);
        final String initiatorPath = (String) initiatorMeth.invoke(initiator);
        final String responderPath = (String) responderMeth.invoke(responder);
        assertEquals(64, initiatorPath.length());
        assertEquals(initiatorPath, responderPath);
    }

}

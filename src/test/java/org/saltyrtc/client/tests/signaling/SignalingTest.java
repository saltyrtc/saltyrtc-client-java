/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.signaling;

import org.junit.Test;
import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.SaltyRTCBuilder;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.InitiatorSignaling;
import org.saltyrtc.client.signaling.ResponderSignaling;
import org.saltyrtc.client.signaling.Signaling;
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.client.tests.Config;
import org.saltyrtc.client.tests.DummyTask;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class SignalingTest {

    @Test
    public void testWsPath() throws Exception {
        // Create signaling instances for initiator and responder
        final InitiatorSignaling initiator = new InitiatorSignaling(
                null, Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, null, null,
                new KeyStore(), null, null,
                new Task[] { new DummyTask() },
                0);
        final ResponderSignaling responder = new ResponderSignaling(
                null, Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, null, null, new KeyStore(),
                initiator.getPublicPermanentKey(), initiator.getAuthToken(), null, null,
                new Task[] { new DummyTask() },
                0);

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

    /**
     * By default, WebSocket connect timeout should be set to 10 seconds.
     */
    @Test
    public void testSaltyRTCBuilderWithoutTimeout() throws Exception {
        final SaltyRTC salty = new SaltyRTCBuilder()
            .withKeyStore(new KeyStore())
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, null)
            .usingTasks(new Task[] { new DummyTask() })
            .asInitiator();

        // Make signaling field accessible
        Field fSignaling = salty.getClass().getDeclaredField("signaling");
        fSignaling.setAccessible(true);
        Signaling sig = (Signaling) fSignaling.get(salty);

        // Make wsConnectTimeout field accessible
        Field fTimeout = sig.getClass().getSuperclass().getDeclaredField("wsConnectTimeout");
        fTimeout.setAccessible(true);
        int timeout = (int) fTimeout.get(sig);
        assertEquals(timeout, 10000);
    }

    /**
     * WebSocket connect timeout should be configurable.
     */
    @Test
    public void testSaltyRTCBuilderWithTimeout() throws Exception {
        final SaltyRTC salty = new SaltyRTCBuilder()
            .withKeyStore(new KeyStore())
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, null)
            .withWebsocketConnectTimeout(1234)
            .usingTasks(new Task[] { new DummyTask() })
            .asInitiator();

        // Make signaling field accessible
        Field fSignaling = salty.getClass().getDeclaredField("signaling");
        fSignaling.setAccessible(true);
        Signaling sig = (Signaling) fSignaling.get(salty);

        // Make wsConnectTimeout field accessible
        Field fTimeout = sig.getClass().getSuperclass().getDeclaredField("wsConnectTimeout");
        fTimeout.setAccessible(true);
        int timeout = (int) fTimeout.get(sig);
        assertEquals(timeout, 1234);
    }

}

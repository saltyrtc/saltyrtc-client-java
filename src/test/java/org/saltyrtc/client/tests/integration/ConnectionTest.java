/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.integration;

import org.junit.Before;
import org.junit.Test;
import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.events.ConnectedEvent;
import org.saltyrtc.client.events.ConnectionClosedEvent;
import org.saltyrtc.client.events.ConnectionErrorEvent;
import org.saltyrtc.client.events.EventHandler;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tests.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.net.ssl.SSLContext;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionTest {

    static {
        if (Config.VERBOSE) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }
    }

    private SaltyRTC initiator;
    private SaltyRTC responder;
    private ExecutorService threadpool;
    private Map<String, Boolean> eventsCalled;

    @Before
    public void setUp() throws Exception {
        // Get SSL context
        final SSLContext sslContext = SSLContextHelper.getSSLContext();

        // Create SaltyRTC instances for initiator and responder
        initiator = new SaltyRTC(
            new KeyStore(), Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext);
        responder = new SaltyRTC(
            new KeyStore(), Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext,
            initiator.getPublicPermanentKey(), initiator.getAuthToken());

        // Enable debug mode
        if (Config.VERBOSE) {
            initiator.setDebug(true);
            responder.setDebug(true);
        }

        // Create a new executor pool
        threadpool = Executors.newFixedThreadPool(4);

        // Initiate event registry
        eventsCalled = new HashMap<>();
        final String[] events = new String[] { "Connected", "Error", "Closed" };
        for (String event : events) {
            eventsCalled.put("initiator" + event, false);
            eventsCalled.put("responder" + event, false);
        }

        // Register event handlers
        initiator.events.connected.register(new EventHandler<ConnectedEvent>() {
            @Override
            public boolean handle(ConnectedEvent event) {
                eventsCalled.put("initiatorConnected", true);
                return false;
            }
        });
        initiator.events.connectionError.register(new EventHandler<ConnectionErrorEvent>() {
            @Override
            public boolean handle(ConnectionErrorEvent event) {
                eventsCalled.put("initiatorError", true);
                return false;
            }
        });
        initiator.events.connectionClosed.register(new EventHandler<ConnectionClosedEvent>() {
            @Override
            public boolean handle(ConnectionClosedEvent event) {
                eventsCalled.put("initiatorClosed", true);
                return false;
            }
        });
        responder.events.connected.register(new EventHandler<ConnectedEvent>() {
            @Override
            public boolean handle(ConnectedEvent event) {
                eventsCalled.put("responderConnected", true);
                return false;
            }
        });
        responder.events.connectionError.register(new EventHandler<ConnectionErrorEvent>() {
            @Override
            public boolean handle(ConnectionErrorEvent event) {
                eventsCalled.put("responderError", true);
                return false;
            }
        });
        responder.events.connectionClosed.register(new EventHandler<ConnectionClosedEvent>() {
            @Override
            public boolean handle(ConnectionClosedEvent event) {
                eventsCalled.put("responderClosed", true);
                return false;
            }
        });
    }

    @Test
    public void testWsConnect() throws Exception {
        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, initiator.getSignalingState());
        assertEquals(SignalingState.NEW, responder.getSignalingState());

        // Connect server
        FutureTask<Void> initiatorConnect = initiator.connect();
        FutureTask<Void> responderConnect = responder.connect();
        System.out.println("Executing future...");
        threadpool.execute(initiatorConnect);
        threadpool.execute(responderConnect);

        // Wait until both are connected
        initiatorConnect.get();
        System.out.println("Initiator connected");
        assertEquals(SignalingState.SERVER_HANDSHAKE, initiator.getSignalingState());
        responderConnect.get();
        System.out.println("Responder connected");
        assertEquals(SignalingState.SERVER_HANDSHAKE, responder.getSignalingState());

        // Test if event handlers were called.
        // As this is only the first stage of the connection process,
        // no ConnectionEvents should be sent out yet.
        assertFalse(eventsCalled.get("initiatorConnected"));
        assertFalse(eventsCalled.get("initiatorError"));
        assertFalse(eventsCalled.get("initiatorClosed"));
        assertFalse(eventsCalled.get("responderConnected"));
        assertFalse(eventsCalled.get("responderError"));
        assertFalse(eventsCalled.get("responderClosed"));

        // Disconnect
        initiator.disconnect();
        responder.disconnect();

        // Await close events
        Thread.sleep(200);
        assertTrue(eventsCalled.get("initiatorClosed"));
        assertTrue(eventsCalled.get("responderClosed"));
        assertFalse(eventsCalled.get("initiatorError"));
        assertFalse(eventsCalled.get("responderError"));
    }

}

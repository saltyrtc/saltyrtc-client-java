/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.SaltyRTCBuilder;
import org.saltyrtc.client.events.EventHandler;
import org.saltyrtc.client.events.SignalingStateChangedEvent;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.client.tests.Config;
import org.saltyrtc.client.tests.DummyTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        if (Config.DEBUG) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }
    }

    private SaltyRTC initiator;
    private SaltyRTC responder;
    private Map<String, Boolean> eventsCalled;

    @Before
    public void setUp() throws Exception {
        // Get SSL context
        final SSLContext sslContext = SSLContextHelper.getSSLContext();

        // Create SaltyRTC instances for initiator and responder
        initiator = new SaltyRTCBuilder()
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(new KeyStore())
                .usingTasks(new Task[]{ new DummyTask() })
                .asInitiator();
        responder = new SaltyRTCBuilder()
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(new KeyStore())
                .usingTasks(new Task[]{ new DummyTask() })
                .initiatorInfo(initiator.getPublicPermanentKey(), initiator.getAuthToken())
                .asResponder();

        // Enable verbose debug mode
        if (Config.VERBOSE) {
            initiator.setDebug(true);
            responder.setDebug(true);
        }

        // Initiate event registry
        eventsCalled = new HashMap<>();
        final String[] events = new String[] { "Connected", "Error", "Closed" };
        for (String event : events) {
            eventsCalled.put("initiator" + event, false);
            eventsCalled.put("responder" + event, false);
        }

        // Register event handlers
        initiator.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                switch (event.getState()) {
                    case OPEN:
                        eventsCalled.put("initiatorConnected", true);
                        break;
                    case ERROR:
                        eventsCalled.put("initiatorError", true);
                        break;
                    case CLOSED:
                        eventsCalled.put("initiatorClosed", true);
                        break;
                }
                return false;
            }
        });
        responder.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                switch (event.getState()) {
                    case OPEN:
                        eventsCalled.put("responderConnected", true);
                        break;
                    case ERROR:
                        eventsCalled.put("responderError", true);
                        break;
                    case CLOSED:
                        eventsCalled.put("responderClosed", true);
                        break;
                }
                return false;
            }
        });
    }

    @Test
    public void testHandshakeInitiatorFirst() throws Exception {
        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, initiator.getSignalingState());
        assertEquals(SignalingState.NEW, responder.getSignalingState());

        // Latches to test connection state
        final CountDownLatch connectedPeers = new CountDownLatch(2);

        // Register onConnect handler
        initiator.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });
        responder.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });

        // Connect server
        initiator.connect();
        Thread.sleep(1000);
        responder.connect();

        // Wait for full handshake
        final boolean bothConnected = connectedPeers.await(4, TimeUnit.SECONDS);
        assertTrue(bothConnected);
        assertFalse(eventsCalled.get("initiatorError"));
        assertFalse(eventsCalled.get("responderError"));

        // Signaling state should be OPEN
        assertEquals(SignalingState.OPEN, initiator.getSignalingState());
        assertEquals(SignalingState.OPEN, responder.getSignalingState());

        // Disconnect
        initiator.disconnect();
        responder.disconnect();

        // Await close events
        Thread.sleep(300);
        assertTrue(eventsCalled.get("initiatorClosed"));
        assertTrue(eventsCalled.get("responderClosed"));
        assertFalse(eventsCalled.get("initiatorError"));
        assertFalse(eventsCalled.get("responderError"));

        // Signaling state should be CLOSED
        assertEquals(SignalingState.CLOSED, initiator.getSignalingState());
        assertEquals(SignalingState.CLOSED, responder.getSignalingState());
    }

    @Test
    public void testHandshakeResponderFirst() throws Exception {
        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, initiator.getSignalingState());
        assertEquals(SignalingState.NEW, responder.getSignalingState());

        // Latches to test connection state
        final CountDownLatch connectedPeers = new CountDownLatch(2);

        // Register onConnect handler
        responder.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });
        initiator.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });

        // Connect server
        responder.connect();
        Thread.sleep(1000);
        initiator.connect();

        // Wait for full handshake
        final boolean bothConnected = connectedPeers.await(4, TimeUnit.SECONDS);
        assertTrue(bothConnected);
        assertFalse(eventsCalled.get("responderError"));
        assertFalse(eventsCalled.get("initiatorError"));

        // Signaling state should be OPEN
        assertEquals(SignalingState.OPEN, responder.getSignalingState());
        assertEquals(SignalingState.OPEN, initiator.getSignalingState());

        // Disconnect
        responder.disconnect();
        initiator.disconnect();

        // Await close events
        Thread.sleep(300);
        assertTrue(eventsCalled.get("responderClosed"));
        assertTrue(eventsCalled.get("initiatorClosed"));
        assertFalse(eventsCalled.get("responderError"));
        assertFalse(eventsCalled.get("initiatorError"));

        // Signaling state should be CLOSED
        assertEquals(SignalingState.CLOSED, responder.getSignalingState());
        assertEquals(SignalingState.CLOSED, initiator.getSignalingState());
    }

    @Test
    public void testConnectionSpeed() throws Exception {
        // Max 1s for handshake
        final int MAX_DURATION = 1000;

        // Latches to test connection state
        final CountDownLatch connectedPeers = new CountDownLatch(2);
        initiator.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });
        responder.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });

        // Connect server
        final long startTime = System.nanoTime();
        initiator.connect();
        responder.connect();

        // Wait for full handshake
        final boolean bothConnected = connectedPeers.await(2 * MAX_DURATION, TimeUnit.MILLISECONDS);
        final long endTime = System.nanoTime();
        assertTrue(bothConnected);
        assertFalse(eventsCalled.get("responderError"));
        assertFalse(eventsCalled.get("initiatorError"));
        long durationMs = (endTime - startTime) / 1000 / 1000;
        System.out.println("Full handshake took " + durationMs + " milliseconds");

        // Disconnect
        responder.disconnect();
        initiator.disconnect();

        assertTrue("Duration time (" + durationMs + "ms) should be less than " + MAX_DURATION + "ms",
                   durationMs < MAX_DURATION);
    }

    @Test
    public void testTrustedHandshakeInitiatorFirst() throws Exception {
        // Create trusting peers
        final SSLContext sslContext = SSLContextHelper.getSSLContext();
        final SaltyRTC trustingInitiator = new SaltyRTCBuilder()
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(this.initiator.getKeyStore())
                .withTrustedPeerKey(this.responder.getPublicPermanentKey())
                .usingTasks(new Task[]{ new DummyTask() })
                .asInitiator();
        final SaltyRTC trustingResponder = new SaltyRTCBuilder()
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(this.responder.getKeyStore())
                .withTrustedPeerKey(this.initiator.getPublicPermanentKey())
                .usingTasks(new Task[]{ new DummyTask() })
                .asResponder();

        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.NEW, trustingResponder.getSignalingState());

        // Latches to test connection state
        final CountDownLatch connectedPeers = new CountDownLatch(2);
        trustingInitiator.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });
        trustingResponder.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });

        // Connect server
        trustingInitiator.connect();
        Thread.sleep(1000);
        trustingResponder.connect();

        // Wait for full handshake
        final boolean bothConnected = connectedPeers.await(4, TimeUnit.SECONDS);
        assertTrue(bothConnected);

        // Signaling state should be OPEN
        assertEquals(SignalingState.OPEN, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.OPEN, trustingResponder.getSignalingState());

        // Disconnect
        trustingInitiator.disconnect();
        trustingResponder.disconnect();

        // Await close events
        Thread.sleep(300);

        // Signaling state should be CLOSED
        assertEquals(SignalingState.CLOSED, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.CLOSED, trustingResponder.getSignalingState());
    }

    @Test
    public void testTrustedHandshakeResponderFirst() throws Exception {
        // Create trusting peers
        final SSLContext sslContext = SSLContextHelper.getSSLContext();
        final SaltyRTC trustingInitiator = new SaltyRTCBuilder()
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(this.initiator.getKeyStore())
                .withTrustedPeerKey(this.responder.getPublicPermanentKey())
                .usingTasks(new Task[]{ new DummyTask() })
                .asInitiator();
        final SaltyRTC trustingResponder = new SaltyRTCBuilder()
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(this.responder.getKeyStore())
                .withTrustedPeerKey(this.initiator.getPublicPermanentKey())
                .usingTasks(new Task[]{ new DummyTask() })
                .asResponder();

        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.NEW, trustingResponder.getSignalingState());

        // Latches to test connection state
        final CountDownLatch connectedPeers = new CountDownLatch(2);

        // Register onConnect handler
        trustingInitiator.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });
        trustingResponder.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
            @Override
            public boolean handle(SignalingStateChangedEvent event) {
                if (event.getState() == SignalingState.OPEN) {
                    connectedPeers.countDown();
                }
                return false;
            }
        });

        // Connect server
        trustingResponder.connect();
        Thread.sleep(1000);
        trustingInitiator.connect();

        // Wait for full handshake
        final boolean bothConnected = connectedPeers.await(4, TimeUnit.SECONDS);
        assertTrue(bothConnected);

        // Signaling state should be OPEN
        assertEquals(SignalingState.OPEN, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.OPEN, trustingResponder.getSignalingState());

        // Disconnect
        trustingInitiator.disconnect();
        trustingResponder.disconnect();

        // Await close events
        Thread.sleep(300);

        // Signaling state should be CLOSED
        assertEquals(SignalingState.CLOSED, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.CLOSED, trustingResponder.getSignalingState());
    }

    @After
    public void tearDown() {
        initiator.disconnect();
        responder.disconnect();
    }

}

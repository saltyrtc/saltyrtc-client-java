/*
 * Copyright (c) 2016-2018 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.tests.integration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.SaltyRTCBuilder;
import org.saltyrtc.client.SaltyRTCServerInfo;
import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.tests.LazysodiumCryptoProvider;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.exceptions.InvalidStateException;
import org.saltyrtc.client.helpers.HexHelper;
import org.saltyrtc.client.helpers.RandomHelper;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.CloseCode;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.client.tests.Config;
import org.saltyrtc.client.tests.DummyTask;
import org.saltyrtc.client.tests.PingPongTask;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

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
    private CryptoProvider cryptoProvider;

    static void await(@NonNull final CountDownLatch latch) throws InterruptedException {
        await(latch, 5);
    }

    static void await(@NonNull final CountDownLatch latch, long timeoutSeconds) throws InterruptedException {
        if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timed out after " + timeoutSeconds + " seconds");
        }
    }

    static void awaitState(@NonNull final SignalingState state, @NonNull final SaltyRTC... peers)
        throws ConnectionException, InterruptedException {
        assert peers.length > 0;

        // Latches to wait for a specific state
        final CountDownLatch done = new CountDownLatch(peers.length);

        // Register event on all peers
        for (@NonNull final SaltyRTC peer: peers) {
            // Check current state
            if (peer.getSignalingState() == state) {
                done.countDown();
                continue;
            }

            // Register state change event
            peer.events.signalingStateChanged.register(event -> {
                if (event.getState() == state) {
                    done.countDown();
                    return true;
                }
                return false;
            });
        }

        // Wait for the state to fire on all peers
        await(done);

    }

    static void connect(@NonNull final SignalingState state, @NonNull final SaltyRTC... peers)
        throws ConnectionException, InterruptedException {
        assert peers.length > 0;

        // Connect all peers
        for (@NonNull final SaltyRTC peer: peers) {
            peer.connect();
        }

        // Wait until connected
        awaitState(state, peers);
    }

    static void disconnect(@NonNull final SaltyRTC... peers) {
        assert peers.length > 0;

        // Disconnect all peers
        for (@NonNull final SaltyRTC peer: peers) {
            peer.disconnect();
        }
    }

    @Before
    public void setUp() throws Exception {
        this.cryptoProvider = new LazysodiumCryptoProvider();

        // Get SSL context
        final SSLContext sslContext = SSLContextHelper.getSSLContext();

        // Create SaltyRTC instances for initiator and responder
        initiator = new SaltyRTCBuilder(this.cryptoProvider)
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(new KeyStore(this.cryptoProvider))
                .usingTasks(new Task[]{ new DummyTask() })
                .asInitiator();
        responder = new SaltyRTCBuilder(this.cryptoProvider)
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(new KeyStore(this.cryptoProvider))
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
        initiator.events.signalingStateChanged.register(event -> {
            switch (event.getState()) {
                case TASK:
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
        });
        responder.events.signalingStateChanged.register(event -> {
            switch (event.getState()) {
                case TASK:
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
        });
    }

    @After
    public void tearDown() {
        disconnect(initiator, responder);
    }

    @Test
    public void testHandshakeInitiatorFirst() throws Exception {
        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, initiator.getSignalingState());
        assertEquals(SignalingState.NEW, responder.getSignalingState());

        // Connect initiator, then the responder
        connect(SignalingState.PEER_HANDSHAKE, initiator);
        connect(SignalingState.TASK, responder);
        awaitState(SignalingState.TASK, initiator);

        // Ensure no error has been fired
        assertFalse(eventsCalled.get("initiatorError"));
        assertFalse(eventsCalled.get("responderError"));

        // Signaling state should be TASK
        assertEquals(SignalingState.TASK, initiator.getSignalingState());
        assertEquals(SignalingState.TASK, responder.getSignalingState());

        // Disconnect
        disconnect(initiator, responder);

        // Await close events
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

        // Connect responder, then the initiator
        connect(SignalingState.PEER_HANDSHAKE, responder);
        connect(SignalingState.TASK, initiator);
        awaitState(SignalingState.TASK, responder);

        // Ensure no error has been fired
        assertFalse(eventsCalled.get("responderError"));
        assertFalse(eventsCalled.get("initiatorError"));

        // Signaling state should be TASK
        assertEquals(SignalingState.TASK, responder.getSignalingState());
        assertEquals(SignalingState.TASK, initiator.getSignalingState());

        // Disconnect
        disconnect(initiator, responder);

        // Await close events
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
        final int MAX_DURATION = 1000;
        final long startTime = System.nanoTime();

        // Wait for full handshake
        connect(SignalingState.TASK, initiator, responder);

        // Calculate duration
        final long endTime = System.nanoTime();
        assertFalse(eventsCalled.get("responderError"));
        assertFalse(eventsCalled.get("initiatorError"));
        long durationMs = (endTime - startTime) / 1000 / 1000;
        System.out.println("Full handshake took " + durationMs + " milliseconds");

        // Disconnect
        disconnect(initiator, responder);
        assertTrue("Duration time (" + durationMs + "ms) should be less than " + MAX_DURATION + "ms",
                   durationMs < MAX_DURATION);
    }

    @Test
    public void testDisconnectBeforePeerHandshake() throws Exception {
        // Connect and wait for peer handshake
        connect(SignalingState.PEER_HANDSHAKE, initiator);
        assertEquals(SignalingState.PEER_HANDSHAKE, initiator.getSignalingState());

        // Disconnect and wait until closed
        disconnect(initiator);
        assertEquals(SignalingState.CLOSED, initiator.getSignalingState());
    }

    @Test
    public void testDisconnectAfterPeerHandshake() throws Exception {
        final CountDownLatch peerDisconnected = new CountDownLatch(1);

        // Connect both and wait until established
        connect(SignalingState.TASK, initiator, responder);

        // Disconnect initiator and wait until closed
        disconnect(initiator);

        // Expect 'peer-disconnected' message
        responder.events.peerDisconnected.register(event -> {
            peerDisconnected.countDown();
            return true;
        });
        final boolean success = peerDisconnected.await(2, TimeUnit.SECONDS);
        assertTrue(success);
    }

    @Test
    public void testHandshakeRespondersFirstWithMultipleResponders() throws Exception {
        final SSLContext sslContext = SSLContextHelper.getSSLContext();

        // Create two responders
        final SaltyRTC responder1 = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ new DummyTask() })
            .initiatorInfo(initiator.getPublicPermanentKey(), initiator.getAuthToken())
            .asResponder();
        final SaltyRTC responder2 = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ new DummyTask() })
            .initiatorInfo(initiator.getPublicPermanentKey(), initiator.getAuthToken())
            .asResponder();

        // Ensure one of them goes into the 'task' state, the other is being dropped
        final CountDownLatch established = new CountDownLatch(1);
        final CountDownLatch dropped = new CountDownLatch(1);
        for (SaltyRTC responder: new SaltyRTC[]{responder1, responder2}) {
            responder.events.signalingStateChanged.register(event -> {
                if (event.getState() == SignalingState.TASK) {
                    established.countDown();
                    return true;
                }
                return false;
            });
            responder.events.close.register(event -> {
                if (dropped.getCount() != 0) {
                    assertEquals(CloseCode.DROPPED_BY_INITIATOR, event.getReason());
                    dropped.countDown();
                }
                return true;
            });
        }

        // Connect responders
        connect(SignalingState.PEER_HANDSHAKE, responder1, responder2);

        // Connect initiator
        assertEquals(1, established.getCount());
        assertEquals(1, dropped.getCount());
        connect(SignalingState.TASK, initiator);

        // Wait for dropped/established, then disconnect
        assertTrue(established.await(2, TimeUnit.SECONDS));
        assertTrue(dropped.await(2, TimeUnit.SECONDS));
        disconnect(initiator, responder1, responder2);
    }

    @Test
    public void testHandshakeInitiatorFirstWithMultipleResponders() throws Exception {
        final SSLContext sslContext = SSLContextHelper.getSSLContext();

        // Create two responders
        final SaltyRTC responder1 = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ new DummyTask() })
            .initiatorInfo(initiator.getPublicPermanentKey(), initiator.getAuthToken())
            .asResponder();
        final SaltyRTC responder2 = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ new DummyTask() })
            .initiatorInfo(initiator.getPublicPermanentKey(), initiator.getAuthToken())
            .asResponder();

        // Ensure one of them goes into the 'task' state, the other is being dropped
        final CountDownLatch firstResponderEstablished = new CountDownLatch(1);
        final CountDownLatch secondResponderDropped = new CountDownLatch(1);
        responder1.events.signalingStateChanged.register(event -> {
            if (event.getState() == SignalingState.TASK) {
                firstResponderEstablished.countDown();
                return true;
            }
            return false;
        });
        responder2.events.close.register(event -> {
            assertEquals(CloseCode.DROPPED_BY_INITIATOR, event.getReason());
            secondResponderDropped.countDown();
            return true;
        });

        // Connect initiator
        connect(SignalingState.PEER_HANDSHAKE, initiator);

        // Connect responders
        // Note: We wait until the task kicked in as that has been a source of
        //       errors in the past.
        assertEquals(1, firstResponderEstablished.getCount());
        assertEquals(1, secondResponderDropped.getCount());
        connect(SignalingState.TASK, responder1);
        connect(SignalingState.PEER_HANDSHAKE, responder2);

        // Wait for dropped/established, then disconnect
        assertTrue(firstResponderEstablished.await(2, TimeUnit.SECONDS));
        assertTrue(secondResponderDropped.await(2, TimeUnit.SECONDS));
        disconnect(initiator, responder1, responder2);
    }

    @Test
    public void testHandshakeResponderWithMultipleInitiators() throws Exception {
        final SSLContext sslContext = SSLContextHelper.getSSLContext();

        // Create two initiators
        final KeyStore keyStore = new KeyStore(this.cryptoProvider);
        final SaltyRTC initiator1 = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(keyStore)
            .usingTasks(new Task[]{ new DummyTask() })
            .asInitiator();
        final SaltyRTC initiator2 = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(keyStore)
            .usingTasks(new Task[]{ new DummyTask() })
            .asInitiator();

        // Create single responder
        final SaltyRTC responder = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ new DummyTask() })
            .initiatorInfo(initiator1.getPublicPermanentKey(), initiator1.getAuthToken())
            .asResponder();

        // Bind closed events
        final CountDownLatch responderClosedNormally = new CountDownLatch(1);
        final CountDownLatch firstInitiatorDropped = new CountDownLatch(1);
        final CountDownLatch secondInitiatorClosedNormally = new CountDownLatch(1);
        responder.events.close.register(event -> {
            assertEquals(CloseCode.CLOSING_NORMAL, event.getReason());
            responderClosedNormally.countDown();
            return true;
        });
        initiator1.events.close.register(event -> {
            assertEquals(CloseCode.DROPPED_BY_INITIATOR, event.getReason());
            firstInitiatorDropped.countDown();
            return true;
        });
        initiator2.events.close.register(event -> {
            assertEquals(CloseCode.CLOSING_NORMAL, event.getReason());
            secondInitiatorClosedNormally.countDown();
            return true;
        });

        // Connect responder
        connect(SignalingState.PEER_HANDSHAKE, responder);

        // Connect first initiator
        // Note: We wait until the task kicked in as that has been a source of
        //       errors in the past.
        assertEquals(1, responderClosedNormally.getCount());
        connect(SignalingState.TASK, initiator1);

        // Connect second initiator and disconnect after the responder
        // disconnected
        assertEquals(1, responderClosedNormally.getCount());
        assertEquals(1, firstInitiatorDropped.getCount());
        initiator2.events.peerDisconnected.register(event -> {
            initiator2.disconnect();
            return true;
        });
        initiator2.connect();

        // Ensure...
        //
        // - the responder closed normally,
        // - the first initiator has been dropped by the second initiator,
        //   and
        // - the second initiator closed normally.
        assertTrue(responderClosedNormally.await(2, TimeUnit.SECONDS));
        assertTrue(firstInitiatorDropped.await(2, TimeUnit.SECONDS));
        assertTrue(secondInitiatorClosedNormally.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testTrustedHandshakeInitiatorFirst() throws Exception {
        // Create trusting peers
        final SSLContext sslContext = SSLContextHelper.getSSLContext();
        final SaltyRTC trustingInitiator = new SaltyRTCBuilder(this.cryptoProvider)
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(this.initiator.getKeyStore())
                .withTrustedPeerKey(this.responder.getPublicPermanentKey())
                .usingTasks(new Task[]{ new DummyTask() })
                .asInitiator();
        final SaltyRTC trustingResponder = new SaltyRTCBuilder(this.cryptoProvider)
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(this.responder.getKeyStore())
                .withTrustedPeerKey(this.initiator.getPublicPermanentKey())
                .usingTasks(new Task[]{ new DummyTask() })
                .asResponder();

        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.NEW, trustingResponder.getSignalingState());

        // Connect initiator, then the responder
        connect(SignalingState.PEER_HANDSHAKE, trustingInitiator);
        connect(SignalingState.TASK, trustingResponder);
        awaitState(SignalingState.TASK, trustingInitiator);

        // Signaling state should be TASK
        assertEquals(SignalingState.TASK, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.TASK, trustingResponder.getSignalingState());

        // Disconnect
        disconnect(trustingInitiator, trustingResponder);

        // Signaling state should be CLOSED
        assertEquals(SignalingState.CLOSED, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.CLOSED, trustingResponder.getSignalingState());
    }

    @Test
    public void testTrustedHandshakeResponderFirst() throws Exception {
        // Create trusting peers
        final SSLContext sslContext = SSLContextHelper.getSSLContext();
        final SaltyRTC trustingInitiator = new SaltyRTCBuilder(this.cryptoProvider)
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(this.initiator.getKeyStore())
                .withTrustedPeerKey(this.responder.getPublicPermanentKey())
                .usingTasks(new Task[]{ new DummyTask() })
                .asInitiator();
        final SaltyRTC trustingResponder = new SaltyRTCBuilder(this.cryptoProvider)
                .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
                .withKeyStore(this.responder.getKeyStore())
                .withTrustedPeerKey(this.initiator.getPublicPermanentKey())
                .usingTasks(new Task[]{ new DummyTask() })
                .asResponder();

        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.NEW, trustingResponder.getSignalingState());

        // Connect responder, then the initiator
        connect(SignalingState.PEER_HANDSHAKE, trustingResponder);
        connect(SignalingState.TASK, trustingInitiator);
        awaitState(SignalingState.TASK, trustingResponder);

        // Signaling state should be TASK
        assertEquals(SignalingState.TASK, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.TASK, trustingResponder.getSignalingState());

        // Disconnect
        disconnect(trustingInitiator, trustingResponder);

        // Signaling state should be CLOSED
        assertEquals(SignalingState.CLOSED, trustingInitiator.getSignalingState());
        assertEquals(SignalingState.CLOSED, trustingResponder.getSignalingState());
    }

    @Test
    public void testTaskRegistration() throws Exception {
        // Create tasks
        final PingPongTask initiatorTask = new PingPongTask();
        final PingPongTask responderTask = new PingPongTask();

        // Create peers
        final SSLContext sslContext = SSLContextHelper.getSSLContext();
        final SaltyRTC initiator = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ initiatorTask, new DummyTask() })
            .asInitiator();
        final SaltyRTC responder = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ new DummyTask(), responderTask })
            .initiatorInfo(initiator.getPublicPermanentKey(), initiator.getAuthToken())
            .asResponder();

        // Signaling state should still be NEW
        assertEquals(SignalingState.NEW, initiator.getSignalingState());
        assertEquals(SignalingState.NEW, responder.getSignalingState());

        // Connect responder, then the initiator
        connect(SignalingState.PEER_HANDSHAKE, responder);
        connect(SignalingState.TASK, initiator);
        awaitState(SignalingState.TASK, responder);

        // Signaling state should be TASK
        assertEquals(SignalingState.TASK, initiator.getSignalingState());
        assertEquals(SignalingState.TASK, responder.getSignalingState());

        // Chosen task should be PingPong task
        assertTrue(initiator.getTask() instanceof PingPongTask);
        assertTrue(responder.getTask() instanceof PingPongTask);

        // Wait for ping-pong-messages
        Thread.sleep(100);

        // Check whether ping-pong happened
        assertTrue(responderTask.sentPong);
        assertTrue(initiatorTask.receivedPong);
        assertFalse(responderTask.receivedPong);
        assertFalse(initiatorTask.sentPong);

        // Disconnect
        disconnect(initiator, responder);

        // Signaling state should be CLOSED
        assertEquals(SignalingState.CLOSED, initiator.getSignalingState());
        assertEquals(SignalingState.CLOSED, responder.getSignalingState());
    }

    @Test
    public void testServerAuthenticationInitiatorSuccess() throws Exception {
        final SaltyRTC initiator = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, SSLContextHelper.getSSLContext())
            .withKeyStore(this.initiator.getKeyStore())
            .withServerKey(HexHelper.hexStringToByteArray(Config.SALTYRTC_SERVER_PUBLIC_KEY))
            .usingTasks(new Task[]{ new DummyTask() })
            .asInitiator();

        // Connect and wait for peer handshake
        connect(SignalingState.PEER_HANDSHAKE, initiator);
    }

    @Test
    public void testServerAuthenticationInitiatorFails() throws Exception {
        final SaltyRTC initiator = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, SSLContextHelper.getSSLContext())
            .withKeyStore(this.initiator.getKeyStore())
            .withServerKey(RandomHelper.pseudoRandomBytes(32))
            .usingTasks(new Task[]{ new DummyTask() })
            .asInitiator();

        // Look for signaling changes
        final CountDownLatch errored = new CountDownLatch(1);
        initiator.events.signalingStateChanged.register(event -> {
            if (event.getState() == SignalingState.PEER_HANDSHAKE) {
                fail("Server handshake succeeded even though server key was wrong");
            }
            return false;
        });
        initiator.events.close.register(event -> {
            if (event.getReason() == CloseCode.PROTOCOL_ERROR) {
                errored.countDown();
            } else {
                fail("Signaling was closed without a protocol error");
            }
            return false;
        });

        // Connect and wait for protocol error
        initiator.connect();
        final boolean success = errored.await(2, TimeUnit.SECONDS);
        assertTrue(success);
    }

    @Test
    public void testServerAuthenticationResponderSuccess() throws Exception {
        final SaltyRTC responder = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, SSLContextHelper.getSSLContext())
            .withKeyStore(this.responder.getKeyStore())
            .withServerKey(Config.SALTYRTC_SERVER_PUBLIC_KEY)
            .initiatorInfo(RandomHelper.pseudoRandomBytes(32), RandomHelper.pseudoRandomBytes(32))
            .usingTasks(new Task[]{ new DummyTask() })
            .asResponder();

        // Connect and wait for peer handshake
        connect(SignalingState.PEER_HANDSHAKE, responder);
    }

    @Test
    public void testServerAuthenticationResponderFails() throws Exception {
        final SaltyRTC responder = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, SSLContextHelper.getSSLContext())
            .withKeyStore(this.responder.getKeyStore())
            .withServerKey(RandomHelper.pseudoRandomBytes(32))
            .initiatorInfo(RandomHelper.pseudoRandomBytes(32), RandomHelper.pseudoRandomBytes(32))
            .usingTasks(new Task[]{ new DummyTask() })
            .asResponder();

        // Look for signaling changes
        final CountDownLatch errored = new CountDownLatch(1);
        responder.events.signalingStateChanged.register(event -> {
            if (event.getState() == SignalingState.PEER_HANDSHAKE) {
                fail("Server handshake succeeded even though server key was wrong");
            }
            return false;
        });
        responder.events.close.register(event -> {
            if (event.getReason() == CloseCode.PROTOCOL_ERROR) {
                errored.countDown();
            } else {
                fail("Signaling was closed without a protocol error");
            }
            return false;
        });

        // Connect and wait for protocol error
        responder.connect();
        final boolean success = errored.await(2, TimeUnit.SECONDS);
        assertTrue(success);
    }

    @Test
    public void testDynamicConnectionInfo() throws Exception {
        final SSLContext sslContext = SSLContextHelper.getSSLContext();
        final SaltyRTC responder = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(new SaltyRTCServerInfo() {
                @Override
                public String getHost(String initiatorPublicKey) {
                    return Config.SALTYRTC_HOST;
                }

                @Override
                public int getPort(String initiatorPublicKey) {
                    return Config.SALTYRTC_PORT;
                }

                @Override
                public SSLContext getSSLContext(String initiatorPublicKey) {
                    return sslContext;
                }
            })
            .withKeyStore(this.responder.getKeyStore())
            .withServerKey(Config.SALTYRTC_SERVER_PUBLIC_KEY)
            .initiatorInfo(RandomHelper.pseudoRandomBytes(32), RandomHelper.pseudoRandomBytes(32))
            .usingTasks(new Task[]{ new DummyTask() })
            .asResponder();

        // Connect and wait for peer handshake
        connect(SignalingState.PEER_HANDSHAKE, responder);
    }

    @Test
    public void testApplicationMessagePingPong() throws Exception {
        // Create peers
        final SSLContext sslContext = SSLContextHelper.getSSLContext();
        final SaltyRTC initiator = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ new DummyTask() })
            .asInitiator();
        final SaltyRTC responder = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
            .withKeyStore(new KeyStore(this.cryptoProvider))
            .usingTasks(new Task[]{ new DummyTask() })
            .initiatorInfo(initiator.getPublicPermanentKey(), initiator.getAuthToken())
            .asResponder();
        final CountDownLatch messagesReceived = new CountDownLatch(2);

        // Connect both
        connect(SignalingState.TASK, initiator, responder);

        // Add application message handlers
        initiator.events.applicationData.register(event -> {
            messagesReceived.countDown();
            return false;
        });
        responder.events.applicationData.register(event -> {
            Assert.assertEquals(event.getData(), "ping");
            try {
                responder.sendApplicationMessage("pong");
            } catch (ConnectionException | InvalidStateException e) {
                e.printStackTrace();
            }
            messagesReceived.countDown();
            return false;
        });

        // Send ping message
        initiator.sendApplicationMessage("ping");

        // Wait for ping-pong-messages
        final boolean bothReceived = messagesReceived.await(2, TimeUnit.SECONDS);
        assertTrue(bothReceived);

        // Disconnect
        disconnect(initiator, responder);
    }

    @Test
    public void testSSLSocketFactory() throws Exception {
        final SSLSocketFactory sslSocketFactory = SSLContextHelper.getSSLContext().getSocketFactory();

        final SaltyRTC initiator = new SaltyRTCBuilder(this.cryptoProvider)
            .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslSocketFactory)
            .withKeyStore(this.initiator.getKeyStore())
            .withServerKey(HexHelper.hexStringToByteArray(Config.SALTYRTC_SERVER_PUBLIC_KEY))
            .usingTasks(new Task[]{ new DummyTask() })
            .asInitiator();

        // Connect and wait for peer handshake
        connect(SignalingState.PEER_HANDSHAKE, initiator);
    }

}

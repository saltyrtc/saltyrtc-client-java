/*
 * Copyright (c) 2016-2017 Threema GmbH
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.exceptions.InvalidBuilderStateException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.helpers.HexHelper;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.signaling.SignalingRole;
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.vendor.com.neilalexander.jnacl.NaCl;

import javax.net.ssl.SSLContext;

/**
 * Builder class to construct a SaltyRTC instance.
 */
public class SaltyRTCBuilder {

    private boolean hasKeyStore = false;
    private boolean hasConnectionInfo = false;
    private boolean hasInitiatorInfo = false;
    private boolean hasTrustedPeerKey = false;
    private boolean hasTasks = false;

    private KeyStore keyStore;
    private String host;
    private Integer port;
    private Integer wsConnectTimeout;
    private SSLContext sslContext;
    private SaltyRTCServerInfo serverInfo;
    private byte[] initiatorPublicKey;
    private byte[] authToken;
    private byte[] peerTrustedKey;
    private byte[] serverKey;
    private Task[] tasks;
    private int pingInterval = 0;

    /**
     * Validate the specified host, throw an IllegalArgumentException if it's invalid.
     */
    private void validateHost(String host) {
        if (host.endsWith("/")) {
            throw new IllegalArgumentException("SaltyRTC host may not end with a slash");
        }
        if (host.contains("//")) {
            throw new IllegalArgumentException("SaltyRTC host should not contain protocol");
        }
    }

    /**
     * Assert that a keystore has been set.
     */
    private void requireKeyStore() throws InvalidBuilderStateException {
        if (!this.hasKeyStore) {
            throw new InvalidBuilderStateException(
                    "Keys not set yet. Please call .withKeyStore method first.");
        }
    }

    /**
     * Assert that connection info has been set.
     */
    private void requireConnectionInfo() throws InvalidBuilderStateException {
        if (!this.hasConnectionInfo) {
            throw new InvalidBuilderStateException(
                    "Connection info not set yet. Please call .connectTo method first.");
        }
    }

    /**
     * Assert that tasks have been set.
     */
    private void requireTasks() throws InvalidBuilderStateException {
        if (!this.hasTasks) {
            throw new InvalidBuilderStateException(
                "Tasks not set yet. Please call .usingTasks method first.");
        }
    }

    /**
     * Assert that initiator info has been set.
     */
    private void requireInitiatorInfo() throws InvalidBuilderStateException {
        if (!this.hasInitiatorInfo) {
            throw new InvalidBuilderStateException(
                    "Initiator info not set yet. Please call .initiatorInfo method first.");
        }
    }

    /**
     * Set SaltyRTC signalling server connection info.
     *
     * @param host The SaltyRTC server host.
     * @param port The SaltyRTC server port.
     * @param sslContext The SSL context used to create the encrypted WebSocket connection.
     * @throws IllegalArgumentException Thrown if the host string is invalid.
     */
    public SaltyRTCBuilder connectTo(String host, int port, SSLContext sslContext) {
        validateHost(host);
        this.host = host;
        this.port = port;
        this.sslContext = sslContext;
        this.hasConnectionInfo = true;
        return this;
    }

    /**
     * Provide a class that can determine SaltyRTC signalling server connection info.
     */
    public SaltyRTCBuilder connectTo(SaltyRTCServerInfo serverInfo) {
        this.serverInfo = serverInfo;
        this.hasConnectionInfo = true;
        return this;
    }

    /**
     * Set the key store. This can be either a new `KeyStore` instance, or a saved one if you
     * intend to use trusted keys.
     *
     * @param keyStore The KeyStore instance containing the public and private permanent key to
     * use.
     */
    public SaltyRTCBuilder withKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
        this.hasKeyStore = true;
        return this;
    }

    /**
     * Set the trusted public key of the peer.
     *
     * @param peerTrustedKey The trusted public key of the peer.
     */
    public SaltyRTCBuilder withTrustedPeerKey(byte[] peerTrustedKey) {
        this.peerTrustedKey = peerTrustedKey;
        this.hasTrustedPeerKey = true;
        return this;
    }

    /**
     * Set the trusted public key of the peer.
     *
     * @param peerTrustedKeyHex The trusted public key of the peer (hex encoded).
     */
    public SaltyRTCBuilder withTrustedPeerKey(String peerTrustedKeyHex) {
        return this.withTrustedPeerKey(HexHelper.hexStringToByteArray(peerTrustedKeyHex));
    }

    /**
     * Set the public permanent server key.
     *
     * When setting the server key to a known value, the server will be authenticated during the
     * handshake, so that MITM attacks can be prevented. It can be thought of as certificate
     * pinning.
     *
     * If you know the public permanent server key, it is strongly recommended to set this value!
     *
     * @param serverKey The public permanent key of the server.
     */
    public SaltyRTCBuilder withServerKey(byte[] serverKey) {
        this.serverKey = serverKey;
        return this;
    }

    /**
     * Set the public permanent server key.
     *
     * When setting the server key to a known value, the server will be authenticated during the
     * handshake, so that MITM attacks can be prevented. It can be thought of as certificate
     * pinning.
     *
     * If you know the public permanent server key, it is strongly recommended to set this value!
     *
     * @param serverKeyHex The public permanent key of the server (hex encoded).
     */
    public SaltyRTCBuilder withServerKey(String serverKeyHex) {
        return this.withServerKey(HexHelper.hexStringToByteArray(serverKeyHex));
    }

    /**
     * Request that the server sends a WebSocket ping every `intervalSeconds`.
     *
     * @param intervalSeconds A positive integer. Set it to 0 for no ping messages.
     */
    public SaltyRTCBuilder withPingInterval(int intervalSeconds) {
        if (intervalSeconds < 0) {
            throw new IllegalArgumentException("Ping interval may not be negative");
        }
        this.pingInterval = intervalSeconds;
        return this;
    }

    /**
     * Override the default WebSocket connect timeout.
     *
     * @param timeoutSeconds A positive integer.
     */
    public SaltyRTCBuilder withWebsocketConnectTimeout(int timeoutSeconds) {
        if (timeoutSeconds < 0) {
            throw new IllegalArgumentException("Websocket connect timeout may not be negative");
        }
        this.wsConnectTimeout = timeoutSeconds;
        return this;
    }

    /**
     * Set initiator connection info transferred via a secure data channel.
     *
     * @param initiatorPublicKey The public key of the initiator.
     * @param authToken The secret auth token.
     */
    public SaltyRTCBuilder initiatorInfo(byte[] initiatorPublicKey, byte[] authToken) {
        this.initiatorPublicKey = initiatorPublicKey;
        this.authToken = authToken;
        this.hasInitiatorInfo = true;
        return this;
    }

    /**
     * Set initiator connection info transferred via a secure data channel.
     *
     * @param initiatorPublicKeyHex The public key of the initiator (hex encoded).
     * @param authTokenHex The secret auth token (hex encoded).
     */
    public SaltyRTCBuilder initiatorInfo(String initiatorPublicKeyHex, String authTokenHex) {
        return this.initiatorInfo(
            HexHelper.hexStringToByteArray(initiatorPublicKeyHex),
            HexHelper.hexStringToByteArray(authTokenHex)
        );
    }

    /**
     * Set a list of tasks in order of descending preference.
     */
    public SaltyRTCBuilder usingTasks(Task[] tasks) {
        if (tasks.length < 1) {
            throw new IllegalArgumentException("You must specify at least 1 task");
        }
        this.tasks = tasks;
        this.hasTasks = true;
        return this;
    }

    /**
     * If a SaltyRTCServerInfo instance is provided, dynamically determine host and port.
     */
    private void processServerInfo(@NonNull SaltyRTCServerInfo serverInfo, byte[] publicKey) {
        final String hexPublicKey = NaCl.asHex(publicKey);
        this.host = serverInfo.getHost(hexPublicKey);
        this.port = serverInfo.getPort(hexPublicKey);
        this.sslContext = serverInfo.getSSLContext(hexPublicKey);
    }

    /**
     * Return a SaltyRTC instance configured as initiator.
     *
     * @throws InvalidBuilderStateException Thrown if key or connection info haven't been set yet.
     * @throws InvalidKeyException Thrown if a key is invalid.
     */
    public SaltyRTC asInitiator() throws InvalidBuilderStateException, InvalidKeyException {
        this.requireKeyStore();
        this.requireConnectionInfo();
        this.requireTasks();

        if (this.serverInfo != null) {
            this.processServerInfo(this.serverInfo, this.keyStore.getPublicKey());
        }

        if (this.hasTrustedPeerKey) {
            return new SaltyRTC(
                this.keyStore, this.host, this.port, this.sslContext, this.wsConnectTimeout,
                this.peerTrustedKey, this.serverKey,
                this.tasks, this.pingInterval, SignalingRole.Initiator);
        } else {
            return new SaltyRTC(
                this.keyStore, this.host, this.port, this.sslContext, this.wsConnectTimeout,
                this.serverKey, this.tasks, this.pingInterval);
        }
    }

    /**
     * Return a SaltyRTC instance configured as responder.
     *
     * @throws InvalidBuilderStateException Thrown if key or connection info or initiator info
     *     haven't been set yet.
     * @throws InvalidKeyException Thrown if public key or auth token are invalid.
     */
    public SaltyRTC asResponder() throws InvalidBuilderStateException, InvalidKeyException {
        this.requireKeyStore();
        this.requireConnectionInfo();
        this.requireTasks();

        if (this.hasTrustedPeerKey) {
            if (this.serverInfo != null) {
                this.processServerInfo(this.serverInfo, this.peerTrustedKey);
            }
            return new SaltyRTC(this.keyStore, this.host, this.port, this.sslContext, this.wsConnectTimeout,
                    this.peerTrustedKey, this.serverKey, this.tasks, this.pingInterval,
                    SignalingRole.Responder);
        } else {
            this.requireInitiatorInfo();
            if (this.serverInfo != null) {
                this.processServerInfo(this.serverInfo, this.initiatorPublicKey);
            }
            return new SaltyRTC(this.keyStore, this.host, this.port, this.sslContext, this.wsConnectTimeout,
                    this.initiatorPublicKey, this.authToken, this.serverKey,
                    this.tasks, this.pingInterval);
        }
    }
}

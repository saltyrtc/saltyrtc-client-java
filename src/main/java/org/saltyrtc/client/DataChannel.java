/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import org.saltyrtc.client.exceptions.CryptoException;
import org.saltyrtc.client.exceptions.InvalidChunkException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.webrtc.DataChannel.Buffer;
import org.webrtc.DataChannel.Observer;
import org.webrtc.DataChannel.State;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Handles the communication between browser and app.
 * Note: Public methods can be used safely from any thread.
 */
public class DataChannel extends EncryptedChannel {
    protected static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DataChannel.class);
    protected static final String LABEL = "saltyrtc";
    protected static final int HEARTBEAT_ACK_TIMEOUT = 10000;
    protected static final int MTU = 16384;

    protected State state;
    protected org.webrtc.DataChannel dc;
    protected ArrayList<JSONObject> cached;
    protected Events events;
    // Ex protected
    public final StateDispatcher stateDispatcher = new StateDispatcher();
    // Ex protected
    public final DataChannelMessageDispatcher messageDispatcher = new DataChannelMessageDispatcher();
    protected final HeartbeatAckTimer heartbeatAckTimer = new HeartbeatAckTimer();
    protected String heartbeat = null;

    public interface MessageListener {
        void onMessage(JSONObject message);
    }

    /**
     * Handles data channel events and dispatches messages.
     * TODO: So... where are the error events?
     */
    protected class Events implements Observer, Unchunkifier.Events {
        private volatile boolean stopped = false;
        private final Unchunkifier unchunkifier;

        public Events() {
            this.unchunkifier = new Unchunkifier(this);
        }

        public void stop() {
            this.stopped = true;
        }

        // Note: For some reason this method is called twice when the data channel closes.
        @Override
        public void onStateChange() {
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!stopped) {
                        setState(dc.state());
                    }
                }
            });
        }

        @Override
        public void onMessage(Buffer buffer) {
            if (this.stopped) {
                return;
            }
            if (!buffer.binary) {
                LOG.warn("Ignored ASCII message");
            } else {
                // Note: Buffer need to be received directly as it will be disposed after return
                try {
                    this.unchunkifier.add(buffer.data);
                } catch (InvalidChunkException e) {
                    stateDispatcher.error("chunk", "Invalid chunk received: " + e.moreChunks);
                }
            }
        }

        @Override
        public void onCompletedMessage(final ByteBuffer buffer) {
            // Now that the bytes have been fetched from the buffer, we can safely dispatch
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!stopped) {
                        receive(buffer);
                    }
                }
            });
        }
    }

    protected class HeartbeatAckTimer implements Runnable {
        @Override
        public void run() {
            LOG.error("Heartbeat ack timeout");
            stateDispatcher.error("timeout", "Heartbeat ack timeout");
            dc.close();
        }
    }

    public DataChannel() {
        this.reset(true);
    }

    // Ex protected
    public void reset() {
        this.reset(false);
    }

    // Ex protected
    public void reset(boolean hard) {
        // Set to unknown state
        this.setState(null);

        // Close and reset event instance
        if (this.events != null) {
            this.events.stop();
        }
        this.events = new Events();

        // Cancel and reset heartbeat ack timer
        this.cancelHeartbeatAckTimer();
        // Reset heartbeat content
        this.heartbeat = null;

        // Close data channel instance
        if (this.dc != null) {
            this.dc.close();
            this.dc.dispose();
            this.dc = null;
        }

        // Hard reset?
        if (!hard) {
            return;
        }

        // Cached messages
        this.cached = new ArrayList<>();
    }

    protected void setInstance(final org.webrtc.DataChannel dc) {
        this.dc = dc;
        // Register events on data channel instance
        dc.registerObserver(this.events);
        // Set the initial state of the data channel instance
        setState(dc.state());
    }

    protected void setState(final State state) {
        // Special case: Unknown state
        if (state == null) {
            // Ignore repeated state changes
            if (this.state == null) {
                LOG.debug("Ignoring repeated state: unknown");
                return;
            }

            // Update state and notify listeners
            this.state = null;
            this.stateDispatcher.state("unknown");
        } else {
            // Ignore repeated state changes
            if (state == this.state) {
                LOG.debug("Ignoring repeated state: " + state.toString().toLowerCase());
                return;
            }

            // Update state and notify listeners
            this.state = state;
            this.stateDispatcher.state(state.toString().toLowerCase());
        }
    }

    // Ex protected
    public boolean close() {
        if (this.dc != null) {
            this.dc.close();
            return true;
        } else {
            return false;
        }
    }

    protected void startHeartbeatAckTimer() {
        Handler.postDelayed(this.heartbeatAckTimer, HEARTBEAT_ACK_TIMEOUT);
    }

    protected void cancelHeartbeatAckTimer() {
        Handler.removeCallbacks(this.heartbeatAckTimer);
    }

    // Ex protected
    public void sendCached() {
        LOG.debug("Sending " + this.cached.size() + " delayed messages");
        for (JSONObject message : this.cached) {
            this.send(message);
        }
        this.cached.clear();
    }

    // Ex protected
    public void sendMessage(JSONObject inner) {
        // Build JSON
        JSONObject message = new JSONObject();
        try {
            // Prepare data
            message.put("type", "message");
            message.put("data", inner);
        } catch (JSONException e) {
            LOG.error("Message encode error: " + e.toString());
            e.printStackTrace();
            this.stateDispatcher.error("encode", e.toString());
            return;
        }

        // Send message
        this.send(message);
    }

    // Ex protected
    public void receiveMessage(JSONObject inner) {
        LOG.debug("Broadcasting message");
        this.messageDispatcher.message(inner);
    }

    protected void sendHeartbeat() {
        this.sendHeartbeat(Utils.getRandomString());
    }

    protected void sendHeartbeat(String content) {
        LOG.debug("Sending heartbeat");

        // Store heartbeat
        this.heartbeat = content;

        // Build JSON
        JSONObject message = new JSONObject();
        try {
            // Prepare data
            message.put("type", "heartbeat");
            message.put("data", content);
        } catch (JSONException e) {
            LOG.error("Heartbeat encode error: " + e.toString());
            e.printStackTrace();
            this.stateDispatcher.error("encode", e.toString());
            return;
        }

        // Start timer and send heartbeat
        this.startHeartbeatAckTimer();
        this.send(message);
    }

    protected void receiveHeartbeatAck(String content) {
        // Validate heartbeat ack
        if (this.heartbeat == null) {
            LOG.error("Ignored heartbeat-ack that has not been sent");
            return;
        }
        if (!content.equals(this.heartbeat)) {
            LOG.error("Heartbeat-ack does not match, expected: " + this.heartbeat +
                    "received: " + content);
            this.stateDispatcher.error("heartbeat", "Content did not match");
        } else {
            LOG.debug("Received heartbeat-ack");
            this.heartbeat = null;
            // Cancel heartbeat ack timer
            this.cancelHeartbeatAckTimer();
        }
    }

    protected void receiveHeartbeat(String content) {
        LOG.debug("Received heartbeat");
        this.sendHeartbeatAck(content);
    }

    protected void sendHeartbeatAck(String content) {
        LOG.debug("Sending heartbeat-ack");

        // Build JSON
        JSONObject message = new JSONObject();
        try {
            // Prepare data
            message.put("type", "heartbeat-ack");
            message.put("data", content);
        } catch (JSONException e) {
            this.stateDispatcher.error("encode", e.toString());
            LOG.error("Heartbeat ack encode error: " + e.toString());
            e.printStackTrace();
            return;
        }

        // Send heartbeat ack
        this.send(message);
    }

    protected void send(JSONObject message) {
        // Delay sending until connected
        if (this.dc != null && this.state == State.OPEN) {
            KeyStore.Box box;
            try {
                // Encrypt data
                box = this.encrypt(message.toString());
            } catch (CryptoException e) {
                this.stateDispatcher.error(e.getState(), e.getError());
                return;
            }

            // Send chunks
            String sizeKb = String.format("%.2f", ((float) box.getSize()) / 1024);
            LOG.debug("Sending message (size: " + sizeKb + " KB): " + message);
            Chunkifier chunkifier = new Chunkifier(box.getBuffer().array(), MTU);
            for (ByteBuffer chunk : chunkifier) {
                // Wrap buffer into data channel buffer and set the 'binary' flag
                Buffer buffer = new Buffer(chunk, true);

                // Send buffer content
                this.dc.send(buffer);
            }
        } else {
            LOG.debug("Delaying message until channel is open");
            this.cached.add(message);
        }
    }

    protected void receive(ByteBuffer buffer) {
        final String data;
        KeyStore.Box box = new KeyStore.Box(buffer);
        String sizeKb = String.format("%.2f", ((float) box.getSize()) / 1024);

        // Decrypt data
        try {
            data = this.decrypt(box);
        } catch (CryptoException e) {
            stateDispatcher.error(e.getState(), e.getError());
            return;
        }

        try {
            // Decode data
            LOG.debug("Received message (size: " + sizeKb + " KB): " + data);
            JSONObject message = new JSONObject(data);
            String type = message.getString("type");

            // Relay message
            //noinspection IfCanBeSwitch
            if (type.equals("message")) {
                JSONObject inner = message.getJSONObject("data");
                receiveMessage(inner);
            } else if (type.equals("heartbeat-ack")) {
                String content = message.getString("data");
                receiveHeartbeatAck(content);
            } else if (type.equals("heartbeat")) {
                String content = message.getString("data");
                receiveHeartbeat(content);
            } else {
                LOG.error("Ignored message: " + data);
            }
        } catch (JSONException e) {
            LOG.error("Ignored invalid message: " + data);
        }
    }
}

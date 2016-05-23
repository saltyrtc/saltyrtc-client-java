package org.saltyrtc.client;

import org.saltyrtc.client.exceptions.CryptoException;
import org.saltyrtc.client.exceptions.SessionUnavailableException;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import de.tavendo.autobahn.WebSocketOptions;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * The signaling channel used to exchange metadata of the peers.
 * Note: Public methods can be used safely from any thread.
 */
public class Signaling extends EncryptedChannel {
    protected static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Signaling.class);
    protected static final String DEFAULT_URL = "ws://127.0.0.1:8765/";
    protected static final int CONNECT_MAX_RETRIES = 10;
    protected static final int CONNECT_RETRY_INTERVAL = 10000;
    protected String path;
    protected String url;
    protected String state;
    protected final WebSocketOptions options;
    protected WebSocketConnection ws = null;
    protected int connectTries;
    protected ArrayList<CachedItem> cached;
    protected Events events;
    // Ex protected
    public final StateDispatcher stateDispatcher = new StateDispatcher();
    // Ex protected
    public final SignalingMessageDispatcher messageDispatcher = new SignalingMessageDispatcher();
    protected final ConnectTimer connectTimer = new ConnectTimer();

    public Signaling() {
        // Set timeout option
        this.options = new WebSocketOptions();
        this.options.setSocketConnectTimeout(CONNECT_RETRY_INTERVAL);

        // Store own public key for announcement
        this.reset(true);
    }

    /**
     * Listener for message (answer and candidate) dispatch request events.
     */
    public interface MessageListener {
        void onReset();
        void onSendError();
        void onOffer(SessionDescription description);
        void onCandidate(IceCandidate candidate);
    }

    protected class CachedItem {
        private final JSONObject message;
        private final boolean encrypt;

        public CachedItem(JSONObject message, boolean encrypt) {
            this.message = message;
            this.encrypt = encrypt;
        }
    }

    protected class ConnectTimer implements Runnable {
        @Override
        public void run() {
            connectTries += 1;
            LOG.error("Connect timeout, retry " +
                    connectTries + "/" + CONNECT_MAX_RETRIES);
            connect(path, url);
        }
    }

    /**
     * Handles signaling events and dispatches messages.
     * Note: It is vital that messages are always processed with the same amount of post calls
     * to the event loop. This will avoid message reordering while processing.
     */
    protected class Events extends WebSocketHandler {
        private volatile boolean stopped = false;

        public void stop() {
            this.stopped = true;
        }

        @Override
        public void onOpen() {
            // Web socket connection is ready for sending and receiving
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!stopped) {
                        setState("open");
                    }
                }
            });
        }

        @Override
        public void onClose(final int code, String reason) {
            LOG.debug("Connection closed with code: " + code + ", reason: " + reason);
            // Web Socket connection has been closed
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (stopped) {
                        return;
                    }
                    // Note: We don't need a timer like in the browser version here
                    if (code == CLOSE_CANNOT_CONNECT) {
                        reconnect(0);
                    } else {
                        setState("closed");
                    }
                }
            });
        }

        @Override
        public void onTextMessage(final String payload) {
            // A message has been received
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!stopped) {
                        receiveText(payload);
                    }
                }
            });
        }

        @Override
        public void onRawTextMessage(byte[] payload) {
            if (this.stopped) {
                return;
            }
            LOG.error("Ignored raw text message");
        }

        @Override
        public void onBinaryMessage(final byte[] payload) {
            if (this.stopped) {
                return;
            }

            // Note: Bytes need to be received directly as they might be disposed after return
            final String data;
            try {
                data = decrypt(new KeyStore.Box(ByteBuffer.wrap(payload)));
            } catch (CryptoException e) {
                stateDispatcher.error(e.getState(), e.getError());
                return;
            }

            // Now that the bytes have been fetched from the buffer, we can safely dispatch the
            // data (if it could be decrypted)
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (stopped) {
                        return;
                    }
                    receiveBinary(data);
                }
            });
        }
    }

    protected void setState(String state) {
        // Ignore repeated state changes
        if (state.equals(this.state)) {
            LOG.debug("Ignoring repeated state: " + state);
            return;
        }

        // Update state and notify listeners
        this.state = state;
        this.stateDispatcher.state(state);

        // Open?
        if (state.equals("open")) {
            // Reset connect counter
            this.connectTries = 0;
        }
    }

    public synchronized void reset() {
        this.reset(false);
    }

    // Ex protected
    public synchronized void reset(boolean hard) {
        this.setState("unknown");

        // Close and reset event instance
        if (this.events != null) {
            this.events.stop();
        }
        this.events = new Events();

        // Close web socket instance
        if (this.ws != null && this.ws.isConnected()) {
            LOG.debug("Disconnecting");
            this.ws.disconnect();
        }
        // Note: This is required because the web socket can't disconnect reliably, thus
        //       a new instance is required.
        this.ws = null;

        // Hard reset?
        if (!hard) {
            return;
        }

        // Reset connect counter
        this.connectTries = 0;

        // Clear cached messages
        this.clear();
    }

    // Ex protected
    public void clear() {
        this.cached = new ArrayList<>();
    }

    // Ex protected
    public void connect(String path) {
        this.connect(path, DEFAULT_URL);
    }

    protected void connect(String path, String url) { // TODO: Use WSS
        // Store path and URL
        this.path = path;
        this.url = url;

        // Give up?
        if (this.connectTries == CONNECT_MAX_RETRIES) {
            this.connectTries = 0;
            LOG.error("Connecting failed");
            this.setState("failed");
            return;
        }

        // Reset and create web socket instance
        this.reset();
        this.ws = new WebSocketConnection();
        LOG.debug("Created");
        this.setState("connecting");

        // Connect
        LOG.debug("Connecting to path: " + path);
        try {
            this.ws.connect(url + path, this.events, this.options);
        } catch (WebSocketException e) {
            LOG.error("Connect error: " + e.toString());
            this.stateDispatcher.error("connect", e.toString());
        }
    }

    public void reconnect() {
        this.reconnect(CONNECT_RETRY_INTERVAL);
    }

    public void reconnect(int delay) {
        this.restartConnectTimer(delay);
    }

    public void sendHello() {
        LOG.debug("Sending hello");

        // Build JSON
        String type = "hello-server";
        JSONObject message = new JSONObject();
        try {
            // Prepare data
            message.put("type", type);
            message.put("key", KeyStore.getPublicKey());
        } catch (JSONException e) {
            LOG.error("Hello encode error: " + e.toString());
            this.stateDispatcher.error("encode", e.toString());
            return;
        }

        // Send hello
        this.send(message, false);
    }

    // Ex protected
    public void sendReset() {
        LOG.debug("Sending reset");

        // Build JSON
        String type = "reset";
        JSONObject message = new JSONObject();
        try {
            // Prepare data
            message.put("type", type);
        } catch (JSONException e) {
            LOG.error("Reset encode error: " + e.toString());
            e.printStackTrace();
            this.stateDispatcher.error("encode", e.toString());
            return;
        }

        // Send reset
        this.send(message, false);
    }

    protected void receiveReset() {
        LOG.debug("Broadcasting reset");
        this.messageDispatcher.reset();
    }

    protected void receiveSendError() {
        LOG.debug("Broadcasting send error");
        this.messageDispatcher.sendError();
    }

    protected void receiveOffer(SessionDescription offer, String session) {
        LOG.debug("Broadcasting offer");
        this.messageDispatcher.offer(offer, session);
    }

    // Ex protected
    public void sendAnswer(SessionDescription description) {
        LOG.debug("Sending answer");

        // Build JSON
        String type = "answer";
        JSONObject message = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            // Prepare payload
            payload.put("type", type);
            payload.put("sdp", description.description);

            // Prepare data
            message.put("type", type);
            message.put("session", Session.get());
            message.put("data", payload);
        } catch (JSONException e) {
            LOG.error("Answer encode error: " + e.toString());
            e.printStackTrace();
            this.stateDispatcher.error("encode", e.toString());
            return;
        } catch (SessionUnavailableException e) {
            LOG.error("Session unavailable error: " + e.toString());
            e.printStackTrace();
            this.stateDispatcher.error("session", e.toString());
            return;
        }

        // Send answer
        this.send(message);
    }

    // Ex protected
    public void sendCandidate(IceCandidate candidate) {
        LOG.debug("Sending candidate");

        // Build JSON
        String type = "candidate";
        JSONObject message = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            // Prepare payload
            payload.put("type", type);
            payload.put("sdpMLineIndex", candidate.sdpMLineIndex);
            payload.put("sdpMid", candidate.sdpMid);
            payload.put("candidate", candidate.sdp);

            // Prepare data
            message.put("type", type);
            message.put("session", Session.get());
            message.put("data", payload);
        } catch (JSONException e) {
            LOG.error("Candidate encode error: " + e.toString());
            e.printStackTrace();
            this.stateDispatcher.error("encode", e.toString());
            return;
        } catch (SessionUnavailableException e) {
            LOG.error("Session unavailable error: " + e.toString());
            e.printStackTrace();
            this.stateDispatcher.error("session", e.toString());
            return;
        }

        // Send candidate
        this.send(message);
    }

    protected void receiveCandidate(IceCandidate candidate) {
        LOG.debug("Broadcasting candidate");
        this.messageDispatcher.candidate(candidate);
    }

    protected void startConnectTimer(int delay) {
        Handler.postDelayed(this.connectTimer, delay);
    }

    protected void restartConnectTimer(int delay) {
        this.cancelConnectTimer();
        this.startConnectTimer(delay);
    }

    protected void cancelConnectTimer() {
        Handler.removeCallbacks(this.connectTimer);
    }

    public void sendCached() {
        LOG.debug("Sending " + this.cached.size() + " delayed messages");
        for (CachedItem item : this.cached) {
            this.send(item.message, item.encrypt);
        }
        this.cached.clear();
    }

    protected void send(JSONObject message) {
        this.send(message, true);
    }

    protected void send(JSONObject message, boolean encrypt) {
        // Delay sending until connected
        if (this.ws != null && this.ws.isConnected()) {
            LOG.debug("Sending message (encrypted: " + encrypt + "): " + message);
            if (encrypt) {
                KeyStore.Box box;
                try {
                    // Encrypt data
                    box = this.encrypt(message.toString());
                } catch (CryptoException e) {
                    this.stateDispatcher.error(e.getState(), e.getError());
                    return;
                }

                // Send buffer content as byte array
                this.ws.sendBinaryMessage(box.getBuffer().array());
            } else {
                this.ws.sendTextMessage(message.toString());
            }
        } else {
            LOG.debug("Delaying message until WebSocket is open");
            this.cached.add(new CachedItem(message, encrypt));
        }
    }

    protected void receiveText(String data) {
        try {
            // Decode data
            LOG.debug("Received text message: " + data);
            JSONObject message = new JSONObject(data);
            String type = message.getString("type");

            // Relay message
            //noinspection IfCanBeSwitch
            if (type.equals("reset")) {
                this.receiveReset();
            } else if (type.equals("send-error")) {
                this.receiveSendError();
            } else {
                LOG.error("Ignored text message: " + data);
            }
        } catch (JSONException e) {
            LOG.error("Ignored invalid text message: " + data);
        }
    }

    protected void receiveBinary(String data) {
        try {
            // Decode data
            LOG.debug("Received encrypted message: " + data);
            JSONObject message = new JSONObject(data);
            String type = message.getString("type");

            // Check session
            String session = message.getString("session");
            if (!type.equals("offer") && !equals(session)) {
                LOG.error("Ignored message from another session: " + session);
                return;
            }

            // Relay message
            //noinspection IfCanBeSwitch
            if (type.equals("offer")) {
                JSONObject payload = message.getJSONObject("data");
                this.receiveOffer(new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type),
                        payload.getString("sdp")
                ), session);
            } else if (type.equals("candidate")) {
                JSONObject payload = message.getJSONObject("data");
                this.receiveCandidate(new IceCandidate(
                        payload.getString("sdpMid"),
                        payload.getInt("sdpMLineIndex"),
                        payload.getString("candidate")
                ));
            } else {
                LOG.error("Ignored encrypted message: " + data);
            }
        } catch (JSONException e) {
            LOG.error("Ignored invalid encrypted message: " + data);
        }
    }
}

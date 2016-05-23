package org.saltyrtc.client;

import android.content.Context;
import android.util.Log;
import org.webrtc.*;
import org.webrtc.PeerConnection.*;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * The connection between the peers (obviously). Creates the data channel and handles connection
 * events.
 * Public methods can be used safely from any thread.
 */
public class PeerConnection {
    protected static final String NAME = "PeerConnection";
    protected PeerConnectionFactory factory;
    protected String state = null;
    protected org.webrtc.PeerConnection pc;
    protected final org.saltyrtc.client.DataChannel dc;
    protected MediaConstraints constraints = new MediaConstraints();
    protected LinkedList<IceServer> iceServers = new LinkedList<>();
    protected boolean descriptionsExchanged;
    protected ArrayList<IceCandidate> localCandidates;
    protected ArrayList<IceCandidate> remoteCandidates;
    protected Events events;
    protected LocalDescriptionEvents localDescriptionEvents;
    protected RemoteDescriptionEvents remoteDescriptionEvents;
    // Ex protected
    public final StateDispatcher stateDispatcher = new StateDispatcher();
    // Ex protected
    public final PeerConnectionMessageDispatcher messageDispatcher = new PeerConnectionMessageDispatcher();

    /**
     * Listener for message (answer and candidate) dispatch request events.
     */
    public interface MessageListener {
        void onAnswer(SessionDescription description);
        void onCandidate(IceCandidate candidate);
    }

    /**
     * Handles signaling server events and dispatches messages.
     */
    protected class Events implements Observer {
        private volatile boolean stopped = false;

        public void stop() {
            this.stopped = true;
        }

        @Override
        public void onRenegotiationNeeded() {
            if (!this.stopped) {
                Log.w(NAME, "Ignored renegotiation request");
            }
        }

        @Override
        public void onIceCandidate(final IceCandidate iceCandidate) {
            // Note: This check might not be necessary but the browser does it as well
            if (iceCandidate != null) {
                Handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!stopped) {
                            sendCandidate(iceCandidate);
                        }
                    }
                });
            }
        }

        @Override
        public void onSignalingChange(SignalingState signalingState) {
            if (!this.stopped) {
                Log.d(NAME, "Ignored signaling state change to: " + signalingState.toString());
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            if (!this.stopped) {
                Log.w(NAME, "Ignored incoming media stream");
            }
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            if (!this.stopped) {
                Log.w(NAME, "Ignored media stream removal");
            }
        }

        @Override
        public void onIceConnectionChange(IceConnectionState iceConnectionState) {
            final String state = iceConnectionState.toString().toLowerCase();
            // Set state
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!stopped) {
                        setState(state);
                    }
                }
            });
        }

        @Override
        public void onIceGatheringChange(IceGatheringState iceGatheringState) {
            if (this.stopped) {
                return;
            }
            Log.d(NAME, "Ignored ICE gathering state change to: " + iceGatheringState.toString());
        }

        @Override
        public void onDataChannel(final org.webrtc.DataChannel incomingDc) {
            // Validate label
            if (incomingDc.label().equals(org.saltyrtc.client.DataChannel.LABEL)) {
                // Set incoming data channel instance
                Handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (stopped) {
                            return;
                        }
                        Log.i(NAME, "Received channel");
                        dc.setInstance(incomingDc);
                    }
                });
            } else {
                Log.w(NAME, "Ignored channel with label: " + incomingDc.label());
            }
        }
    }

    /**
     * Handles session description events that have been emitted by the local side.
     */
    protected class LocalDescriptionEvents implements SdpObserver {
        private volatile boolean stopped = false;

        public void stop() {
            this.stopped = true;
        }

        @Override
        public void onCreateSuccess(final SessionDescription originalDescription) {
            // Set local description after creation
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (stopped) {
                        return;
                    }
                    // Dirty hack to increase the application specific bandwidth
                    // Note: This setting exists because the current SCTP over DTLS implementation
                    //       lacks a flow control mechanism. However, 30 kbps is really not what
                    //       we want here, so we increase the allowed bandwidth.
                    final SessionDescription modifiedDescription;
                    SessionDescription.Type type = originalDescription.type;
                    String[] parts = originalDescription.description.split("b=AS:30");
                    if (parts.length == 2) {
                        Log.d(NAME, "Overriding bandwidth setting to 100 Mbps");
                        modifiedDescription = new SessionDescription(
                                type, (parts[0] + "b=AS:102400" + parts[1]));
                    } else {
                        Log.w(NAME, "Couldn't override bandwidth setting");
                        modifiedDescription = originalDescription;
                    }
                    pc.setLocalDescription(localDescriptionEvents, modifiedDescription);
                }
            });
        }

        @Override
        public void onSetSuccess() {
            Log.d(NAME, "Local description set");
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (stopped) {
                        return;
                    }
                    // Answer created
                    SessionDescription description = pc.getLocalDescription();
                    // Note: So this thing can be null for some idiotic reason...
                    if (description == null) {
                        stateDispatcher.error("local", "Local description was null");
                        return;
                    }
                    // Send the answer
                    messageDispatcher.answer(description);
                    // Send the local candidates and set the remote candidates
                    handleCachedCandidates();
                }
            });
        }

        @Override
        public void onCreateFailure(String error) {
            if (this.stopped) {
                return;
            }
            Log.e(NAME, "Creating answer failed: " + error);
            stateDispatcher.error("create", error);
        }

        @Override
        public void onSetFailure(String error) {
            if (this.stopped) {
                return;
            }
            Log.e(NAME, "Setting local description failed: " + error);
            stateDispatcher.error("local", error);
        }
    }

    /**
     * Handles session description events that have been emitted by the remote side.
     */
    protected class RemoteDescriptionEvents implements SdpObserver {
        private volatile boolean stopped = false;

        public void stop() {
            this.stopped = true;
        }

        @Override
        public void onCreateSuccess(SessionDescription description) {
            if (this.stopped) {
                return;
            }
            // Note: Not used, should never trigger
            Log.w(NAME, "Ignored remote description create event");
        }

        @Override
        public void onSetSuccess() {
            Log.d(NAME, "Remote description set");
            Handler.post(new Runnable() {
                @Override
                public void run() {
                    if (stopped) {
                        return;
                    }
                    // Offer received: Send answer
                    sendAnswer();
                }
            });
        }

        @Override
        public void onCreateFailure(String error) {
            if (this.stopped) {
                return;
            }
            // Note: Not used, should never trigger
            Log.w(NAME, "Ignored remote description creation failure: " + error);
        }

        @Override
        public void onSetFailure(String error) {
            if (this.stopped) {
                return;
            }
            Log.e(NAME, "Setting remote description failed: " + error);
            stateDispatcher.error("remote", error);
        }
    }

    /**
     * TODO: Description
     *
     * @param dc The used data channel (wrapper) instance.
     * @param context Required because WebRTC init stuff... dunno, just don't ask
     */
    public PeerConnection(org.saltyrtc.client.DataChannel dc, Context context) {
        this.dc = dc;

        // Set initial state
        this.setState("unknown");

        // Some init stuff... no idea why it's required and what it's doing exactly but otherwise
        // the app crashes...
        PeerConnectionFactory.initializeFieldTrials(null);

        // For some shitty reason we need to initialise audio here...
        // See: https://code.google.com/p/webrtc/issues/detail?id=3416
        if (!PeerConnectionFactory.initializeAndroidGlobals(
                context, true, false, false, null
        )) {
            Log.e(NAME, "Initialising Android globals failed!");
            this.stateDispatcher.error("init", "Initialising Android globals failed");
            return;
        }

        // Now we can safely create the factory... hopefully...
        this.factory = new PeerConnectionFactory();

        // Session description constraints
        this.constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "false"
        ));
        this.constraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveAudio", "false"
        ));

        // Set ice servers
        this.iceServers.add(new IceServer(
                "turn:example.org",
                "user",
                "pass"
        ));
    }

    protected void setState(String state) {
        // Ignore repeated state changes
        if (state.equals(this.state)) {
            Log.d(NAME, "Ignoring repeated state: " + state);
            return;
        }

        // Update state and notify listeners
        this.state = state;
        this.stateDispatcher.state(state);
    }

    // Ex protected
    public void reset() {
        this.setState("unknown");

        // Close and reset event instances
        if (this.events != null) {
            this.events.stop();
            this.localDescriptionEvents.stop();
            this.remoteDescriptionEvents.stop();
        }
        this.events = new Events();
        this.localDescriptionEvents = new LocalDescriptionEvents();
        this.remoteDescriptionEvents = new RemoteDescriptionEvents();

        // Close peer connection instance
        if (this.pc != null) {
            Log.d(NAME, "Closing");
            this.pc.close();
            this.pc.dispose();
            this.pc = null;
        }
        this.descriptionsExchanged = false;

        // Cached ICE candidates
        this.localCandidates = new ArrayList<>();
        this.remoteCandidates = new ArrayList<>();
    }

    // Ex protected
    public void create() {
        this.create(null, null);
    }

    protected void create(MediaConstraints constraints, LinkedList<IceServer> iceServers) {
        // Override defaults
        if (constraints != null) {
            this.constraints = constraints;
        }
        if (iceServers != null) {
            this.iceServers = iceServers;
        }

        // Enable data channel communication with Firefox and Chromium
        // Note: This shouldn't be necessary anymore but it doesn't do any harm either
        MediaConstraints peerConstraints = new MediaConstraints();
        peerConstraints.optional.add(new MediaConstraints.KeyValuePair(
                "DtlsSrtpKeyAgreement", "true"
        ));

        // Create peer connection
        this.setState("init");
        this.pc = this.factory.createPeerConnection(
                this.iceServers, peerConstraints, this.events
        );
        Log.d(NAME, "Peer Connection created");
    }

    // Ex protected
    public void receiveOffer(SessionDescription description) {
        Log.i(NAME, "Received offer");
        this.pc.setRemoteDescription(this.remoteDescriptionEvents, description);
    }

    protected void sendAnswer() {
        Log.i(NAME, "Creating answer");
        this.pc.createAnswer(this.localDescriptionEvents, this.constraints);
    }

    protected void sendCandidate(IceCandidate candidate) {
        if (this.descriptionsExchanged) {
            // Send candidate
            Log.d(NAME, "Broadcasting candidate");
            this.messageDispatcher.candidate(candidate);
        } else {
            // Cache candidates if no answer has been received yet
            this.localCandidates.add(candidate);
        }
    }

    // Ex protected
    public void receiveCandidate(IceCandidate candidate) {
        Log.d(NAME, "Received candidate");
        if (!this.descriptionsExchanged) {
            // Queue candidates if not connected
            // Note: This is required because the app will crash if a candidate is added
            //       before the local description has been set.
            Log.d(NAME, "Delaying setting remote candidate until descriptions have been exchanged");
            this.remoteCandidates.add(candidate);
        } else {
            // Note: A weird freeze occurred here... if this happens again, you're fucked!
            this.pc.addIceCandidate(candidate);
            Log.d(NAME, "Candidate set");
        }
    }

    protected void handleCachedCandidates() {
        this.descriptionsExchanged = true;

        // Send cached local candidates
        Log.d(NAME, "Sending " + this.localCandidates.size() + " delayed local candidates");
        for (IceCandidate candidate : this.localCandidates) {
            this.sendCandidate(candidate);
        }
        this.localCandidates.clear();

        // Set cached remote candidates
        Log.d(NAME, "Setting " + this.remoteCandidates.size() + " delayed remote candidates");
        for (IceCandidate candidate : this.remoteCandidates) {
            this.pc.addIceCandidate(candidate);
        }
        this.remoteCandidates.clear();
    }
}

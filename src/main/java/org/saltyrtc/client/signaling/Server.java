package org.saltyrtc.client.signaling;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.signaling.state.ServerHandshakeState;

/**
 * Information about the server.
 */
public class Server extends Peer {
    private static short ID = 0x00;

    public ServerHandshakeState handshakeState = ServerHandshakeState.NEW;

    public Server() {
        super(Server.ID);
    }

    @NonNull
    @Override
    public String getName() {
        return "Server";
    }
}

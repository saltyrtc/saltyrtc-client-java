package org.saltyrtc.client.signaling;

/**
 * Information about the server.
 */
public class Server extends Peer {
    private static short ID = 0x00;

    public Server() {
        super(Server.ID);
    }
}

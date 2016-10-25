package org.saltyrtc.client.signaling;

import org.saltyrtc.client.annotations.NonNull;
import org.saltyrtc.client.cookie.CookiePair;

/**
 * Information about the server.
 */
public class Server extends Peer {
    private static short ID = 0x00;

    public Server(@NonNull CookiePair cookiePair, @NonNull byte[] sessionKey) {
        super(Server.ID, cookiePair);
        this.setSessionKey(sessionKey);
    }
}

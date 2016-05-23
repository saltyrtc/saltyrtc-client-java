package org.saltyrtc.client;

import android.util.Log;
import org.saltyrtc.client.exceptions.SessionUnavailableException;

public class Session {
    protected static final String NAME = "Session";
    protected static String id = null;

    protected synchronized static boolean equals(String otherId) {
        return id != null && id.equals(otherId);
    }

    protected synchronized static String get() throws SessionUnavailableException {
        if (id == null) {
            throw new SessionUnavailableException();
        }
        return id;
    }

    protected synchronized static void set(String id) {
        Log.d(NAME, "New: " + id);
        Session.id = id;
    }

    // Ex protected
    public synchronized static void reset() {
        id = null;
        Log.d(NAME, "Reset");
    }
}

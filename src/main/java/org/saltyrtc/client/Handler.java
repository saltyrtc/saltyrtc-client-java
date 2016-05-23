package org.saltyrtc.client;

/**
 * A globally accessible handler proxy that is used for the whole package
 * to avoid the need for synchronisation.
 */
public class Handler {
    protected static android.os.Handler handler;

    /**
     * Note: This method has to be called before any other method can be called safely.
     * @param handler A handler instance.
     */
    public static void setHandler(android.os.Handler handler) {
        Handler.handler = handler;
    }

    public static boolean post(Runnable runnable) {
        return handler.post(runnable);
    }

    public static boolean postDelayed(Runnable runnable, long delay) {
        return handler.postDelayed(runnable, delay);
    }

    public static void removeCallbacks(Runnable runnable) {
        handler.removeCallbacks(runnable);
    }
}

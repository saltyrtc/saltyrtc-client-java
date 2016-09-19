package org.saltyrtc.client.exceptions;

/**
 * Thrown by SaltyRTCBuilder if a method is called when the builder isn't properly initialized
 * yet.
 */
public class InvalidBuilderStateException extends RuntimeException {
    public InvalidBuilderStateException() {
        super();
    }

    public InvalidBuilderStateException(String message) {
        super(message);
    }

    public InvalidBuilderStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidBuilderStateException(Throwable cause) {
        super(cause);
    }
}

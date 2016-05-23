package org.saltyrtc.client.exceptions;

public class CryptoException extends Exception {
    protected final String state;
    protected final String error;

    public CryptoException(final String state, final String error) {
        this.state = state;
        this.error = error;
    }

    public String getState() {
        return state;
    }

    public String getError() {
        return error;
    }
}

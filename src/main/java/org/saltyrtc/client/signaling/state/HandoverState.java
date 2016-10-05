package org.saltyrtc.client.signaling.state;

public class HandoverState {
    private boolean local;
    private boolean peer;

    public HandoverState() {
        this.reset();
    }

    public boolean getLocal() {
        return this.local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean getPeer() {
        return this.peer;
    }

    public void setPeer(boolean peer) {
        this.peer = peer;
    }

    public void reset() {
        this.local = false;
        this.peer = false;
    }
}

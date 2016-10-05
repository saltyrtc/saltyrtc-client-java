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

    /**
     * Return true only if both we and the peer have finished handover.
     */
    public boolean getAll() {
        return this.local && this.peer;
    }

    /**
     * Return true if either we or the peer have finished handover.
     */
    public boolean getAny() {
        return this.local || this.peer;
    }

    public void reset() {
        this.local = false;
        this.peer = false;
    }
}

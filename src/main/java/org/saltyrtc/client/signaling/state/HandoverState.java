package org.saltyrtc.client.signaling.state;

import org.saltyrtc.client.events.Event;
import org.saltyrtc.client.events.EventRegistry;

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
        final boolean wasAll = this.getAll();
        this.local = local;
        if (!wasAll && this.getAll()) {
            this.handoverComplete.notifyHandlers(new HandoverComplete());
        }
    }

    public boolean getPeer() {
        return this.peer;
    }

    public void setPeer(boolean peer) {
        final boolean wasAll = this.getAll();
        this.peer = peer;
        if (!wasAll && this.getAll()) {
            this.handoverComplete.notifyHandlers(new HandoverComplete());
        }
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

    /**
     * Event that indicates that both sides have finished the handover.
     */
    public static class HandoverComplete implements Event { }

    /**
     * Registry for `HandoverComplete` event handlers.
     */
    public final EventRegistry<HandoverComplete> handoverComplete = new EventRegistry<>();
}

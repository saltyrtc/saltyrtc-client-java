package org.saltyrtc.client.signaling;

import org.saltyrtc.client.nonce.CombinedSequence;

public abstract class Peer {
    protected byte[] permanentKey;
    protected byte[] sessionKey;
    protected CombinedSequence csn;

    public Peer() {
        this.csn = new CombinedSequence();
    }

    public byte[] getPermanentKey() {
        return permanentKey;
    }

    public void setPermanentKey(byte[] permanentKey) {
        this.permanentKey = permanentKey;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public CombinedSequence getCsn() {
        return csn;
    }
}
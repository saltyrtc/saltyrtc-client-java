/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client;

import java.util.HashMap;

/**
 * Contains state instances for signaling, peer connection, data channel and the
 * summarising client state.
 * Note: Public methods can be used safely from any thread.
 */
public class States {
    protected final ClientState client;
    protected final HashMap<String, InternalState> states = new HashMap<>();

    public States() {
        // Setup internal states
        InternalState signaling = new InternalState("signaling");
        InternalState pc = new InternalState("pc");
        InternalState dc = new InternalState("dc");
        this.states.put("signaling", signaling);
        this.states.put("pc", pc);
        this.states.put("dc", dc);

        // Signaling state rules
        this.setupRules(signaling, StateType.DANGER,
                new String[]{"unknown", "failed"});
        this.setupRules(signaling, StateType.WARNING,
                new String[]{"connecting", "closing", "closed"});
        this.setupRules(signaling, StateType.SUCCESS,
                new String[]{"open"});

        // Peer Connection state rules
        this.setupRules(pc, StateType.DANGER,
                new String[]{"unknown", "init", "new", "failed", "closed"});
        this.setupRules(pc, StateType.WARNING,
                new String[]{"checking", "disconnected"});
        this.setupRules(pc, StateType.SUCCESS,
                new String[]{"connected", "completed"});

        // Data Channel state rules
        this.setupRules(dc, StateType.DANGER,
                new String[]{"unknown", "init", "closed"});
        this.setupRules(dc, StateType.WARNING,
                new String[]{"connecting", "closing"});
        this.setupRules(dc, StateType.SUCCESS,
                new String[]{"open"});

        // Setup client state
        this.client = new ClientState("client");
        this.client.update(this.states);
    }

    public ClientState getClient() {
        return this.client;
    }

    public InternalState getSignaling() {
        return this.states.get("signaling");
    }

    public InternalState getPeerConnection() {
        return this.states.get("pc");
    }

    public InternalState getDataChannel() {
        return this.states.get("dc");
    }

    protected void setupRules(InternalState state, int type, String[] values) {
        HashMap<String, Integer> rules = new HashMap<>();
        for (String value : values) {
            rules.put(value, type);
        }
        state.addRules(rules);
    }

    // Ex protected
    public void reset() {
        this.getSignaling().reset();
        this.getPeerConnection().reset();
        this.getDataChannel().reset();
        this.client.update(this.states);
    }

    // Ex protected
    public void updateSignaling(String value) {
        this.getSignaling().update(value);
        this.client.update(this.states);
    }

    // Ex protected
    public void updatePeerConnection(String value) {
        this.getPeerConnection().update(value);
        this.client.update(this.states);
    }

    // Ex protected
    public void updateDataChannel(String value) {
        this.getDataChannel().update(value);
        this.client.update(this.states);
    }
}

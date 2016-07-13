package org.saltyrtc.client.datachannel;

import org.saltyrtc.client.SaltyRTC;
import org.webrtc.DataChannel;

/**
 * A wrapper for a DataChannel that will encrypt and decrypt data on the fly.
 */
public class SecureDataChannel {

    private final DataChannel dc;
    private final SaltyRTC salty;

    public SecureDataChannel(DataChannel dc, SaltyRTC salty) {
        this.dc = dc;
        this.salty = salty;
    }

    public void registerObserver(final DataChannel.Observer observer) {
        this.dc.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                observer.onBufferedAmountChange(l);
            }

            @Override
            public void onStateChange() {
                observer.onStateChange();
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                System.err.println("INTERCEPT ALL THE THINGS");

                String msg = buffer.data.asCharBuffer().toString();
                System.err.println("Secret message was: " + msg);
                observer.onMessage(buffer);
            }
        });
    }

    public void unregisterObserver() {
        this.dc.unregisterObserver();
    }

    public String label() {
        return this.dc.label();
    }

    public DataChannel.State state() {
        return this.dc.state();
    }

    public long bufferedAmount() {
        return this.dc.bufferedAmount();
    }

    public void close() {
        this.dc.close();
    }

    public boolean send(DataChannel.Buffer buffer) {
        System.err.println("ENCRYPT ALL THE THINGS");
        return this.dc.send(buffer);
    }

    public void dispose() {
        this.dc.dispose();
    }

}

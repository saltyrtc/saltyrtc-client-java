package org.saltyrtc.client;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * This interface can be implemented to dynamically determine the host and port of a SaltyRTC
 * server based on the public key of the intiator.
 *
 * This allows techniques like load balancing based on a prefix of the public key.
 */
public interface SaltyRTCServerInfo {
    String getHost(String initiatorPublicKey);
    int getPort(String initiatorPublicKey);
    SSLContext getSSLContext(String initiatorPublicKey);

    /**
     * Return a non-null value if the WebSocket should use the SSLSocketFactory returned by this
     * method instead of the one associated to the SSLContext returned by getSSLContext.
     */
    default SSLSocketFactory getSSLSocketFactory(String initiatorPublicKey) {
        return null;
    }
}

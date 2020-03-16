package org.saltyrtc.client;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * This interface can be implemented to dynamically determine the host and port of a SaltyRTC
 * server based on the public key of the intiator.
 *
 * This allows techniques like load balancing based on a prefix of the public key.
 *
 * Note: Returning a non-null value for both getSSLContext and getSSLSocketFactory is not
 *       allowed since they are mutually exclusive.
 */
public interface SaltyRTCServerInfo {
    String getHost(String initiatorPublicKey);
    int getPort(String initiatorPublicKey);

    /**
     * Return null if you do not want to use TLS. Otherwise, return a non-null value if the
     * WebSocket should use the SSLContext returned by this method.
     */
    default SSLContext getSSLContext(String initiatorPublicKey) {
        return null;
    }

    /**
     * Return null if you do not want to use TLS. Otherwise, return a non-null value if the
     * WebSocket should use the SSLSocketFactory returned by this method.
     */
    default SSLSocketFactory getSSLSocketFactory(String initiatorPublicKey) {
        return null;
    }
}

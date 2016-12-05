package org.saltyrtc.client;

import javax.net.ssl.SSLContext;

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
}

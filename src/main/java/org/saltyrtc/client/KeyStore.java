package org.saltyrtc.client;

import org.saltyrtc.client.exceptions.CryptoFailedException;
import org.saltyrtc.client.exceptions.InvalidKeyException;
import org.saltyrtc.client.exceptions.OtherKeyMissingException;
import org.slf4j.Logger;

import com.neilalexander.jnacl.NaCl;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * Handles encrypting and decrypting messages for the peers.
 * Note: This class is thread safe.
 */
public class KeyStore {
    protected static final Logger LOG = org.slf4j.LoggerFactory.getLogger(KeyStore.class);
    protected static boolean setupDone = false;
    protected static final byte[] privateKey = new byte[NaCl.SECRETKEYBYTES];
    protected static final byte[] publicKey = new byte[NaCl.PUBLICKEYBYTES];
    protected static byte[] otherKey = null;
    protected static NaCl nacl = null;
    protected static final SecureRandom random = new SecureRandom();

    public static class Box {
        public final byte[] nonce;
        public final byte[] data;

        public Box(byte[] nonce, byte[] data) {
            this.nonce = nonce;
            this.data = data;
        }

        public Box(ByteBuffer buffer) {
            // Unpack nonce
            this.nonce = new byte[NaCl.NONCEBYTES];
            buffer.get(nonce, 0, NaCl.NONCEBYTES);

            // Unpack data
            this.data = new byte[buffer.remaining()];
            buffer.get(data);
        }

        public int getSize() {
            return this.nonce.length + this.data.length;
        }

        public ByteBuffer getBuffer() {
            // Pack data
            // Note: 'allocateDirect' does NOT work, DO NOT CHANGE!
            ByteBuffer box = ByteBuffer.allocate(this.getSize());
            box.put(this.nonce);
            box.put(this.data);

            // Flip offset and remaining length for reading
            box.flip();

            // Return box as byte buffer
            return box;
        }
    }

    public synchronized static String getPublicKey() {
        return NaCl.asHex(publicKey);
    }

    public synchronized static void setOtherKey(String otherKey) throws InvalidKeyException {
        // Start setup if required
        setup();
        // Store binary key
        KeyStore.otherKey = NaCl.getBinary(otherKey);
        // Create getNaCl for encryption and decryption
        try {
            nacl = new NaCl(privateKey, KeyStore.otherKey);
        } catch (Error e) {
            throw new InvalidKeyException(e.toString());
        }
    }

    public synchronized static NaCl getNaCl() throws OtherKeyMissingException {
        if (nacl == null) {
            throw new OtherKeyMissingException();
        }
        return nacl;
    }

    public synchronized static void setup() {
        // Skip if already set up
        if (setupDone) {
            return;
        }

        // Generate key pair
        LOG.debug("Generating new key pair");
        NaCl.genkeypair(KeyStore.publicKey, KeyStore.privateKey);
        LOG.debug("Private key: " + NaCl.asHex(KeyStore.privateKey));
        LOG.debug("Public key: " + NaCl.asHex(KeyStore.publicKey));
        setupDone = true;

        // Make sure encryption and decryption works properly
        // TODO: Self-test
        NaCl nacl = KeyStore.nacl;
        KeyStore.nacl = new NaCl(privateKey, publicKey);
        String expected = Utils.getRandomString();
        try {
            if (!decrypt(encrypt(expected)).equals(expected)) {
                throw new AssertionError("Self-test failed");
            }
        } catch (Exception e) {
            LOG.error("Self-test failed");
            e.printStackTrace();
        }
        LOG.debug("Self-test passed");
        KeyStore.nacl = nacl;
    }

    public static Box encrypt(String message) throws
            OtherKeyMissingException, UnsupportedEncodingException,
            CryptoFailedException {
        // Convert string to bytes
        byte[] data = message.getBytes("UTF-8");

        // Generate random nonce
        byte[] nonce = new byte[NaCl.NONCEBYTES];
        random.nextBytes(nonce);

        // Encrypt data with keys and nonce
        try {
            data = getNaCl().encrypt(data, nonce);
        } catch (Error e) {
            throw new CryptoFailedException(e.toString());
        }
        if (data == null) {
            throw new CryptoFailedException("Encrypted data is null");
        }

        // Return box
        return new Box(nonce, data);
    }

    public static String decrypt(Box box) throws
            OtherKeyMissingException, UnsupportedEncodingException,
            CryptoFailedException {
        // Decrypt data
        byte[] data;
        try {
            data = getNaCl().decrypt(box.data, box.nonce);
        } catch (Error e) {
            throw new CryptoFailedException(e.toString());
        }
        if (data == null) {
            throw new CryptoFailedException("Decrypted data is null");
        }

        // Return data as string
        return new String(data, "UTF-8");
    }
}

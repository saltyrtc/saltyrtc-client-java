# Usage

This chapter gives a short introduction on how to use the SaltyRTC JavaScript client.

To see a more practical example, you may also want to take a look at our [demo
application](https://github.com/saltyrtc/saltyrtc-demo).

## The CryptoProvider

SaltyRTC is based on the [NaCl](https://nacl.cr.yp.to/) crypto system, which
features both symmetric and asymmetric encryption. However, there are various
implementations of NaCl for different target systems with different performance
characteristics, therefore the crypto backend in this library is pluggable.

In order to instantiate a `SaltyRTC` instance or a `KeyStore`, you need to
provide an implementation of the `org.saltyrtc.client.crypto.CryptoProvider`
interface. For ease of use, this library provides a pure-java implementation of
such a CryptoProvider using [jnacl](https://github.com/neilalexander/jnacl). To
use, simply create an instance of the `JnaclCryptoProvider`.

```java
import org.saltyrtc.client.crypto.JnaclCryptoProvider;

final CryptoProvider cryptoProvider = new JnaclCryptoProvider();
```

## The SaltyRTCBuilder

To initialize a `SaltyRTC` instance, you can use the `SaltyRTCBuilder`.

```java
import org.saltyrtc.client.SaltyRTCBuilder;

final SaltyRTCBuilder builder = new SaltyRTCBuilder(cryptoProvider);
```

### Connection info

Then you need to provide connection info:

```java
builder.connectTo(host, port, sslContext);
```

For testing, you can use [our test server](https://saltyrtc.org/pages/getting-started.html).

### Key store

The client needs to have its own public/private keypair. Create a new keypair
with the `KeyStore` class:

```java
import org.saltyrtc.client.keystore.KeyStore;

final KeyStore keyStore = new KeyStore(cryptoProvider);
builder.withKeyStore(keyStore);
```

### Server key pinning

If you want to use server key pinning, specify the server public permanent key:

```java
builder.withServerKey(serverPublicPermanentKey);
```

The `serverPublicPermanentKey` can be either a byte array or a hex encoded string.

### Websocket connect timeout

Optionally, you can specify the Websocket connection timeout in milliseconds:

```java
builder.withWebsocketConnectTimeout(5000);
```

### Websocket ping interval

Optionally, you can specify a Websocket ping interval in seconds:

```java
builder.withPingInterval(60);
```

### Task configuration

You must initialize SaltyRTC with a task (TODO: Link to tasks documentation)
that takes over after the handshake is done.

For example, when using the [WebRTC task](https://github.com/saltyrtc/saltyrtc-task-webrtc-java):

```java
import org.saltyrtc.client.tasks.Task;
import org.saltyrtc.tasks.webrtc.WebRTCTask;

builder.usingTasks(new Task[] { new WebRTCTask() });
```

### Connecting as Initiator

If you want to connect to the server as initiator, you can use the `.asInitiator()` method:

```java
final SaltyRTC client = builder.asInitiator();
```

### Connecting as Responder

If you want to connect as responder, you need to provide the initiator information first.

```java
builder.initiatorInfo(initiatorPublicPermanentKey, initiatorAuthToken);
final SaltyRTC client = builder.asResponder();
```

The initiator public permanent key as well as the initiator auth token can be
either byte arrays or hex encoded strings.

## Full example

All methods on the `SaltyRTCBuilder` support chaining. Here's a full example of an initiator configuration:

```java
import javax.net.ssl.SSLContext;
import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.SaltyRTCBuilder;
import org.saltyrtc.client.crypto.CryptoProvider;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.tasks.webrtc.WebRTCTask;

final SSLContext sslContext = SSLContext.getDefault();
final CryptoProvider cryptoProvider = new JnaclCryptoProvider();
final SaltyRTC client = new SaltyRTCBuilder(cryptoProvider)
        .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
        .withKeyStore(new KeyStore(cryptoProvider))
        .withPingInterval(60)
        .withWebsocketConnectTimeout(5000)
        .withServerKey(Config.SALTYRTC_SERVER_PUBLIC_KEY)
        .usingTasks(new Task[] { new WebRTCTask() })
        .asInitiator();
```

To see a more practical example, you may also want to take a look at our
[demo application](https://github.com/saltyrtc/saltyrtc-demo).

## Trusted keys

In order to reconnect to a session using a trusted key, you first need to
restore your `KeyStore` with the permanent keypair originally used to establish
the trusted session:

```java
final KeyStore keyStore = new KeyStore(cryptoProvider, ourPrivatePermanentKey);
builder.withKeyStore(keyStore);
```

The `ourPrivatePermanentKey` can be either a byte array or a hex encoded string.

Then, on the `SaltyRTCBuilder` instance, set the trusted peer key:

```java
builder.withTrustedPeerKey(peerPublicPermanentKey);
```

The `peerPublicPermanentKey` can be either a byte array or a hex encoded string.

## Event handlers

The SaltyRTC Client for Java emits a number of events during its lifecycle.
For all events, event handlers can be registered. Please take a look at
[the reference](reference/) for a list of all possible events.

Example for registering an event handler:

```java
client.events.signalingStateChanged.register(new EventHandler<SignalingStateChangedEvent>() {
    @Override
    public boolean handle(SignalingStateChangedEvent event) {
        System.out.println("Signaling state changed to " + event.getState().name());
        return false; // Don't deregister this event handler
    }
};
```

Every event class in `client.events` is an `EventRegistry`. For every event,
multiple event handlers can be registered and -- when desired --
deregistered.

To deregister event handlers, you have three options:

* Use the `event.unregister(instance)` method to deregister a specific
  event handler instance.
* Return `true` from with in an event handler to remove itself from the
  event registry.
* Use the `event.EVENT_NAME.clear()` method to remove all event handlers
  for that event.

To remove all handlers for all events, use the `client.events.clearAll()`
method.

## Dynamically determine server connection info

Instead of specifying the SaltyRTC server host, port and SSL context directly,
you can instead provide an implementation of `SaltyRTCServerInfo` that can
dynamically determine the connection info based on the public key of the
initiator.

```java
final SSLContext sslContext = SSLContext.getDefault();
final SaltyRTC responder = new SaltyRTCBuilder(cryptoProvider)
    .connectTo(new SaltyRTCServerInfo() {
        @Override
        public String getHost(String initiatorPublicKey) {
            if (initiatorPublicKey.startsWith("a")) {
                return "a.example.org";
            } else {
                return "other.example.org";
            }
        }

        @Override
        public int getPort(String initiatorPublicKey) {
            return Config.SALTYRTC_PORT;
        }

        @Override
        public SSLContext getSSLContext(String initiatorPublicKey) {
            return sslContext;
        }
    })
    // ...
```

## Logging

The library uses the [slf4j](https://www.slf4j.org/) logging API. Configure
a logger (e.g. slf4j-simple) to see the log output.

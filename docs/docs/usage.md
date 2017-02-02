# Usage

## The SaltyRTCBuilder

To initialize a `SaltyRTC` instance, you can use the `SaltyRTCBuilder`.

```java
import org.saltyrtc.client.SaltyRTCBuilder;

final SaltyRTCBuilder builder = new SaltyRTCBuilder();
```

### Connection info

Then you need to provide connection info:

```java
builder.connectTo(host, port, sslContext);
```

### Key store

The client needs to have its own public/private keypair. Create one with the
`KeyStore` class:

```java
import org.saltyrtc.client.keystore.KeyStore;

final KeyStore keyStore = new KeyStore();
builder.withKeyStore(keyStore);
```

### Server key pinning

If you want to use server key pinning, specify the server public permanent key:

```java
builder.withServerKey(serverPublicPermanentKey);
```

The `serverPublicPermanentKey` can be either a byte array or a hex encoded string.

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
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.tasks.webrtc.WebRTCTask;

final SSLContext sslContext = SSLContext.getDefault();
final SaltyRTC client = new SaltyRTCBuilder()
        .connectTo(Config.SALTYRTC_HOST, Config.SALTYRTC_PORT, sslContext)
        .withKeyStore(new KeyStore())
        .withPingInterval(60)
        .withServerKey(Config.SALTYRTC_SERVER_PUBLIC_KEY)
        .usingTasks(new Task[] { new WebRTCTask() })
        .asInitiator();
```

To see a more practical example, you may also want to take a look at our
[demo application](https://github.com/saltyrtc/saltyrtc-demo).

## Trusted keys

TODO

## Dynamically determine server connection info

Instead of specifying the SaltyRTC server host, port and SSL context directly,
you can instead provide an implementation of `SaltyRTCServerInfo` that can
dynamically determine the connection info based on the public key of the
initiator.

```java
final SSLContext sslContext = SSLContext.getDefault();
final SaltyRTC responder = new SaltyRTCBuilder()
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

The library uses the slf4j logging API. Configure a logger (e.g. slf4j-simple)
to see the log output.

# Changelog

This project follows semantic versioning.

Possible log types:

- `[added]` for new features.
- `[changed]` for changes in existing functionality.
- `[deprecated]` for once-stable features removed in upcoming releases.
- `[removed]` for deprecated features removed in this release.
- `[fixed]` for any bug fixes.
- `[security]` to invite users to upgrade in case of vulnerabilities.


### v0.9.0 (2017-02-07)

- [changed] Change subprotocol to `v1.saltyrtc.org` (#60)
- [added] Add new close code: 3007 Invalid Key (#58)
- [added] Add support for multiple server permanent keys (#58)

### v0.8.2 (2017-01-25)

- [fixed] If connection is closing or closed while sending a message,
  don't throw a ProtocolException

### v0.8.1 (2016-12-14)

- [fixed] The previous release was broken, this release should fix it

### v0.8.0 (2016-12-12)

- [changed] `SaltyRTC.sendApplicationMessage` now throws an
  `InvalidStateException` if SaltyRTC instance is not in `TASK` state
- [fixed] Stop logging private key
- [changed] "Secret key" renamed to "private key" everywhere
- [added] Implement dynamic server endpoints (#53)

### v0.7.1 (2016-11-15)

- [added] Add clearAll method to event registry (#39)

### v0.7.0 (2016-11-14)

- [added] Add support for `ping_interval` (#46)
- [added] Support sending Application messages (#47)
- [added] Allow hex strings as keys in `KeyStore` and `SaltyRTCBuilder` (#38)
- [fixed] Properly handle signaling errors (#36)
- [fixed] Send close message on disconnect
- [fixed] Close websocket on handover (#43, #49)
- [fixed] Drop inactive responders (#33)

### v0.6.2 (2016-11-10)

- [fixed] Validate source in send-error message
- [fixed] The `CloseEvent` should now be emitted even if the server closes the connection
- [fixed] Fix and simplify nonce validation

### v0.6.1 (2016-11-03)

- [fixed] Synchronize disconnect method (#41)
- [fixed] Fix for double-encryption of signaling messages (#42)

### v0.6.0 (2016-10-27)

- [added] Implement MITM prevention by accepting server keys in SaltyRTCBuilder (#29)
- [added] Update drop-responder messages (#35)
- [added] Support send-error messages (#37)
- [changed] Use custom `InvalidKeyException` instead of `java.security.InvalidKeyException`
- [fixed] Proper cookie / CSN scoping (#30)
- [fixed] Various small bugfixes and improvements

### v0.5.0 (2016-10-20)

- [fixed] Fix concurrency bug in `CombinedSequence`, introduce `CombinedSequenceSnapshot`

### v0.4.0 (2016-10-18)

- [changed] `Task.sendSignalingMessage` now throws `SignalingException`, not `ConnectionException`
- [fixed] Make sure that new responders aren't already known

### v0.3.0 (2016-10-06)

- [added] Implement support for tasks
- [added] Implement close messages
- [added] Add support for permanent server keys
- [added] Add `KeyStore(publicKey, secretKey)` constructor
- [changed] `keyStore.getPrivateKey()` is now `keyStore.getSecretKey()`
- [changed] Rename `OPEN` SignalingState to `TASK`
- [changed] Vendorize jnacl library
- [changed] `ProtocolException` is now a subclass of `SignalingException`
- [removed] Remove all WebRTC related functionality
- [removed] Remove restart message

### v0.2.0 (2016-09-19)

- [changed] Introduce `SaltyRTCBuilder` (#15)
- [added] Implement trusted keys (#16)

### v0.1.0 (2016-09-07)

- Initial release

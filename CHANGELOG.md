# Changelog

This project follows semantic versioning.

Possible log types:

- `[added]` for new features.
- `[changed]` for changes in existing functionality.
- `[deprecated]` for once-stable features removed in upcoming releases.
- `[removed]` for deprecated features removed in this release.
- `[fixed]` for any bug fixes.
- `[security]` to invite users to upgrade in case of vulnerabilities.


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

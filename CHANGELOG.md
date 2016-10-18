# Changelog

This project follows semantic versioning.

Possible log types:

- `[added]` for new features.
- `[changed]` for changes in existing functionality.
- `[deprecated]` for once-stable features removed in upcoming releases.
- `[removed]` for deprecated features removed in this release.
- `[fixed]` for any bug fixes.
- `[security]` to invite users to upgrade in case of vulnerabilities.


### v0.4.0 (2016-10-18)

- [changed] Task.sendSignalingMessage now throws SignalingException, not ConnectionException
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

# Reference

## Events

All events are accessible through the static `client.events` class.

### signalingStateChanged

This event is emitted every time the signaling state changes.

### handover

This event is emitted once the signaling channel has been handed over to
the task.

### applicationData

Application data has been received.

### signalingConnectionLost

The signaling server lost connection to the specified peer.

### peerDisconnected

A previously authenticated peer has disconnected from the server.

### close

The connection to the signaling server was closed.

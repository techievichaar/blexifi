# Protocol Draft (Relay-capable)

## Envelope fields

- `envelopeId` (UUID)
- `sourceId`
- `destinationId`
- `timestamp`
- `ttl` (decrement each hop)
- `hopCount`
- `requiresAck` (bool)
- `payloadType` (`text`, `ack`, `presence`, `route_hint`)
- `ciphertext` (for message payload)
- `signature` (optional but recommended)

## Relay rule set

On envelope receive:

1. If `envelopeId` already seen: drop.
2. Mark as seen (with expiry window).
3. If `destinationId == self`: process and maybe emit ACK.
4. Else if `ttl <= 0`: drop as expired.
5. Else choose next-hop neighbors and forward with `ttl-1`.

## ACK format

- `payloadType = ack`
- `ackForEnvelopeId`
- `fromId` (final receiver)
- `toId` (original sender)

ACKs can also be relayed using same forwarding logic.

## Loop and flood protection

- Hard max TTL (e.g., 6)
- Per-node recent envelope cache (LRU + time expiry)
- Limit fanout to top 1–2 candidates

## Privacy considerations

- Relay nodes should only see routing metadata.
- User content remains encrypted end-to-end.
- Rotate ephemeral session keys periodically.

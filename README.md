# Blexifi — Offline Bluetooth + Wi‑Fi Direct Chat (Android)

Blexifi is an offline chat concept for Android where devices exchange messages without internet using BLE/Wi‑Fi Direct and relay messages through nearby phones.

## What is implemented now

This repository now includes a **working Kotlin mesh-routing core** (protocol models + forwarding engine + tests):

- Relay envelope model with UUID, source, destination, TTL, hop count, payload type.
- Duplicate suppression via a seen-cache.
- TTL/hop-based forwarding logic.
- Neighbor scoring and top-k next-hop fanout.
- Unit tests including A→C→D→B relay simulation.

Code lives under:

- `src/main/kotlin/com/blexifi/mesh`
- `src/test/kotlin/com/blexifi/mesh`

## Product requirements addressed

1. Use BLE + Wi‑Fi Direct to chat offline.
2. Multi-hop relay when destination is out of direct range (e.g., A→C→D→B).
3. Android first (practical baseline: `minSdk 26`, not all historical Android versions).

## Run tests

```bash
gradle test
```

## High-level Android architecture (next integration step)

- Discovery layer: BLE advertiser/scanner for peer presence.
- Transport layer: BLE control + Wi‑Fi Direct bulk socket transfer.
- Routing layer: current `MeshRouter` core from this repo.
- Storage layer: Room DB for outbox/inbox + dedupe + route hints.

## Protocol and architecture references

- `docs/architecture.md`
- `docs/protocol.md`

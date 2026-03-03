# Blexifi — Offline Bluetooth + Wi‑Fi Direct Chat (Android)

This repository now contains a practical blueprint to build an **offline Android chat app** that works without internet using:

1. **BLE + Wi‑Fi Direct** for nearby device discovery and messaging.
2. **Store-and-forward mesh relays** so messages can hop through other phones (C, D) to reach an out-of-range user (B).
3. A realistic Android target strategy (not literally every version).

## Reality check for Android compatibility

"Any Android version" is not feasible for BLE + Wi‑Fi Direct mesh behavior. A practical and maintainable target is:

- **minSdk 26 (Android 8.0)**
- targetSdk latest stable

This keeps BLE, foreground service limits, and modern permissions manageable.

## High-level architecture

- **Discovery layer**
  - BLE advertisements for presence, device ID, capabilities.
  - Optional nearby peer hints over Wi‑Fi Direct.
- **Transport layer**
  - BLE GATT for control/metadata and low-bandwidth fallback.
  - Wi‑Fi Direct sockets for larger payloads.
- **Routing layer (mesh)**
  - Peer table with last-seen + link quality.
  - Message envelope with unique IDs, TTL/hop limit, ACKs.
  - Opportunistic forwarding to neighbors likely to reach destination.
- **Storage layer**
  - Room DB for conversations, pending outbox, dedupe cache, route hints.

See `docs/architecture.md` and `docs/protocol.md` for implementable details.

## User stories covered

- A and B in range: direct BLE/Wi‑Fi Direct message exchange.
- A and B out of range, but A↔C↔D↔B connected:
  - A sends encrypted message to B.
  - C and D only relay ciphertext/metadata.
  - B receives and ACKs; ACK propagates back to A.

## Security model (recommended)

- Identity key pair generated on first app launch.
- Session establishment through X25519 + authenticated fingerprint verification.
- End-to-end encryption for message body.
- Relay nodes cannot decrypt payload.

## Development phases

1. **Phase 1**: Direct P2P chat (BLE discovery + Wi‑Fi Direct send/receive).
2. **Phase 2**: Reliable messaging (IDs, ACK/retry, Room persistence).
3. **Phase 3**: Mesh relay (TTL, duplicate suppression, route scoring).
4. **Phase 4**: Security hardening + background performance tuning.
5. **Phase 5**: UX polish, battery optimization, beta testing across OEM devices.

## Non-goals (initial release)

- Guaranteed delivery with all devices asleep.
- Unlimited hop routing.
- iOS support.

## Next implementation step

Start by implementing the packet format and relay rules in `docs/protocol.md`, then build a small Android prototype with 3–4 phones to validate forwarding behavior.

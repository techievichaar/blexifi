# Blexifi — Offline Bluetooth + Wi‑Fi Direct Chat (Android)

Blexifi now contains:

1. A **mesh-routing core** (Java) for multi-hop forwarding logic.
2. An **Android app module** (Kotlin) with chat UI + foreground offline mesh service scaffold.

## What is implemented

### Core routing engine (`src/main/java/com/blexifi/mesh`)
- Envelope model with source/destination/TTL/hop count.
- ACK envelope generation and ACK relay handling.
- Duplicate suppression cache.
- Scored neighbor forwarding with fanout and previous-hop filtering.
- Relay decision output for deliver/drop/forward actions with envelope validation guardrails.
- AES-GCM encryption utility for payload confidentiality plus X25519-based shared-session key derivation helper.
- BLE presence payload codec (`BlePresencePayload`) for advertise/scan byte-format compatibility.
- Socket frame codec (`SocketFrameCodec`) for length-prefixed Wi‑Fi Direct payload transfer.
- Delivery persistence abstraction (`MessageStore`) with in-memory and file-backed implementations for durable retries.
- Delivery lifecycle manager with failure policy and exponential-backoff retry planning.

### Android app scaffold (`app/`, enable with `BLEXIFI_ENABLE_ANDROID=1`)
- `MainActivity` with simple chat UI.
- `OfflineMeshService` foreground service that initializes transports, builds BLE presence payload format, and schedules periodic retry work.
- `WifiDirectSocketManager` for socket server/client send-receive framing.
- Room database (`AppDatabase`, DAOs, entities) for peers/messages.
- WorkManager retry worker (`RetryOutboxWorker`) for pending outbox processing.

## Current status

- ✅ Routing, ACK, crypto, persistence, retry planning, and key-derivation core are runnable and tested.
- ✅ Android persistence and retry infrastructure (Room + WorkManager wiring) now exists.
- 🚧 Full BLE advertise/scan runtime callbacks and Wi‑Fi Direct group negotiation are scaffolded but not yet integrated end-to-end.
- 🚧 Device-level instrumentation, OEM battery-policy hardening, and UX polish are pending.

## Run core tests

```bash
JAVA_HOME=$HOME/.local/share/mise/installs/java/17.0.2 PATH=$JAVA_HOME/bin:$PATH gradle meshSelfTest
```

## Target Android baseline

- `minSdk 26` (Android 8.0)
- `targetSdk 34`

## Final steps to production

1. Wire BLE scanner/advertiser callbacks to `BlePresencePayload` decode + peer table updates.
2. Complete Wi‑Fi Direct group creation/discovery and connect sockets through `WifiDirectSocketManager`.
3. Connect `OfflineChatRepository` to Room DAOs for durable inbox/outbox state.
4. Add fingerprint verification UI + key rotation policy for X25519 identities.
5. Add instrumentation tests on multiple OEMs and optimize background behavior.

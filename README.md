# Blexifi — Offline Bluetooth + Wi‑Fi Direct Chat (Android)

Blexifi now contains:

1. A **mesh-routing core** (Java) for multi-hop forwarding logic.
2. An **Android app module** (Kotlin) with basic chat UI + foreground offline mesh service scaffold.

## What is implemented

### Core routing engine (`src/main/java/com/blexifi/mesh`)
- Envelope model with source/destination/TTL/hop count.
- ACK envelope generation and ACK relay handling.
- Duplicate suppression cache.
- Scored neighbor forwarding with fanout and previous-hop filtering.
- Relay decision output for deliver/drop/forward actions.
- AES-GCM encryption utility for payload confidentiality.
- Delivery persistence abstraction (`MessageStore`) with in-memory implementation.
- Delivery lifecycle manager that tracks pending/relayed/delivered/failed states and applies max-attempt failure policy.

### Android app scaffold (`app/`, enable with `BLEXIFI_ENABLE_ANDROID=1`)
- `MainActivity` with a simple chat screen (target input, message input, send button, service start button).
- `OfflineMeshService` foreground service scaffold prepared for BLE scanner + Wi‑Fi P2P manager wiring.
- `OfflineChatRepository` that bridges UI sends to mesh envelope generation/routing core.

## Current status

- ✅ Routing logic, ACK flow simulation, crypto utility, and delivery-state manager are runnable and tested.
- ✅ Android app structure is present and ready for transport integration.
- 🚧 Actual BLE packet exchange + Wi‑Fi Direct socket transport still needs full implementation.
- 🚧 Real device-to-device E2E key agreement/session management still pending.
- 🚧 Room-backed persistent store + WorkManager retry worker for Android app are pending.

## Run core tests

```bash
JAVA_HOME=$HOME/.local/share/mise/installs/java/17.0.2 PATH=$JAVA_HOME/bin:$PATH gradle meshSelfTest
```

## Target Android baseline

- `minSdk 26` (Android 8.0)
- `targetSdk 34`

## Next steps to reach production

1. Implement BLE advertise/scan payload format and parser.
2. Add Wi‑Fi Direct connection orchestration + socket channels.
3. Replace in-memory `MessageStore` with Room-backed persistence and recovery.
4. Add X25519 identity/session key exchange and fingerprint verification.
5. Add instrumentation tests across real devices and OEM battery-policy checks.

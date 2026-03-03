# Architecture Plan

## 1) Core modules

- `discovery`
  - BLE advertiser/scanner
  - Peer presence state machine
- `transport`
  - `BleTransport` (control, tiny payloads)
  - `WifiDirectTransport` (bulk messages)
  - Unified `Transport` interface for routing layer
- `routing`
  - Peer graph and route scoring
  - Forwarding decision engine
  - Dedup + TTL enforcement
- `messaging`
  - Conversation model
  - Outbox/inbox processor
  - Delivery state machine
- `storage`
  - Room entities: Peer, Message, Envelope, RouteHint

## 2) Message flow

1. App discovers nearby peers via BLE.
2. For a send action, routing chooses direct or relay path.
3. Envelope is persisted before send (outbox durable queue).
4. Neighbor receives envelope:
   - If destination == self => decrypt + deliver + ACK.
   - Else if TTL > 0 and unseen => decrement TTL and forward.
5. ACK updates delivery status and halts retries.

## 3) Routing strategy (simple and robust)

Use a pragmatic score-based forwarding approach instead of heavy ad-hoc protocols:

`score = recency_weight + link_quality_weight + historical_delivery_weight`

Forward to top `k` neighbors (k=1 or 2) while avoiding loops through:

- Envelope ID dedupe cache
- Previous-hop exclusion
- TTL/hop cap (e.g., 6)

## 4) Reliability

- At-least-once forwarding semantics
- Duplicate suppression at each node
- Retry with exponential backoff
- Tombstones for delivered/expired envelopes

## 5) Android constraints to design around

- Background limits require foreground service for continuous discovery/relay.
- BLE scan policies vary by OEM; test Samsung/Xiaomi/Pixel separately.
- Wi‑Fi Direct setup latency can be high; keep BLE control channel active.

## 6) Suggested tech stack

- Kotlin + Coroutines/Flow
- Room
- WorkManager (deferred retries when app process is killed)
- Foreground Service for active mesh mode

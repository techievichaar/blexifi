# OEM Validation Matrix (Production Readiness)

This matrix is used to complete field-validation rollout tasks.

## Devices

- Pixel (Android 14)
- Samsung OneUI (Android 13/14)
- Xiaomi MIUI/HyperOS (Android 13/14)
- OnePlus/Oppo (Android 13/14)

## Test cases

1. **Direct in-range chat**
   - A <-> B online in same room.
   - Verify send/receive latency, ACK success, retry count.
2. **Multi-hop relay**
   - A -> C -> D -> B, with A and B out of range.
   - Verify message + ACK propagate through relay.
3. **Background reliability**
   - App in background for 60+ minutes.
   - Verify foreground service stays alive and retry worker runs.
4. **Reboot recovery**
   - Reboot phone.
   - Verify Boot receiver restarts service and pending outbox resumes.
5. **Battery optimization behavior**
   - Validate prompt path and behavior after user allow/deny.
6. **Soak test**
   - 2-hour run with periodic sends every 30s.
   - Capture delivery ratio and median/95p latency.

## Exit criteria

- Delivery success ratio >= 98% in direct mode.
- Delivery success ratio >= 95% in 2-hop/3-hop mode.
- No crash loops or service death loops in 2-hour soak.
- Retry queue does not grow unbounded.

---
icon: material/timer
---

# Requests

> For: intermediate admins tuning requests.

```yaml
tpa:
  requestDuration: 60
  concurrency:
    allowConcurrent: true
    maxConcurrent: 3
    cancelExistingOnAccept: true
  receiverFeedback:
    message: true
    actionbar: true
  senderFeedback:
    message: true
    actionbar: true
  rejectFeedback:
    message: true
    actionbar: true
  timeoutFeedback:
    message: true
    actionbar: true
  cooldown:
    enabled: true
    defaultSeconds: 15
    metaKey: juicytpa-cooldown
```

- `requestDuration` » how long a request stays pending before it times
  out, in seconds. Shown to both parties as a compact duration (`48s`,
  `1m 30s`).
- `concurrency.allowConcurrent` » let players hold several pending
  requests at once (off means one outgoing at a time).
- `concurrency.maxConcurrent` » maximum outgoing requests a player may
  hold at once (zero or below means unlimited). `/tpall` bypasses the
  cap and the toggle entirely.
- `concurrency.cancelExistingOnAccept` » accepting one request cancels
  the receiver's other pending requests. Only applies with
  `allowConcurrent`.
- `receiverFeedback` / `senderFeedback` / `rejectFeedback` /
  `timeoutFeedback` » `message` toggles the chat message for that event,
  `actionbar` toggles the actionbar. Request, deny and timeout events
  each notify both parties; accepts always notify.
- `cooldown` » `enabled` gates a cooldown between `/tpa` and `/tpahere`
  sends (bulk sends are exempt). Each player waits their LuckPerms
  `metaKey` value in seconds, or `defaultSeconds` without (or with
  unparseable) meta; zero or below means no cooldown. LuckPerms is
  optional: without it everyone uses the default. See
  [LuckPerms](../advanced/luckperms.md) for the rank-perk setup.

---
icon: material/inbox
---

# Receiving

> For: everyone tuning how they receive requests.

**Modes** (`/tpamode`, bare cycles, `/tpa` only): Normal receives
requests as usual; Block All refuses them at once (the sender is
told); Accept All teleports at once. Modes persist across restarts;
`/tpahere` always goes through.

**Concurrency** on (default) means up to `maxConcurrent` (3)
outgoing requests at a time (`≤0` unlimited); off means one.
Accepting one then cancels the receiver's other pending requests
(toggleable). `/tpall` bypasses all of this. See
[Configuration](../configuration/requests.md).

**Blocking** (`/tpablock`) toggles a persistent per-player block: a
blocked sender's requests always fail, whatever the receiver's mode.
Blocking drops the sender's pending requests to the blocker.

**Warnings** flag receivers that let requests expire (AFK) or deny
them often: senders must re-send within 15 seconds to confirm, and
repeat offenders get a nudge with a one-click Block All (or Block
player, when one sender spams) button. Thresholds live under
`tpa.warnings` (see [Warnings](../configuration/warnings.md)) and
long-term counters persist in the database.

**Everyone** (`/tpall`, OP) sends a plain request to every online
player. It bypasses concurrency entirely (toggle, cap and warning
confirmations never apply); the sender, duplicates, blocked pairs
and block-all receivers are skipped. Holders of `juicytpa.tpall` may
also use `@a` in `/tpa` and `/tpahere`; without the permission `@a`
is hidden from completion and rejected.

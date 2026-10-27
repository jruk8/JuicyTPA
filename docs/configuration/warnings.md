---
icon: material/alert
---

# Warnings

> For: intermediate admins tuning warnings.

```yaml
tpa:
  warnings:
    afkMinUnanswered: 3
    afkMinPendingSeconds: 180
    cancellerMinDenied: 3
    cancellerMinPendingSeconds: 75
    confirmWindowSeconds: 15
    nudgeCooldownSeconds: 300
    nudgeMinCancelStreak: 3
    nudgeMinExpiryStreak: 3
    harassMinRequests: 3
    harassWindowSeconds: 600
```

A receiver counts as AFK past `afkMinUnanswered` expiries and
`afkMinPendingSeconds` total pending seconds (overlapping requests
count once per second), and as a canceller past
`cancellerMinDenied` denies and `cancellerMinPendingSeconds`
denied-pending seconds. Senders warned about such a receiver must
re-send within `confirmWindowSeconds` to confirm. Receivers get at
most one nudge per `nudgeCooldownSeconds`: after
`nudgeMinCancelStreak` straight denies or `nudgeMinExpiryStreak`
straight expiries (any accept resets both) the nudge offers one-click
Block All; after `harassMinRequests` requests from one sender within
`harassWindowSeconds` it offers Block player. `/tpall` skips warning
confirmations.

Long-term counters persist in the database (`jtpa_warn_unanswered`,
`jtpa_warn_pending_seconds`, `jtpa_warn_denied`,
`jtpa_warn_denied_seconds`); streaks, confirmations and throttles are
in-memory. The sender warnings and nudges are `messages.yml` keys
(`warnAfk`, `warnCanceller`, `nudgeCancel`, `nudgeAfk`,
`nudgeHarass` plus the button texts), so their wording stays
editable without touching thresholds.

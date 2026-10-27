---
icon: material/variable
---

# Placeholders

PlaceholderAPI support is handled by the `core.placeholders` module. When
PlaceholderAPI is installed, the plugin registers an expansion with the
identifier `jtpa`. The expansion is re-registered on
`/juicytpa reload`, and unregistered when the plugin disables.

## JuicyTPA Placeholders

| Placeholder | Type | Description |
| --- | --- | --- |
| `%jtpa_sent%` | INTEGER | TP requests you have sent. |
| `%jtpa_sent_<player>%` | INTEGER | Another player's sent TP requests. |
| `%jtpa_sent_global%` | INTEGER | Server-wide sent TP requests. |
| `%jtpa_received%` | INTEGER | TP requests you have received. |
| `%jtpa_received_<player>%` | INTEGER | Another player's received TP requests. |
| `%jtpa_received_global%` | INTEGER | Server-wide received TP requests. |
| `%jtpa_mode%` | STRING | Your TPA mode display name. |
| `%jtpa_mode_<player>%` | STRING | Another player's TPA mode display name. |
| `%jtpa_pending%` | STRING | Most recent player with a request pending you (`---` when none). |

A send is recorded whenever a request is delivered (auto-accepted
requests count; blocked ones never exist). Counts are stored in the SQLite
database per player and globally, so they survive restarts. Unknown
player names read as `0`.

## Adding Your Own Placeholders

Track new state with `TpaUsageTracker` (one stat key per counter,
recorded from the command) and expose it in `TpaExpansion.onRequest()`:

```java
if ("my_stat".equals(params)) {
    return counterFor(player, "my_stat", params, tracker::playerMyStat, tracker.globalMyStat());
}
```

Provide the value through an injected service and store it in SQLite if it
should survive restarts. Registered placeholders are announced to
PlaceholderAPI via `getPlaceholders()`. A full walkthrough (including new
stat keys) lives in [api.md](api.md).

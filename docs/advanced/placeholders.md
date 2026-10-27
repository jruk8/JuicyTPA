---
icon: material/variable
---

# Placeholders

> For: advanced users showing stats.

When PlaceholderAPI is installed, the plugin registers an expansion with the
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

Adding your own placeholders is covered in the
[Plugin API](api.md).

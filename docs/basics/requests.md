---
icon: material/send
---

# Requests

> For: everyone sending and answering requests.

Requests expire after `requestDuration` seconds. The receiver gets
a chat notice with clickable [Accept]/[Reject] prompts (linked to
`/tpaccept` and `/tpadeny` for that sender) plus an actionbar; the
sender gets a confirmation plus an actionbar. Each side's chat and
actionbar can be toggled separately (see
[Configuration](../configuration/requests.md)). Bare `/tpa`,
`/tpahere` and `/tpablock` show a usage hint instead of running.

Answering bare (`/tpaccept`, `/tpadeny`) answers the single pending
request; with several pending (concurrency on), name the player.
`/tpacancel` mirrors this for outgoing requests.

Cooldowns (on, 15 seconds by default) limit how often a player may
send `/tpa` and `/tpahere`; `/tpall` is exempt. Servers with
LuckPerms can set a per-player value (see
[LuckPerms](../advanced/luckperms.md)), which makes shorter cooldowns
a natural rank perk.

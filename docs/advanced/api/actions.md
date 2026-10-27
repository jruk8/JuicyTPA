---
icon: material/cog
---

# Actions

> For: developers driving requests from code.

`JuicyTPA` is the service interface, registered with the Bukkit
services manager on enable and backed by the live request service,
so queries always see current state. Look it up as shown on the
[overview](index.md).

## Queries

| Method | Returns |
| --- | --- |
| `incoming(receiver)` | Pending incoming requests, oldest first |
| `outgoing(sender)` | Pending outgoing requests, oldest first |
| `mode(player)` | The player's `TpaMode` (`NORMAL`, `BLOCK_ALL`, `ACCEPT_ALL`) |
| `isBlocked(owner, player)` | Whether the owner blocked the player |
| `isTrusted(owner, player)` | Whether the owner trusts the player's TP asks |

## Actions

Each action runs exactly the flow its command would, including
events, messages, sounds, and sweeps:

| Method | Behaves like | Returns false when |
| --- | --- | --- |
| `accept(receiver, sender)` | `/tpaccept <sender>` | No match, either party offline, or the accept event was vetoed (the request stays pending) |
| `deny(receiver, sender)` | `/tpadeny <sender>` | No match or the receiver is offline |
| `cancel(sender, receiver)` | `/tpacancel <receiver>` | No match or the sender is offline |

Actions resolve the latest pending request between the pair, so
they never need a request id.

## Threading

Call every method from the server thread. Event listeners and
Brigadier executors already run there; if you come from async code,
schedule onto the server thread first. Teleports stay Folia-safe
either way: they run on the mover's entity scheduler.

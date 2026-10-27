---
icon: material/lightning-bolt
---

# Events

> For: developers listening to request events.

All events extend `TpaRequestEvent`, which carries `getRequest()`
(the immutable `TpaRequest`: sender and receiver UUIDs and names,
the `here` direction flag, and the creation time) plus
`getSender()` and `getReceiver()` players. Either player is null
when they went offline before the event fired.

| Event | When | Cancellable |
| --- | --- | --- |
| `TpaRequestSendEvent` | Internal checks passed, before the request is stored or announced | Yes |
| `TpaRequestAcceptEvent` | A pending request is accepted, before the teleport runs | Yes |
| `TpaRequestDenyEvent` | A request was denied with `/tpadeny` | No |
| `TpaRequestCancelEvent` | A request was removed without a teleport or deny | No |
| `TpaRequestExpireEvent` | A request timed out unanswered | No |

## Ordering

A request that lives its full life fires `Send`, then exactly one
of `Accept`, `Deny`, `Cancel`, or `Expire`. Instant accepts
(accept-all mode and trusted asks) fire `Send` followed at once by
`Accept`; the request is never stored in between.

`/tpall` and `@a` fire one `Send` per delivered target; vetoing one
target skips only that target. Silent removals (a block dropping
incoming requests, or an accept sweeping the receiver's other
requests) fire `Cancel` with a cause instead of staying silent:

| `CancelEvent.Cause` | Meaning |
| --- | --- |
| `CANCEL_COMMAND` | The sender ran `/tpacancel` |
| `ACCEPT_SWEEP` | The receiver accepted another request with sweeps on |
| `BLOCK` | The receiver blocked the sender |

## Vetoes

The two cancellable events carry an optional Adventure
`denyMessage`. Cancel the event to veto the action:

- **Send veto** » the request is never stored or announced, no
  cooldown starts, and the sender sees the deny message (or the
  configured `externalDenied` message when none is set).
- **Accept veto** » a manual request stays pending and the accepter
  is told; an instant accept is dropped and the sender is told.

Notification events (`Deny`, `Cancel`, `Expire`) fire after the
request is already gone, so they cannot be vetoed.

---
icon: material/key
---

# Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `juicytpa.tpa` | Send `/tpa` requests. | Everyone |
| `juicytpa.tpahere` | Send `/tpahere` requests. | Everyone |
| `juicytpa.accept` | Accept requests with `/tpaccept`. | Everyone |
| `juicytpa.deny` | Deny requests with `/tpadeny`. | Everyone |
| `juicytpa.cancel` | Cancel requests with `/tpacancel`. | Everyone |
| `juicytpa.mode` | Change your mode with `/tpamode`. | Everyone |
| `juicytpa.reload` | Use `/juicytpa reload`. | OP |
| `juicytpa.help` | Use `/juicytpa help`. | Everyone |
| `juicytpa.version` | Use `/juicytpa version`. | Everyone |

Permissions are checked on the command nodes with Brigadier `requires()`
predicates (see `CommandContext.requiresPermission`); the node constants
live in `TpaPermissions`. Every TPA command targets other players by
design, so each command has its own node instead of a shared scope. See
[commands.md](commands.md).

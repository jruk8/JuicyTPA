---
icon: material/console
---

# Commands

> For: everyone running TPA commands.

Every TPA command is a standalone root (plus a `/juicytpa` subcommand)
by default; disable a root in `commands` to keep only the hub form (see
[Configuration](../configuration.md)). Requests are strictly one-to-one:
a selector must resolve to exactly one player, and console cannot send or
answer requests. A bare `@r` never picks the sender while others are
online; `@s` and `@p` are not supported.

| Command | Description | Permission |
| --- | --- | --- |
| `/tpa <player>` | Ask to teleport to a player. | `juicytpa.tpa` |
| `/tpahere <player>` | Ask a player to teleport to you. | `juicytpa.tpahere` |
| `/tpaccept [player]` | Accept a TP request. | `juicytpa.accept` |
| `/tpadeny [player]` | Deny a TP request. | `juicytpa.deny` |
| `/tpacancel [player]` | Cancel an outgoing TP request. | `juicytpa.cancel` |
| `/tpablock <player>` | Block or unblock a player's requests. | `juicytpa.block` |
| `/tpall` | Request everyone at once. | `juicytpa.tpall` |
| `/tpamode [mode]` | Cycle or set your TPA mode. | `juicytpa.mode` |
| `/juicytpa <help\|version\|reload>` | Help, version, and config reload (aliases `/jtpa`, `/jtp`, `/jt`). | — |

Default root aliases: `/tpask` for `/tpa`; `/tpah`, `/tphere`, `/tph`
for `/tpahere`; `/tpacc` for `/tpaccept`; `/tpad`, `/tpdeny`,
`/tpd` for `/tpadeny`; `/tpac`, `/tpcancel`, `/tpc` for `/tpacancel`;
`/tpam`, `/tpmode`, `/tpm` for `/tpamode`; `/tpab`, `/tpblock`,
`/tpb` for `/tpablock`.

How requests flow is on the [Requests](requests.md) page; modes,
blocks and warnings live under [Receiving](receiving.md).

- **`help`** supports an optional query argument that filters the listed
  commands (`/juicytpa help reload`). Entries are filtered by permission,
  so every sender only sees what they may use.
- **`version`** prints the plugin's version and author list with links,
  and repeats a pending update notice to admins.
- **`reload`** re-reads `config.yml`, `messages.yml` and `sounds.yml`, then
  reopens the SQLite database and re-registers the placeholders with the new
  settings. Changed root labels need a restart; `reload` says so when it
  detects a change.

See [Permissions](../advanced/permissions.md) for defaults.

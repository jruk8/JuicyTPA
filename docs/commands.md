---
icon: material/console
---

# Commands

Every TPA command is a standalone root (plus a `/juicytpa` subcommand)
by default; disable a root in `commands` to keep only the hub form (see
[configuration.md](configuration.md)). Requests are strictly one-to-one:
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

- **Requests** expire after `requestDuration` seconds. The receiver gets
  a chat notice with clickable [Accept]/[Reject] prompts (linked to
  `/tpaccept` and `/tpadeny` for that sender) plus an actionbar; the
  sender gets a confirmation plus an actionbar. Each side's chat and
  actionbar can be toggled separately.
- **Answering** bare (`/tpaccept`, `/tpadeny`) answers the single pending
  request; with several pending (concurrency on), name the player.
  `/tpacancel` mirrors this for outgoing requests.
- **Modes** (`/tpamode`, bare cycles, `/tpa` only): Normal receives
  requests as usual; Block All refuses them at once (the sender is
  told); Accept All teleports at once. Modes persist across restarts;
  `/tpahere` always goes through.
- **Concurrency** on (default) means up to `maxConcurrent` (3)
  outgoing requests at a time (`≤0` unlimited); off means one.
  Accepting one then cancels the receiver's other pending requests
  (toggleable). `/tpall` bypasses all of this.
- **Blocking** (`/tpablock`) toggles a persistent per-player block: a
  blocked sender's requests always fail, whatever the receiver's mode.
  Blocking drops the sender's pending requests to the blocker.
- **Warnings** flag receivers that let requests expire (AFK) or deny
  them often: senders must re-send within 15 seconds to confirm, and
  repeat offenders get a nudge with a one-click Block All (or Block
  player, when one sender spams) button. Thresholds live under
  `tpa.warnings` and long-term counters persist in the database.
- **Everyone** (`/tpall`, OP) sends a plain request to every online
  player. It bypasses concurrency entirely (toggle, cap and warning
  confirmations never apply); the sender, duplicates, blocked pairs
  and block-all receivers are skipped. Holders of `juicytpa.tpall` may
  also use `@a` in `/tpa` and `/tpahere`; without the permission `@a`
  is hidden from completion and rejected.
- **`help`** supports an optional query argument that filters the listed
  commands (`/juicytpa help reload`). Entries are filtered by permission,
  so every sender only sees what they may use.
- **`version`** prints the plugin's version and author list with links,
  and repeats a pending update notice to admins.
- **`reload`** re-reads `config.yml`, `messages.yml` and `sounds.yml`, then
  reopens the SQLite database and re-registers the placeholders with the new
  settings. Changed root labels need a restart; `reload` says so when it
  detects a change.

See [permissions.md](permissions.md) for defaults.

## Adding a Command

1. Create a command class in a new subpackage of `core.commands` (e.g.
   `core.commands.user.fun`) that builds a Brigadier node with
   `Commands.literal(...)`, gates it with `requires()`, and runs its logic
   in `executes(...)`.
2. Attach the node to the root in the matching registrar
   (`UserCommandsRegistrar` or `AdminCommandsRegistrar`) with
   `root.then(new MyCommand(...).node())`, injecting only the services it
   needs (commonly `CommandContext`).
3. Declare the permission in `paper-plugin.yml` (with a constant in
   `TpaPermissions`), document it in [permissions.md](permissions.md)
   and add a `HelpEntry` so the command shows up in `/juicytpa help`.

See [api.md](api.md) for the full walkthrough.

<div align="center">

![JuicyTPA banner](https://raw.githubusercontent.com/jruk8/JuicyTPA/main/docs/assets/banner-1280x640.png)

<h3>A modern 26.x plugin for /tpa and /tpahere.</h3>

### [Modrinth](https://modrinth.com/project/juicytpa) · [Wiki](https://jruk8.github.io/JuicyTPA/) · [GitHub](https://github.com/jruk8/JuicyTPA)

</div>

# Quick Start 🌀

1. Drop the jar into `plugins/` and restart the server.
2. Run **`/tpa <player>`**.
3. They click **[Accept]** (or run **`/tpaccept`**).

**That's it!** Requests expire automatically, and `/tpamode`
toggles how you receive them.

**Next:** see config or run `/jtpa help`.

# Features

- **Teleport requests** » `/tpa` to go to them, `/tpahere` to summon
  them, with clickable accept/deny prompts.
- **Answer your way** » `/tpaccept`, `/tpadeny` and `/tpacancel`, with
  or without naming the player.
- **TPA modes** » `/tpamode` cycles Normal, Block all and Accept all;
  your mode survives restarts.
- **Full configurability** » expiry time, concurrency, chat/actionbar
  feedback per event, aliases off or on, permissions.
- **Also for serious deployment** » MiniMessage, sounds, PAPI placeholders
  (`%jtpa_sent%`, `%jtpa_received%`, `%jtpa_mode%` and variants),
  and an SQLite store for stats and modes.

### Requirements

- Server Software » [Paper 26.2+](https://papermc.io/downloads/paper) (**Folia🍃** supported)
- Java Version » **Java 25**
- Dependencies » **None** (PlaceholderAPI is optional, only for the
  `%jtpa_...%` placeholders)

### Commands

| Command | Description |
| --- | --- |
| `/tpa <player>` | Ask to teleport to a player. |
| `/tpahere <player>` | Ask a player to teleport to you. |
| `/tpaccept [player]` | Accept a TP request. |
| `/tpadeny [player]` | Deny a TP request. |
| `/tpacancel [player]` | Cancel an outgoing TP request. |
| `/tpablock <player>` | Block or unblock a player's requests. |
| `/tpall` | Request everyone at once. |
| `/tpamode [mode]` | Cycle or set your TPA mode. |
| `/juicytpa <help\\|version\\|reload>` | Help, version, and config reload (aliases `/jtpa`, `/jtp`, `/jt`). |

See the full [Commands Page](https://jruk8.github.io/JuicyTPA/commands/) for more info.

### Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `juicytpa.tpa` | Send `/tpa` requests. | Everyone |
| `juicytpa.tpahere` | Send `/tpahere` requests. | Everyone |
| `juicytpa.accept` | Accept requests with `/tpaccept`. | Everyone |
| `juicytpa.deny` | Deny requests with `/tpadeny`. | Everyone |
| `juicytpa.cancel` | Cancel requests with `/tpacancel`. | Everyone |
| `juicytpa.mode` | Change your mode with `/tpamode`. | Everyone |
| `juicytpa.block` | Block requests with `/tpablock`. | Everyone |
| `juicytpa.tpall` | Request everyone with `/tpall`, and use `@a`. | OP |
| `juicytpa.reload` | Use `/juicytpa reload`. | OP |
| `juicytpa.help` | Use `/juicytpa help`. | Everyone |
| `juicytpa.version` | Use `/juicytpa version`. | Everyone |

### Config

Find `config.yml`, `messages.yml` and `sounds.yml` in `plugins/JuicyTPA/`:

- `updateChecker` » checks for updates.
- `aliases` » aliases for `/jtpa` root command (needs **R**estart).
- `storage` » SQLite DB (stats plus TPA modes).
- `tpa` » expiry, concurrency, feedback toggles.
- `commands` » /tpa, /tpahere, ... aliases (**R**).
- `colors` » color palette for messages.
- `metrics` » anonymous bStats stats (**R**).
- `messages.yml` / `sounds.yml` » messages and sounds.

See the full [Configuration Page](https://jruk8.github.io/JuicyTPA/configuration/) for more info.

# Links

- [Documentation](https://jruk8.github.io/JuicyTPA/)
- [Discord](https://discord.com/invite/hkWmCVmWDC)
- [GitHub](https://github.com/jruk8/JuicyTPA)

### Donate ☕

If you like my plugins, you can support my work through a [**small donation**](https://ko-fi.com/jruk). **Thanks!** 👐

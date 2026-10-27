---
icon: material/cog
---

# Configuration

> For: intermediate admins. Start at [Requests](configuration/requests.md).

The plugin writes `config.yml`, `messages.yml` and `sounds.yml` into its data
folder (`plugins/JuicyTPA/`) on first start. Edit a file and run
`/juicytpa reload` to apply the changes without restarting the server
(command registration is the exception; see `commands` below). Missing
keys are re-added with defaults; unknown keys are removed.

See the individual configuration pages for detailed documentation:

## Sections

- [Requests](configuration/requests.md): expiry, concurrency,
  feedback toggles, cooldowns and warnings.
- [Warnings](configuration/warnings.md): AFK and canceller thresholds,
  resend confirmations, nudges and harassment detection.
- [Messages](configuration/messages.md): the `messages.yml` keys, the
  color palette and the available placeholders.
- [Sounds](configuration/sounds.md): the `sounds.yml` entries for every
  request event.

## config.yml

```yaml
updateChecker:
  enabled: true
aliases:
  - jtpa
  - jtp
  - jt
defaultLanguage: auto
storage:
  dbFile: juicytpa.db
tpa:
  # (see the Requests page)
commands:
  tpa:
    enabled: true
    aliases:
      - tpask
  tpahere:
    enabled: true
    aliases:
      - tpah
      - tphere
      - tph
  tpaccept:
    enabled: true
    aliases:
      - tpacc
  tpadeny:
    enabled: true
    aliases:
      - tpad
      - tpdeny
      - tpd
  tpacancel:
    enabled: true
    aliases:
      - tpac
      - tpcancel
      - tpc
  tpalist:
    enabled: true
    aliases:
      - tpal
      - tplist
      - tpl
  tpatrust:
    enabled: true
    aliases:
      - tpat
      - tptrust
      - tpt
  tpamode:
    enabled: true
    aliases:
      - tpam
      - tpmode
      - tpm
      - tpatoggle
      - tptoggle
  tpablock:
    enabled: true
    aliases:
      - tpab
      - tpblock
      - tpb
      - tpaignore
      - tpai
      - tpignore
      - tpi
  tpacceptall:
    enabled: true
    aliases:
      - tpaauto
      - tpauto
  tpablockall:
    enabled: true
    aliases:
      - tpaba
      - tpblockall
      - tpba
  tpalanguage:
    enabled: true
    aliases:
      - tpalang
  tpall:
    enabled: true
    aliases: []
colors:
  plainColor: '<#b9e0b8>'
  brandColor: '<#87db84>'
  highlightColor: '<#ebf2eb>'
  errorColor: '<#eb5757>'
  acceptColor: '<#7be63e>'
  rejectColor: '<#e63e5a>'
  sentColor: '<#44e63e>'
  lhColor: '<#bababa>'
  warnColor: '<#e6db3e>'
metrics:
  enabled: true
```

### `updateChecker`

- `enabled` » checks GitHub releases for new versions on enable and on
  reload. The check runs asynchronously; findings go to the console and
  in-game to players holding `juicytpa.reload` (on join and via
  `/juicytpa version`). Update notices link both the GitHub release and
  the Modrinth project page.

### `aliases`

Extra labels for the `/juicytpa` hub command (`/jtpa`, `/jtp` and `/jt`
work out of the box). Running the hub with no arguments shows the help
listing. Blanks, duplicates and the `juicytpa` label itself are ignored.
Applies on restart; `/juicytpa reload` reports when a restart is needed.

### `defaultLanguage`

Default language for players without a `/tpalanguage` pin: `auto`
follows each player's client locale, any other
[language key](configuration/messages.md#languages) forces that
language. Pins always win; unknown keys warn and behave as `auto`.
Takes effect on reload. Revoke `juicytpa.language` to strictly force
one language server-wide.

### `storage`

- `dbFile` » name of the SQLite database file created in the plugin's data
  folder. The database holds request counters (tables `player_stats` and
  `global_stats`), each player's TPA mode (key `jtpa_mode`), language pin
  (key `jtpa_lang`, unset while following the client) and the warning
  statistics (`jtpa_warn_unanswered`, `jtpa_warn_pending_seconds`,
  `jtpa_warn_denied`, `jtpa_warn_denied_seconds`).

### `commands`

Each channel has an `enabled` toggle registering (or hiding) its
standalone root command (`tpa`, `tpahere`, `tpaccept`, `tpadeny`,
`tpacancel`, `tpamode`, `tpablock`, `tpall`, `tpalist`, `tpatrust`,
`tpacceptall`, `tpablockall`, `tpalanguage`), plus
`aliases` » extra labels for that root.
The `/juicytpa` subcommands always exist. Aliases colliding with a hub
label, a subcommand or another alias are ignored. Changes take effect
after a restart; `/juicytpa reload` reports when a restart is needed.
Every root and alias also registers under the `jtpa:` namespace
(`/jtpa:tpa`, `/jtpa:tpask`), so there is always an unambiguous strict
ref even when another plugin claims a label.

### `colors`

The message color palette. Every `messages.yml` entry uses these via
`{plain}`, `{brand}`, `{hl}`, `{error}`, `{ac}`, `{rc}`, `{sc}`,
`{lh}` and `{wn}`, resolved when the message is rendered; editing a
color and reloading is enough, the messages themselves never hardcode
colors.

### `metrics`

- `enabled` » sends anonymous usage statistics via bStats (open-source,
  no personal data or IP addresses). Read once at startup: disabling it
  needs a server restart to take effect.

---
icon: material/tune
---

# Configuration

The plugin writes `config.yml`, `messages.yml` and `sounds.yml` into its data
folder (`plugins/JuicyTPA/`) on first start. Edit a file and run
`/juicytpa reload` to apply the changes without restarting the server
(command registration is the exception; see `commands` below). Missing
keys are re-added with defaults; unknown keys are removed.

## config.yml

```yaml
updateChecker:
  enabled: true
aliases:
  - jtpa
  - jtp
  - jt
storage:
  dbFile: juicytpa.db
tpa:
  requestDuration: 60
  concurrency:
    allowConcurrent: true
    maxConcurrent: 3
    cancelExistingOnAccept: true
  receiverFeedback:
    message: true
    actionbar: true
  senderFeedback:
    message: true
    actionbar: true
  rejectFeedback:
    message: true
    actionbar: true
  timeoutFeedback:
    message: true
    actionbar: true
  warnings:
    afkMinUnanswered: 3
    afkMinPendingSeconds: 180
    cancellerMinDenied: 3
    cancellerMinPendingSeconds: 75
    confirmWindowSeconds: 15
    nudgeCooldownSeconds: 300
    nudgeMinCancelStreak: 3
    nudgeMinExpiryStreak: 3
    harassMinRequests: 3
    harassWindowSeconds: 600
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
  tpamode:
    enabled: true
    aliases:
      - tpam
      - tpmode
      - tpm
  tpablock:
    enabled: true
    aliases:
      - tpab
      - tpblock
      - tpb
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

### `storage`

- `dbFile` » name of the SQLite database file created in the plugin's data
  folder. The database holds request counters (tables `player_stats` and
  `global_stats`), each player's TPA mode (key `jtpa_mode`) and the
  warning statistics (`jtpa_warn_unanswered`, `jtpa_warn_pending_seconds`,
  `jtpa_warn_denied`, `jtpa_warn_denied_seconds`).

### `tpa`

- `requestDuration` » how long a request stays pending before it times
  out, in seconds. Shown to both parties as a compact duration (`48s`,
  `1m 30s`).
- `concurrency.allowConcurrent` » let players hold several pending
  requests at once (off means one outgoing at a time).
- `concurrency.maxConcurrent` » maximum outgoing requests a player may
  hold at once (zero or below means unlimited). `/tpall` bypasses the
  cap and the toggle entirely.
- `concurrency.cancelExistingOnAccept` » accepting one request cancels
  the receiver's other pending requests. Only applies with
  `allowConcurrent`.
- `receiverFeedback` / `senderFeedback` / `rejectFeedback` /
  `timeoutFeedback` » `message` toggles the chat message for that event,
  `actionbar` toggles the actionbar. Request, deny and timeout events
  each notify both parties; accepts always notify.
- `warnings` » AFK and canceller thresholds: a receiver counts as AFK
  past `afkMinUnanswered` expiries and `afkMinPendingSeconds` total
  pending seconds (overlapping requests count once per second), and
  as a canceller past `cancellerMinDenied` denies and
  `cancellerMinPendingSeconds` denied-pending seconds. Senders warned
  about such a receiver must re-send within `confirmWindowSeconds` to
  confirm. Receivers get at most one nudge per `nudgeCooldownSeconds`:
  after `nudgeMinCancelStreak` straight denies or
  `nudgeMinExpiryStreak` straight expiries (any accept resets both) the
  nudge offers one-click Block All; after `harassMinRequests` requests
  from one sender within `harassWindowSeconds` it offers Block player.
  `/tpall` skips warning confirmations.

### `commands`

Each channel has an `enabled` toggle registering (or hiding) its
standalone root command (`tpa`, `tpahere`, `tpaccept`, `tpadeny`,
`tpacancel`, `tpamode`, `tpablock`, `tpall`), plus `aliases` » extra
labels for that root.
The `/juicytpa` subcommands always exist. Aliases colliding with a hub
label, a subcommand or another alias are ignored. Changes take effect
after a restart; `/juicytpa reload` reports when a restart is needed.

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

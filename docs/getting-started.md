---
icon: material/rocket-launch
---

![Banner](assets/banner-1280x640.png)
# Getting Started

## Requirements

- Paper 26.2 or newer (Folia supported)
- Java 25

## Installation

1. Download a release jar (or build one: `./gradlew build`).
2. Place the jar in your server's `plugins/` folder.
3. Restart the server. The plugin generates `config.yml`, `messages.yml` and
   `sounds.yml` in `plugins/JuicyTPA/` and creates the SQLite database
   `juicytpa.db`.
4. (Optional) Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
   to use the placeholders.

## Your First Commands

```text
/tpa Alex
/tpaccept
/tpamode
```

`/jtpa` is the main alias for `/juicytpa`. The placeholders count your
requests: run a few commands, install PlaceholderAPI, and try
`%jtpa_sent%` in a scoreboard or chat.

## Next Steps

- [Commands](commands.md)
- [Configuration](configuration.md)
- [Placeholders](placeholders.md)

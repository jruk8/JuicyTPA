---
icon: material/home
---

![Banner](assets/banner-1280x640.png)
# JuicyTPA

A modern 26.x plugin for `/tpa` and `/tpahere`: teleport requests with
clickable accept/deny prompts, per-player modes, Folia-safe teleports
and an SQLite store for stats and modes.

## Key Features

- **Requests** » `/tpa` to go to them, `/tpahere` to summon them, with
  chat prompts plus actionbar nudges.
- **Modes** » Normal, Block all and Accept all per player, persisted in
  `juicytpa.db` and cycled with a bare `/tpamode`.
- **Concurrency** » one pending request at a time by default, or many
  with an accept-sweeps-the-rest option.
- **Placeholders** » `%jtpa_sent%`, `%jtpa_received%` and `%jtpa_mode%`
  with per-player and global variants.
- **Update checker** » async GitHub releases check with console and
  in-game notices.
- **Reloadable** » `/juicytpa reload` re-reads every config and re-opens
  the storage and placeholders.

## Documentation

- [Getting Started](getting-started.md)
- [Commands](commands.md)
- [Permissions](permissions.md)
- [Configuration](configuration.md)
- [Placeholders](placeholders.md)
- [Plugin API](api.md)

## Build

Java 25 is required.

```text
./gradlew build
```

The plugin jar is written to `build/libs/`.

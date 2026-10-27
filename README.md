<div align="center">

![JTemplate banner](https://raw.githubusercontent.com/jruk8/JPaperTemplate/main/docs/assets/banner-1280x640.png)

<h3>A clean starting point for modern Paper plugins.</h3>

### [Modrinth](https://modrinth.com/project/jtemplate) · [Wiki](https://jruk8.github.io/JPaperTemplate/) · [GitHub](https://github.com/jruk8/JPaperTemplate)

</div>

# Quick Start 🏃

1. Find & replace `jtemplate` → *yourname* and `JTemplate` → *YourName*
   across the repo (packages, commands, placeholders, database, docs).
2. Update `gradle.properties` (`pluginName`, `pluginMain`, `pluginAuthor`,
   `pluginDescription`, `pluginWebsite`) and `rootProject.name` in
   `settings.gradle`.
3. Build the jar and drop it into `plugins/`.

**That's it!** You have a working plugin with Brigadier commands,
MiniMessage messages, sounds, PAPI placeholders, SQLite storage and a
GitHub update checker.

**Next:** delete the modules you don't need, then add your business
logic on top.

# Features

- **Brigadier commands** » Paper-native nodes with permission gates,
  config-driven root aliases, and a bare-root help listing.
- **Selectors ready** » `TargetSelector` plus selector/value tab
  completion out of the box.
- **MiniMessage everywhere** » `{braces}` for plugin placeholders,
  angle brackets reserved for raw MiniMessage, shared color palette.
- **Sounds** » config-driven Adventure sounds per feedback event.
- **Placeholders** » a PAPI expansion with self/other variants
  (`%jtemplate_uses%`, `%jtemplate_uses_<player>%`).
- **Update checker** » async GitHub releases check (console log, join
  notice, `/version` line).
- **Reloadable** » `/jtemplate reload` re-reads every config and
  re-opens storage and placeholders without a restart.

### Requirements

- Server Software » [Paper 26.2+](https://papermc.io/downloads/paper) (**Folia🍃** supported)
- Java Version » **Java 25**
- Dependencies » **None** (PlaceholderAPI is optional, only for the
  `%jtemplate_...%` placeholders)

### Commands

| Command | Description |
| --- | --- |
| `/jtemplate <help\|version\|reload>` | Help, version, and config reload (alias `/jt`). |

See the full [Commands Page](https://jruk8.github.io/JPaperTemplate/commands/) for more info.

### Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `jtemplate.other` | Target other players (needs the matching command permission too). | OP |
| `jtemplate.reload` | Use `/jtemplate reload`. | OP |
| `jtemplate.help` | Use `/jtemplate help`. | Everyone |
| `jtemplate.version` | Use `/jtemplate version`. | Everyone |

### Config

Find `config.yml`, `messages.yml` and `sounds.yml` in `plugins/JTemplate/`:

- `updateChecker` » checks for updates.
- `aliases` » aliases for `/jt` root command (needs **R**estart).
- `storage` » SQLite DB (usage stats).
- `colors` » color palette for messages.
- `metrics` » anonymous bStats stats (**R**).
- `messages.yml` / `sounds.yml` » messages and sounds.

See the full [Configuration Page](https://jruk8.github.io/JPaperTemplate/configuration/) for more info.

# Links

- [Documentation](https://jruk8.github.io/JPaperTemplate/)
- [Discord](https://discord.com/invite/hkWmCVmWDC)
- [GitHub](https://github.com/jruk8/JPaperTemplate)

### Donate ☕

If you like my plugins, you can support my work through a [**small donation**](https://ko-fi.com/jruk). **Thanks!** 👐

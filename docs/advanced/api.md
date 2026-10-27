---
icon: material/puzzle
---

# Plugin API

> For: developers extending JuicyTPA.

This page explains where new code plugs in. Every module follows the same
shape: a package under `com.jruk8.juicytpa.core`, its own config class (or
section), and a registrar implementing `Bootstrap` that is invoked from
`JuicyTPAPlugin.setupCoreModules()`.

Dependencies flow one way, constructor-injected: configs -> services ->
commands/placeholders. There are no singletons or static state.

## Add a Command

1. Create a command class in a new subpackage of `core.commands` (e.g.
   `core.commands.user.fun`) that builds a Brigadier node with
   `Commands.literal(...)`, gates it with `requires()`, and runs its logic
   in `executes(...)`:

   ```java
   public class PingCommand {
       private final CommandContext ctx;

       public PingCommand(CommandContext ctx) { this.ctx = ctx; }

       public LiteralArgumentBuilder<CommandSourceStack> node(String label) {
           return Commands.literal(label)
                   .requires(CommandContext.requiresPermission(TpaPermissions.PING))
                   .execuses(context -> {
                       // Own text lives in MessagesConfig; {braces} resolve from the map.
                       ctx.sendMessage(context.getSource(),
                               messages -> messages.getCore().getHelpHeader(), Map.of());
                       return Command.SINGLE_SUCCESS;
                   });
       }
   }
   ```

   Commands are Paper-native Brigadier nodes registered through
   `LifecycleEvents.COMMANDS` (see `CommandsRegistrar`). The root literal
   (`juicytpa`, plus its configured aliases) carries a bare-root help
   fallback, so a new command only defines its own subcommand node.
   Messages are strictly MiniMessage: plugin placeholders use `{braces}`,
   angle brackets are raw MiniMessage.

2. Attach the node to the root in the matching registrar
   (`UserCommandsRegistrar` or `AdminCommandsRegistrar`) with
   `root.then(new PingCommand(...).node(label))`, injecting only the
   services it needs (commonly `CommandContext`).

3. Declare the permission in `paper-plugin.yml` (with a constant in
   `TpaPermissions`), document it in [Permissions](permissions.md)
   and add a `HelpEntry` so the command shows up in `/juicytpa help`.

## Add a Config Option

The config classes are okaeri-configs. For a small option, add a field to an
existing config class; for a coherent new section, add a nested static class
and give it a field in the parent (see `PluginConfig.Colors` for the
pattern). The section will be created in the yaml file automatically and
reloaded by `ConfigRegistrar` on `/juicytpa reload`.

## Add a Stat to the Database

`SqliteStorage` exposes four operations for generic key/value counters:

- `increment(key, player)` and `incrementGlobal(key)` per-player / global
- `playerTotal(key, player)` and `globalTotal(key)` reads

Give your feature its own stat key and a small service class around
`SqliteStorage` (like `TpaUsageTracker`), then inject that service where
state is produced (listener, command) and consumed (placeholder).

## Add a Placeholder

Track new state with `TpaUsageTracker` (one stat key per counter,
recorded from the command) and expose it in `TpaExpansion.onRequest()`:

```java
if ("my_stat".equals(params)) {
    return counterFor(player, "my_stat", params, tracker::playerMyStat, tracker.globalMyStat());
}
```

Provide the value through an injected service and store it in SQLite if it
should survive restarts. Registered placeholders are announced to
PlaceholderAPI via `getPlaceholders()`.

## Remove the Placeholders Feature

If you do not need PAPI in your plugin:

1. Delete the `core.placeholders` package.
2. Remove the usage tracker wiring in `JuicyTPAPlugin`
   (`setupStorageAndPlaceholders()` and the `CommandsRegistrar` argument).
3. Remove `PlaceholderAPI` from `paper-plugin.yml` (`dependencies`), or keep
   it if your messages use `%otherplugin_...%` placeholders.

## Modules Reference

| Package | Key classes |
| --- | --- |
| `core.commands` | `CommandsRegistrar`, `UserCommandsRegistrar`, `AdminCommandsRegistrar`, `CommandContext`, `TpaPermissions` |
| `core.commands.selectors` | `TargetSelector`, `SelectorSuggestions`, `ValueSuggestions` |
| `core.commands.user` | `HelpCommand`, `HelpEntry`, `VersionCommand`, `AuthorFormatter` |
| `core.configs` | `ConfigRegistrar`, `PluginConfig` |
| `core.messages` | `MessageBootstrap`, `Messenger`, `MessageParser` |
| `core.sounds` | `SoundPlayer`, `SoundsConfig` |
| `core.placeholders` | `PlaceholdersBootstrap`, `TpaUsageTracker`, `TpaExpansion` |
| `core.storage` | `StorageBootstrap`, `SqliteStorage` |
| `core.update` | `UpdateCheckerBootstrap`, `UpdateCheckService`, `GitHubReleaseHttp`, `UpdateCheckNotifier`, `UpdateCheckJoinListener` |

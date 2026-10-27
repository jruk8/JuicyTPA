# Changelog
## [1.0.0] - 2026-10-27

### Bug Fixes

- Wrap player selector in Paper CustomArgumentType

> Raw Brigadier argument types are rejected at registration; the wrapper
> now declares players() as its native client type.

- Capitalize Help in the help header

- Treat missing GitHub release as unpublished, not a failure

- Declare LuckPerms soft dependency with joined classpath


### Build & CI

- Decouple docs deploy from external link checks


### Documentation

- Adopt the JGameMode README structure

- Document JuicyTPA commands, config and placeholders

- Cover tpablock, tpall, pending and alias changes

- Add banner, favicon and light/dark logos

- Use Discord announce banner like JGameMode

- Regroup by audience in the JManhunt style


### Features

- Print a startup banner on enable

> Brand-tinted title plus Modrinth, Discord and GitHub bullets.

- Rebrand template scaffold as JuicyTPA

> Green palette with hue-kept saturation and value, JUICY TPA config
> banner, TPA config schema, message catalog, sounds and permissions.

- Add TPA requests with modes, expiry and teleport

> TpaService owns pending requests with scheduler-driven expiry and
> entity-scheduler teleports; six commands register as hub subcommands
> and standalone roots with permission-gated suggestions.

- Add block list, bulk requests and pending placeholder

> /tpablock toggles a persistent per-player block that beats every mode;
> /tpall (OP) requests everyone when concurrency is on; @a works in /tpa
> and /tpahere only with juicytpa.tpall; %jtpa_pending% names the most
> recent pending sender. Accept keeps /tpacc, cancel takes
> /tpac, /tpcancel and /tpc.

- Add default /tpablock aliases

- Re-roll random self picks and drop at-s/at-p selectors

> A bare @r landing on the sender re-rolls to someone else while others
> are online; @s and @p are rejected with a dedicated error and hidden
> from completion. The request-sent tail now stays in the sender color.

- Static link buttons, new update notice and banner flag

- Split receiver notice body for tpa vs tpahere

- Make TPA mode display names configurable

- Inline default colors for block-all and accept-all mode names

- AFK/canceller warnings with resend confirmations and nudges

- Concurrency cap with tpall bypass, on by default

- Order nudges after feedback, add warn palette color

- Bare-command usage, quiet tpall, send cooldowns

- Arrow-prefixed notice lines, arrival sound at destination

- Warn-color the tpahere direction in the request notice


### Miscellaneous

- Drop unused SelectorSuggestions helper

- Set bStats plugin id

- Silence unchecked and JVM warnings for a clean build

> Use typed captors and matchers in tests; add test JVM flags for
> MockBukkit CDS and sqlite native-access noise.


### Testing

- Cover TPA service, modes, prompts and placeholders

## [1.1.0] - 2026-08-17

### Features

- Placeholders, storage (db)


### Miscellaneous

- Update for v1.0.5 [skip ci]


## [Unreleased]

### Features

- Add template PlaceholderAPI expansion with `authors_command_uses` and
  `authors_command_uses_global` placeholders and a configurable identifier
- Add reusable SQLite storage module (`jtemplate.db`) with generic
  per-player and global counters
- Add storage and placeholder config sections to `config.yml`

### Documentation

- Rewrite README and all docs as template documentation (replacing copied
  third-party content)
- Add AI/developer guidance to AGENTS.md

## [1.0.5] - 2026-08-17

### Bug Fixes

- Checkstyle .* import

- Test import


### Miscellaneous

- Bump plugin-conventions to 1.4.0 and remove useless parts

## [1.0.4] - 2026-08-16

### Bug Fixes

- Ci


### Build & CI

- Fix (please)

## [1.0.3] - 2026-08-16

### Bug Fixes

- Ci


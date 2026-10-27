---
icon: material/message-text
---

# Messages

> For: intermediate admins styling text.

Every entry in `messages.yml` starts with `{prefix}` and is strictly
MiniMessage: `{braces}` are placeholders, angle brackets are raw
MiniMessage, and `\n` breaks the line. Each key is commented in the
file with the placeholders it accepts.

## Palette

The `config.yml` colors resolve when a message renders:

| Tag | Use |
| --- | --- |
| `{plain}` | Body text. |
| `{brand}` | Headers and the brand name. |
| `{hl}` | Numbers, player names and similar values. |
| `{error}` | Errors. |
| `{ac}` | Accept prompts. |
| `{rc}` | Reject prompts. |
| `{sc}` | Sender feedback. |
| `{lh}` | Warning highlights. |
| `{wn}` | Warning bodies. |

Editing a color and reloading is enough; messages never hardcode
colors (except the mode display names, which carry their own inline
tags by design).

## Runtime placeholders

Commands fill these per message: `{version}`, `{authors}`, `{time}`,
`{syntax}`, `{description}`, `{latest}`, `{current}`, `{permission}`,
`{player}`, `{mode}`, `{input}`, `{count}`, `{type}`, `{accept}`,
`{reject}`, `{body}`, `{button}`, plus the `{modrinth}`, `{github}`,
`{discord}` and `{donate}` link tags.

## Key groups

- `prefix` » prepended to every message, trailing space included.
- `core` » reload, help, version and update output, plus the shared
  `noPermission`, `noTargets` and `consoleNeedsTarget` errors.
- `tpa` » everything request-shaped: the receiver notice (`requestReceived`
  with its `{body}`, `{accept}` and `{reject}` parts), sender
  confirmations, accept/deny/timeout/cancel pairs (chat plus
  actionbar), mode and block feedback, usage and cooldown errors, and
  the warning and nudge texts.
- `help` » the `/juicytpa help` description per command.

## Languages

Messages are per player, never per server: every recipient reads the
bundle matching their own language. Without a pin the language follows
the client's locale live; `/tpalanguage <language>` (alias `/tpalang`)
pins one persistently, `/tpalanguage auto` clears the pin, and a bare
`/tpalanguage` shows the current one. English is the fallback
everywhere: console, unknown locales, missing files and missing keys
all read `messages.yml`. Each used language seeds its `lang/<code>.yml`
from the bundled translation on first use; the files stay
admin-editable and overlay the English schema, so deleting a key (or a
whole file) just reads English again. Duration units (`48s`, `19m 25s`)
stay English abbreviations in every language.

| Code | Language | Code | Language |
| --- | --- | --- | --- |
| `en` | English | `ja` | 日本語 |
| `fi` | Suomi | `pl` | Polski |
| `de` | Deutsch | `tr` | Türkçe |
| `es` | Español | `it` | Italiano |
| `fr` | Français | `ko` | 한국어 |
| `zh` | 中文 | `nl` | Nederlands |
| `ru` | Русский | `uk` | Українська |
| `pt` | Português | `se` | Svenska |
| | | `no` | Norsk |

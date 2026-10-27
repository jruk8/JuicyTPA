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

The `defaultLanguage` key in `config.yml` decides what unpinned
players read: `auto` (the default) keeps following each player's
client locale, while any other key forces that language on everyone
without a pin; existing pins always win. Unknown keys warn in the
console and behave as `auto`. To strictly force one language (e.g.
for unique messages), set it there and revoke `juicytpa.language`
with your permissions plugin so nobody can pin anything else.

| Code | Language | Code | Language |
| --- | --- | --- | --- |
| `en` | English | `da` | Dansk |
| `fi` | Suomi | `cs` | Čeština |
| `de` | Deutsch | `hu` | Magyar |
| `es` | Español | `vi` | Tiếng Việt |
| `fr` | Français | `id` | Bahasa Indonesia |
| `zh_cn` | 简体中文 | `ar` | العربية |
| `ru` | Русский | `hi` | हिन्दी |
| `pt_br` | Português | `el` | Ελληνικά |
| `ja` | 日本語 | `sk` | Slovenčina |
| `pl` | Polski | `ro` | Română |
| `tr` | Türkçe | `bg` | Български |
| `it` | Italiano | `he` | עברית |
| `ko` | 한국어 | `th` | ไทย |
| `nl` | Nederlands | `ms` | Bahasa Melayu |
| `uk` | Українська | `lt` | Lietuvių |
| `sv` | Svenska | `lv` | Latviešu |
| `no` | Norsk | `et` | Eesti |
| `pt_pt` | Português | `hr` | Hrvatski |
| `zh_tw` | 繁體中文 | `sr` | Српски |
| `ca` | Català | `bs` | Bosanski |
| `gl` | Galego | `sl` | Slovenščina |
| `is` | Íslenska | `mk` | Македонски |
| `ga` | Gaeilge | `ka` | ქართული |
| `hy` | Հայերեն | `az` | Azərbaycanca |
| `kk` | Қазақша | `lol_us` | LOLCAT |
| `en_pt` | Pirate Speak | `qya_aa` | Quenya |

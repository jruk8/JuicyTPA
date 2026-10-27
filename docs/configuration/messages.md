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

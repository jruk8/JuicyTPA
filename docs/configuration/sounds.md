---
icon: material/volume-high
---

# Sounds

> For: intermediate admins styling sounds.

Sound names are namespaced Minecraft keys (e.g.
`block.note_block.pling`); unknown keys are logged and skipped.
`sounds.yml` holds one entry per request event:

| Key | Heard when |
| --- | --- |
| `neutral` | Ordinary command feedback. |
| `celebration` | Reloads, help and version output. |
| `ask` | A TP request arrives (receiver). |
| `accept` | A TP request is accepted (the party that stays put). |
| `teleport` | A teleport lands (the mover, at the destination). |
| `reject` | A TP request is denied (sender). |
| `timeout` | A TP request times out (both parties). |
| `angry` | A command fails. |

Each entry supports `enabled`, `sound`, `pitch` and `volume`:

```yaml
accept:
  enabled: true
  sound: block.cherry_wood_pressure_plate.click_on
  pitch: 1.2
  volume: 1.0
```

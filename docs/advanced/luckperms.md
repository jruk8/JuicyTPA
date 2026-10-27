---
icon: material/shield
---

# LuckPerms

> For: advanced users monetizing cooldowns in ranks.

Install LuckPerms and each player's `/tpa` and `/tpahere` cooldown
reads the `juicytpa-cooldown` meta key (configurable under
`tpa.cooldown`). That makes shorter cooldowns a natural rank perk:
free players wait out the default while donors teleport back to
back.

```
/lp group default meta set juicytpa-cooldown 30
/lp group vip meta set juicytpa-cooldown 5
/lp group admin meta set juicytpa-cooldown 0
```

Zero or below disables the cooldown for that player; missing or
unparseable meta falls back to `defaultSeconds`. Values read live on
every send, so no reload is needed after a meta change. LuckPerms is
optional: without it everyone uses the default, and the plugin never
touches its API.

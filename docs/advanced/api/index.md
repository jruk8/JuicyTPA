---
icon: material/puzzle
---

# Plugin API

> For: developers hooking JuicyTPA from their own plugin.

This folder is the source of truth for the public API: the
`com.jruk8.juicytpa.api` package. Everything in it is stable across
minor releases: the `JuicyTPA` service interface, the `TpaRequest`
and `TpaMode` model, and the five request events. Every other
package is internal and may move without notice.

- [Events](events.md) » which events fire, in which order, and what
  cancelling one does.
- [Actions](actions.md) » the `JuicyTPA` service: queries, accept /
  deny / cancel, and the threading rules.

## Depending on JuicyTPA

Declare the dependency in your `paper-plugin.yml` so your plugin
loads after JuicyTPA:

```yaml
dependencies:
  server:
    JuicyTPA:
      load: BEFORE
      required: true
```

Compile against the JuicyTPA jar (`compileOnly`), then look the
service up when you need it:

```java
RegisteredServiceProvider<JuicyTPA> handle =
        Bukkit.getServicesManager().load(JuicyTPA.class);
if (handle == null) {
    return; // JuicyTPA is not installed.
}
JuicyTPA tpa = handle.getProvider();
```

Events need no lookup: implement `Listener` and register it as
usual. The two `Cancellable` events are the eligibility gate other
systems hook into.

## Example: charging for requests with Vault

Gate sending on the sender's balance, and tell them exactly what
is missing with a custom deny message:

```java
@EventHandler
public void onSend(TpaRequestSendEvent event) {
    double cost = 100.0;
    if (economy.getBalance(event.getSender()) < cost) {
        event.setCancelled(true);
        event.setDenyMessage(Component.text(
                "You need $100 to send a TP request.",
                NamedTextColor.RED));
    } else {
        economy.withdrawPlayer(event.getSender(), cost);
    }
}
```

The vetoed request is never stored, never announced, and never
starts a cooldown, so charging here cannot double-charge: the send
event fires exactly once per accepted delivery attempt, including
`/tpall` targets (once each) and instantly accepted asks.

## Example: custom eligibility

Any rule fits the same shape. A claim plugin that only allows
requests inside friendly territory:

```java
@EventHandler
public void onSend(TpaRequestSendEvent event) {
    if (!claims.mayTeleport(event.getSender(), event.getReceiver())) {
        event.setCancelled(true);
        // No deny message: the sender sees the configured
        // externalDenied message from messages.yml instead.
    }
}
```

Accept-time rules (combat tags, minigame state) belong on
`TpaRequestAcceptEvent` instead: a veto there keeps a manual
request pending, so the player can retry after the fight.

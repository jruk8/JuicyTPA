package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.config.PluginConfig;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.LongSupplier;

/**
 * Cooldown between outgoing requests. Each player waits their LuckPerms
 * meta value in seconds, the configured default without (or with
 * unparseable) meta, or nothing at all when the effective value is zero
 * or below. Last-send timestamps live in memory.
 */
public final class TpaCooldowns {

    private final PluginConfig pluginConfig;
    private final LongSupplier clock;
    private final BiFunction<Player, String, String> metaReader;
    private final Map<UUID, Long> lastSent = new HashMap<>();

    /**
     * @param pluginConfig the live plugin config
     * @param clock the epoch-millis clock, injectable for tests
     * @param metaReader reads a raw LuckPerms meta value for a player
     */
    public TpaCooldowns(
            PluginConfig pluginConfig, LongSupplier clock, BiFunction<Player, String, String> metaReader) {
        this.pluginConfig = pluginConfig;
        this.clock = clock;
        this.metaReader = metaReader;
    }

    /**
     * @param sender the sending player
     * @return the remaining cooldown in millis, or 0 when free to send
     */
    public long remaining(Player sender) {
        var cooldown = this.pluginConfig.getTpa().getCooldown();
        if (!cooldown.isEnabled()) {
            return 0;
        }
        int seconds = secondsFor(sender, cooldown);
        if (seconds <= 0) {
            return 0;
        }
        Long last = this.lastSent.get(sender.getUniqueId());
        if (last == null) {
            return 0;
        }
        return Math.max(0, last + (long) seconds * 1000L - this.clock.getAsLong());
    }

    /**
     * Starts the sender's cooldown now.
     *
     * @param sender the sending player
     */
    public void record(Player sender) {
        this.lastSent.put(sender.getUniqueId(), this.clock.getAsLong());
    }

    private int secondsFor(Player sender, PluginConfig.Cooldown cooldown) {
        String raw = this.metaReader.apply(sender, cooldown.getMetaKey());
        if (raw == null) {
            return cooldown.getDefaultSeconds();
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException bad) {
            return cooldown.getDefaultSeconds();
        }
    }
}

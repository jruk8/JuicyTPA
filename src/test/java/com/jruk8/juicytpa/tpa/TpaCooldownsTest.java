package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.config.PluginConfig;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TpaCooldownsTest {

    private final UUID senderId = UUID.randomUUID();
    private final AtomicLong clock = new AtomicLong(1_000_000L);
    private final Map<UUID, String> metas = new HashMap<>();

    private PluginConfig pluginConfig;
    private TpaCooldowns cooldowns;
    private Player sender;

    @BeforeEach
    void setUp() {
        clock.set(1_000_000L);
        metas.clear();
        pluginConfig = new PluginConfig();
        cooldowns = new TpaCooldowns(pluginConfig, clock::get, (player, key) -> metas.get(player.getUniqueId()));
        sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(senderId);
    }

    @Test
    @DisplayName("should stay free when disabled")
    void shouldStayFreeWhenDisabled() {
        pluginConfig.getTpa().getCooldown().setEnabled(false);
        cooldowns.record(sender);

        assertEquals(0, cooldowns.remaining(sender));
    }

    @Test
    @DisplayName("should stay free before the first send")
    void shouldStayFreeBeforeFirstSend() {
        assertEquals(0, cooldowns.remaining(sender));
    }

    @Test
    @DisplayName("should count down the default after a send")
    void shouldCountDownDefault() {
        cooldowns.record(sender);

        assertEquals(15_000L, cooldowns.remaining(sender));

        clock.addAndGet(14_000L);

        assertEquals(1_000L, cooldowns.remaining(sender));

        clock.addAndGet(1_000L);

        assertEquals(0, cooldowns.remaining(sender));
    }

    @Test
    @DisplayName("should read the meta value when present")
    void shouldReadMetaValue() {
        metas.put(senderId, "60");
        cooldowns.record(sender);

        assertEquals(60_000L, cooldowns.remaining(sender));
    }

    @Test
    @DisplayName("should skip the cooldown for non-positive values")
    void shouldSkipNonPositiveValues() {
        metas.put(senderId, "0");
        cooldowns.record(sender);

        assertEquals(0, cooldowns.remaining(sender));

        metas.put(senderId, "-5");

        assertEquals(0, cooldowns.remaining(sender));
    }

    @Test
    @DisplayName("should fall back to the default on unparseable meta")
    void shouldFallBackOnBadMeta() {
        metas.put(senderId, "soon");
        cooldowns.record(sender);

        assertEquals(15_000L, cooldowns.remaining(sender));
    }
}

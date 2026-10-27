package com.jruk8.juicytpa.core.placeholders;

import com.jruk8.juicytpa.core.commands.tpa.TpaMode;
import com.jruk8.juicytpa.core.commands.tpa.TpaModeStore;
import com.jruk8.juicytpa.core.commands.tpa.TpaService;
import com.jruk8.juicytpa.core.messages.MessagesConfig;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TpaExpansionTest {

    private final UUID selfId = UUID.randomUUID();
    private final UUID otherId = UUID.randomUUID();

    @Mock
    private TpaUsageTracker tracker;

    @Mock
    private TpaModeStore modes;

    @Mock
    private TpaService service;

    @Mock
    private JavaPlugin plugin;

    private TpaExpansion expansion() {
        return new TpaExpansion(
                tracker, modes, service, plugin, new MessagesConfig(), Map.of("Alex", otherId)::get);
    }

    private OfflinePlayer player(UUID id) {
        OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }

    private void meta(String name, String version) {
        PluginMeta meta = mock(PluginMeta.class);
        when(meta.getAuthors()).thenReturn(List.of("jruk"));
        when(meta.getVersion()).thenReturn(version);
        when(plugin.getPluginMeta()).thenReturn(meta);
        when(plugin.getName()).thenReturn(name);
    }

    @Test
    @DisplayName("should advertise the jtpa identifier and every placeholder")
    void shouldAdvertisePlaceholders() {
        var placeholders = expansion().getPlaceholders();

        assertEquals("jtpa", expansion().getIdentifier());
        assertEquals(9, placeholders.size());
        assertTrue(placeholders.contains("%jtpa_sent%"));
        assertTrue(placeholders.contains("%jtpa_sent_<player>%"));
        assertTrue(placeholders.contains("%jtpa_sent_global%"));
        assertTrue(placeholders.contains("%jtpa_received%"));
        assertTrue(placeholders.contains("%jtpa_received_<player>%"));
        assertTrue(placeholders.contains("%jtpa_received_global%"));
        assertTrue(placeholders.contains("%jtpa_mode%"));
        assertTrue(placeholders.contains("%jtpa_mode_<player>%"));
        assertTrue(placeholders.contains("%jtpa_pending%"));
    }

    @Test
    @DisplayName("should read author, version and required plugin from metadata")
    void shouldReadMetadata() {
        meta("JuicyTPA", "1.0.0");

        assertEquals("jruk", expansion().getAuthor());
        assertEquals("1.0.0", expansion().getVersion());
        assertEquals("JuicyTPA", expansion().getRequiredPlugin());
    }

    @Test
    @DisplayName("should resolve the counters for the requesting player")
    void shouldResolveSelfCounters() {
        when(tracker.playerSent(selfId)).thenReturn(7);
        when(tracker.playerReceived(selfId)).thenReturn(3);
        OfflinePlayer self = player(selfId);

        assertEquals("7", expansion().onRequest(self, "sent"));
        assertEquals("3", expansion().onRequest(self, "received"));
    }

    @Test
    @DisplayName("should resolve the counters for a named player")
    void shouldResolveNamedCounters() {
        when(tracker.playerSent(otherId)).thenReturn(4);
        when(tracker.playerReceived(otherId)).thenReturn(9);

        assertEquals("4", expansion().onRequest(mock(OfflinePlayer.class), "sent_Alex"));
        assertEquals("9", expansion().onRequest(mock(OfflinePlayer.class), "received_Alex"));
    }

    @Test
    @DisplayName("should resolve the global counters")
    void shouldResolveGlobalCounters() {
        when(tracker.globalSent()).thenReturn(11);
        when(tracker.globalReceived()).thenReturn(13);

        assertEquals("11", expansion().onRequest(mock(OfflinePlayer.class), "sent_global"));
        assertEquals("13", expansion().onRequest(mock(OfflinePlayer.class), "received_global"));
    }

    @Test
    @DisplayName("should resolve the mode display name")
    void shouldResolveMode() {
        when(modes.mode(selfId)).thenReturn(TpaMode.BLOCK_ALL);
        when(modes.mode(otherId)).thenReturn(TpaMode.ACCEPT_ALL);

        assertEquals("Block All", expansion().onRequest(player(selfId), "mode"));
        assertEquals("Accept All", expansion().onRequest(mock(OfflinePlayer.class), "mode_Alex"));
    }

    @Test
    @DisplayName("should resolve the pending sender or dashes")
    void shouldResolvePending() {
        when(service.latestPendingSender(selfId)).thenReturn(java.util.Optional.of("Alex"));
        when(service.latestPendingSender(otherId)).thenReturn(java.util.Optional.empty());

        assertEquals("Alex", expansion().onRequest(player(selfId), "pending"));
        assertEquals("---", expansion().onRequest(player(otherId), "pending"));
    }

    @Test
    @DisplayName("should return null for missing context and unknown params")
    void shouldReturnNullForUnresolvable() {
        assertNull(expansion().onRequest(null, "sent"));
        assertNull(expansion().onRequest(null, "mode"));
        assertNull(expansion().onRequest(null, "pending"));
        assertNull(expansion().onRequest(mock(OfflinePlayer.class), "sent_"));
        assertNull(expansion().onRequest(mock(OfflinePlayer.class), "mode_"));
        assertNull(expansion().onRequest(mock(OfflinePlayer.class), "unknown"));
    }
}

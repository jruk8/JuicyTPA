package com.jruk8.juicytpa.core.commands.tpa;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.commands.TpaPermissions;
import com.jruk8.juicytpa.core.configs.PluginConfig;
import com.jruk8.juicytpa.core.messages.MessagesConfig;
import com.jruk8.juicytpa.core.messages.Messenger;
import com.jruk8.juicytpa.core.placeholders.TpaUsageTracker;
import com.jruk8.juicytpa.core.sounds.SoundPlayer;
import com.jruk8.juicytpa.core.sounds.SoundsConfig;
import com.jruk8.juicytpa.core.storage.SqliteStorage;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TpaSuggestionsTest {

    private static ServerMock server;

    @Mock
    private CommandSourceStack source;

    @Mock
    private Messenger messenger;

    @Mock
    private SoundPlayer soundPlayer;

    @Mock
    private Plugin plugin;

    private SqliteStorage storage;
    private TpaService service;
    private final Map<UUID, Player> online = new HashMap<>();

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("suggest-test"));
        assertTrue(storage.init());
        var ctx = new CommandContext(new MessagesConfig(), new SoundsConfig(), messenger, soundPlayer);
        var pluginConfig = new PluginConfig();
        var warns = new TpaWarnManager(
                new TpaWarnStore(() -> storage), pluginConfig, System::currentTimeMillis);
        var cooldowns = new TpaCooldowns(pluginConfig, System::currentTimeMillis, (player, key) -> null);
        service = new TpaService(
                ctx, pluginConfig, new TpaModeStore(() -> storage), new TpaBlockStore(() -> storage),
                warns, cooldowns, new TpaUsageTracker(() -> storage), plugin,
                (task, delay) -> mock(ScheduledTask.class), online::get);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    @DisplayName("should list selectors and other online players for targets")
    void shouldSuggestTargets() {
        var self = server.addPlayer("Self");
        var alex = server.addPlayer("Alex");
        try {
            when(source.getSender()).thenReturn(self);

            var candidates = TpaSuggestions.suggestTargets(source, null);

            assertTrue(candidates.contains("@r"));
            assertFalse(candidates.contains("@p"));
            assertFalse(candidates.contains("@s"));
            assertFalse(candidates.contains("@a"));
            assertTrue(candidates.contains("Alex"));
            assertTrue(candidates.stream().noneMatch("Self"::equals));
        } finally {
            self.disconnect();
            alex.disconnect();
        }
    }

    @Test
    @DisplayName("should filter target candidates by prefix")
    void shouldFilterTargets() {
        var self = server.addPlayer("Self");
        var alex = server.addPlayer("Alex");
        try {
            when(source.getSender()).thenReturn(self);

            assertEquals(List.of("Alex"), TpaSuggestions.suggestTargets(source, "al"));
            assertEquals(List.of(), TpaSuggestions.suggestTargets(source, "@a"));
        } finally {
            self.disconnect();
            alex.disconnect();
        }
    }

    @Test
    @DisplayName("should gate at-a on the tpall permission")
    void shouldGateAtAll() {
        Player plain = mock(Player.class);
        when(plain.getName()).thenReturn("Plain");
        when(plain.hasPermission(TpaPermissions.TPALL)).thenReturn(false);
        when(source.getSender()).thenReturn(plain);

        assertFalse(TpaSuggestions.suggestTargets(source, null).contains("@a"));

        Player privileged = mock(Player.class);
        when(privileged.getName()).thenReturn("Privileged");
        when(privileged.hasPermission(TpaPermissions.TPALL)).thenReturn(true);
        when(source.getSender()).thenReturn(privileged);

        assertTrue(TpaSuggestions.suggestTargets(source, null).contains("@a"));
    }

    @Test
    @DisplayName("should suggest names without selectors")
    void shouldSuggestNames() {
        var self = server.addPlayer("Self");
        var alex = server.addPlayer("Alex");
        try {
            when(source.getSender()).thenReturn(self);

            var candidates = TpaSuggestions.suggestNames(source, null);

            assertEquals(List.of("Alex"), candidates);
        } finally {
            self.disconnect();
            alex.disconnect();
        }
    }

    @Test
    @DisplayName("should suggest only pending senders for incoming")
    void shouldSuggestIncoming() {
        Player sender = mock(Player.class);
        when(sender.getUniqueId()).thenReturn(UUID.randomUUID());
        when(sender.getName()).thenReturn("Sender");
        Player receiver = mock(Player.class);
        UUID receiverId = UUID.randomUUID();
        when(receiver.getUniqueId()).thenReturn(receiverId);
        when(receiver.getName()).thenReturn("Receiver");
        online.put(sender.getUniqueId(), sender);
        online.put(receiverId, receiver);
        when(source.getSender()).thenReturn(sender);
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(receiver);

        var candidates = TpaSuggestions.suggestIncoming(service, source, null);

        assertEquals(List.of("Sender"), candidates);
        assertEquals(List.of(), TpaSuggestions.suggestIncoming(service, source, "zzz"));
    }

    @Test
    @DisplayName("should suggest only pending receivers for outgoing")
    void shouldSuggestOutgoing() {
        Player sender = mock(Player.class);
        UUID senderId = UUID.randomUUID();
        when(sender.getUniqueId()).thenReturn(senderId);
        when(sender.getName()).thenReturn("Sender");
        Player receiver = mock(Player.class);
        when(receiver.getUniqueId()).thenReturn(UUID.randomUUID());
        when(receiver.getName()).thenReturn("Receiver");
        online.put(senderId, sender);
        online.put(receiver.getUniqueId(), receiver);
        when(source.getSender()).thenReturn(sender);
        service.send(source, receiver, false);

        assertEquals(List.of("Receiver"), TpaSuggestions.suggestOutgoing(service, source, null));
    }

    @Test
    @DisplayName("should suggest the three mode names")
    void shouldSuggestModes() {
        assertEquals(3, TpaSuggestions.suggestModes(null).size());
        assertEquals(List.of("block_all"), TpaSuggestions.suggestModes("blo"));
    }
}

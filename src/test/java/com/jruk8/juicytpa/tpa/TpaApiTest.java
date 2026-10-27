package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.api.JuicyTPA;
import com.jruk8.juicytpa.api.TpaMode;
import com.jruk8.juicytpa.api.TpaRequestAcceptEvent;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.message.MessagesConfig;
import com.jruk8.juicytpa.message.Messenger;
import com.jruk8.juicytpa.sound.SoundPlayer;
import com.jruk8.juicytpa.sound.SoundsConfig;
import com.jruk8.juicytpa.storage.SqliteStorage;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TpaApiTest {

    private final UUID senderId = UUID.randomUUID();
    private final UUID receiverId = UUID.randomUUID();

    @Mock
    private Messenger messenger;

    @Mock
    private SoundPlayer soundPlayer;

    @Mock
    private CommandSourceStack source;

    @Mock
    private Plugin plugin;

    private SqliteStorage storage;
    private TpaService service;
    private JuicyTPA api;
    private Player sender;
    private Player receiver;
    private final List<Event> events = new ArrayList<>();
    private final Map<UUID, Player> online = new HashMap<>();
    private final AtomicLong clock = new AtomicLong();

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("tpa-api-test"));
        assertTrue(storage.init());
        clock.set(1_000_000L);
        var pluginConfig = new PluginConfig();
        pluginConfig.getTpa().getCooldown().setEnabled(false);
        var ctx = new CommandContext(new MessagesConfig(), new SoundsConfig(), messenger, soundPlayer);
        var warns = new TpaWarnManager(new TpaWarnStore(() -> storage), pluginConfig, clock::get);
        var cooldowns = new TpaCooldowns(pluginConfig, clock::get, (player, key) -> null);
        var modes = new TpaModeStore(() -> storage);
        var blocks = new TpaBlockStore(() -> storage);
        var trusts = new TpaTrustStore(() -> storage);
        service = new TpaService(
                ctx, pluginConfig, modes, blocks, trusts, warns, cooldowns,
                new TpaUsageTracker(() -> storage), plugin,
                this::schedule, online::get, uuid -> onlineName(uuid), events::add);
        api = new TpaApi(service, modes, blocks, trusts, online::get);
        sender = player(senderId, "Sender");
        receiver = player(receiverId, "Receiver");
        online.put(senderId, sender);
        online.put(receiverId, receiver);
        lenient().when(source.getSender()).thenReturn(sender);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    private ScheduledTask schedule(Runnable task, long delay) {
        return mock(ScheduledTask.class);
    }

    private String onlineName(UUID id) {
        Player player = online.get(id);
        return player == null ? null : player.getName();
    }

    private Player player(UUID id, String name) {
        Player player = mock(Player.class);
        lenient().when(player.getUniqueId()).thenReturn(id);
        lenient().when(player.getName()).thenReturn(name);
        lenient().when(player.getLocation()).thenReturn(mock(Location.class));
        EntityScheduler entityScheduler = mock(EntityScheduler.class);
        lenient().when(entityScheduler.run(eq(plugin), any(), any())).thenAnswer(invocation -> {
            invocation.<Consumer<ScheduledTask>>getArgument(1).accept(mock(ScheduledTask.class));
            return mock(ScheduledTask.class);
        });
        lenient().when(player.getScheduler()).thenReturn(entityScheduler);
        return player;
    }

    @Test
    @DisplayName("should expose pending requests through queries")
    void shouldQueryPending() {
        service.send(source, receiver, false);

        assertEquals(1, api.incoming(receiverId).size());
        assertEquals(1, api.outgoing(senderId).size());
        assertEquals(senderId, api.incoming(receiverId).getFirst().sender());
        assertTrue(api.incoming(senderId).isEmpty());
    }

    @Test
    @DisplayName("should expose mode, block and trust state")
    void shouldQueryState() {
        assertEquals(TpaMode.NORMAL, api.mode(senderId));

        service.mode(source, Optional.of("accept"));
        service.block(source, receiver);
        service.trust(source, receiver);

        assertEquals(TpaMode.ACCEPT_ALL, api.mode(senderId));
        assertTrue(api.isBlocked(senderId, receiverId));
        assertFalse(api.isBlocked(receiverId, senderId));
        assertTrue(api.isTrusted(senderId, receiverId));
    }

    @Test
    @DisplayName("should accept through the API with events")
    void shouldAcceptViaApi() {
        service.send(source, receiver, false);

        assertTrue(api.accept(receiverId, senderId));

        assertTrue(api.incoming(receiverId).isEmpty());
        assertTrue(events.getLast() instanceof TpaRequestAcceptEvent);
    }

    @Test
    @DisplayName("should deny through the API")
    void shouldDenyViaApi() {
        service.send(source, receiver, false);

        assertTrue(api.deny(receiverId, senderId));

        assertTrue(api.incoming(receiverId).isEmpty());
    }

    @Test
    @DisplayName("should cancel through the API")
    void shouldCancelViaApi() {
        service.send(source, receiver, false);

        assertTrue(api.cancel(senderId, receiverId));

        assertTrue(api.outgoing(senderId).isEmpty());
    }

    @Test
    @DisplayName("should fail actions without a match")
    void shouldRejectWithoutMatch() {
        assertFalse(api.accept(receiverId, senderId));
        assertFalse(api.deny(receiverId, senderId));
        assertFalse(api.cancel(senderId, receiverId));
    }

    @Test
    @DisplayName("should fail actions for offline actors")
    void shouldRejectOfflineActors() {
        service.send(source, receiver, false);
        online.remove(receiverId);
        online.remove(senderId);

        assertFalse(api.accept(receiverId, senderId));
        assertFalse(api.deny(receiverId, senderId));
        assertFalse(api.cancel(senderId, receiverId));
    }
}

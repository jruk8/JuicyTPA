package com.jruk8.juicytpa.tpa;

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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.jruk8.juicytpa.api.TpaMode;

@ExtendWith(MockitoExtension.class)

class TpaServiceReceivingTest {

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

    @Captor
    private ArgumentCaptor<Map<String, String>> bodyCaptor;

    private SqliteStorage storage;
    private PluginConfig pluginConfig;
    private MessagesConfig messages;
    private SoundsConfig sounds;
    private TpaService service;
    private Player sender;
    private Player receiver;
    private final List<Runnable> scheduled = new ArrayList<>();
    private final Map<UUID, Player> online = new HashMap<>();
    private final Map<UUID, String> metas = new HashMap<>();
    private final AtomicLong clock = new AtomicLong();

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("tpa-service-test"));
        assertTrue(storage.init());
        clock.set(1_000_000L);
        metas.clear();
        pluginConfig = new PluginConfig();
        pluginConfig.getTpa().getCooldown().setEnabled(false);
        messages = new MessagesConfig();
        sounds = new SoundsConfig();
        var ctx = new CommandContext(messages, sounds, messenger, soundPlayer);
        var warns = new TpaWarnManager(new TpaWarnStore(() -> storage), pluginConfig, clock::get);
        var cooldowns = new TpaCooldowns(pluginConfig, clock::get, (player, key) -> metas.get(player.getUniqueId()));
        service = new TpaService(
                ctx, pluginConfig, new TpaModeStore(() -> storage), new TpaBlockStore(() -> storage),
                new TpaTrustStore(() -> storage), warns, cooldowns, new TpaUsageTracker(() -> storage), plugin,
                this::schedule, online::get, uuid -> onlineName(uuid));
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
        scheduled.add(task);
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
    @DisplayName("should cycle and set the mode")
    void shouldChangeMode() {
        assertEquals(1, service.mode(source, Optional.empty()));
        assertEquals(1, service.mode(source, Optional.of("accept")));
        assertEquals(0, service.mode(source, Optional.of("nope")));

        assertEquals(TpaMode.ACCEPT_ALL, new TpaModeStore(() -> storage).mode(senderId));
        verify(messenger, times(2))
                .send(eq(sender), eq(messages.getTpa().getModeSet()), anyMap());
    }

    @Test
    @DisplayName("should list nothing pending gracefully")
    void shouldListEmpty() {
        assertEquals(1, service.list(source));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getListEmpty()), anyMap());
        verify(soundPlayer).play((CommandSender) sender, sounds.getNeutral());
    }

    @Test
    @DisplayName("should list incoming before outgoing with clickable names")
    void shouldListRequests() {
        Player other = player(UUID.randomUUID(), "Other");
        online.put(other.getUniqueId(), other);
        Player rival = player(UUID.randomUUID(), "Rival");
        online.put(rival.getUniqueId(), rival);
        Player third = player(UUID.randomUUID(), "Third");
        online.put(third.getUniqueId(), third);
        service.send(source, receiver, false);
        service.send(source, other, true);
        when(source.getSender()).thenReturn(rival);
        service.send(source, sender, false);
        when(source.getSender()).thenReturn(third);
        service.send(source, sender, true);
        when(source.getSender()).thenReturn(sender);

        assertEquals(1, service.list(source));

        var inOrder = inOrder(messenger);
        inOrder.verify(messenger).send(eq(sender), eq(messages.getTpa().getListHeader()), anyMap());
        inOrder.verify(messenger, times(4))
                .send(eq(sender), eq(messages.getTpa().getListLine()), bodyCaptor.capture());
        var lines = bodyCaptor.getAllValues();
        assertEquals("Incoming TP to you", lines.get(0).get("category"));
        assertEquals("Incoming TP to them", lines.get(1).get("category"));
        assertEquals("Outgoing TP to them", lines.get(2).get("category"));
        assertEquals("Outgoing TP to you", lines.get(3).get("category"));
        assertTrue(lines.get(0).get("players").contains("/tpaccept Rival"));
        assertTrue(lines.get(2).get("players").contains("/tpacancel Receiver"));
        verify(soundPlayer, times(3)).play(sender, sounds.getNeutral());
    }

    @Test
    @DisplayName("should toggle trust on and off")
    void shouldToggleTrust() {
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.trust(source, sender));
        assertTrue(new TpaTrustStore(() -> storage).isTrusted(receiverId, senderId));
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getTrustAdded()), anyMap());

        assertEquals(1, service.trust(source, sender));
        assertFalse(new TpaTrustStore(() -> storage).isTrusted(receiverId, senderId));
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getTrustRemoved()), anyMap());
    }

    @Test
    @DisplayName("should reject trusting self")
    void shouldRejectSelfTrust() {
        assertEquals(0, service.trust(source, sender));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getTrustSelf()), anyMap());
    }

    @Test
    @DisplayName("should auto-accept asks from trusted players")
    void shouldAutoAcceptTrustedAsks() {
        new TpaTrustStore(() -> storage).setTrusted(receiverId, senderId, true);

        assertEquals(1, service.send(source, receiver, false));

        assertTrue(service.incoming(receiverId).isEmpty());
        verify(sender).teleport(receiver.getLocation());
    }

    @Test
    @DisplayName("should still pend heres from trusted players")
    void shouldPendTrustedHeres() {
        new TpaTrustStore(() -> storage).setTrusted(receiverId, senderId, true);

        assertEquals(1, service.send(source, receiver, true));

        assertEquals(1, service.incoming(receiverId).size());
        verify(sender, never()).teleport(any(Location.class));
    }

    @Test
    @DisplayName("should let blocks beat trust")
    void shouldPreferBlockOverTrust() {
        new TpaTrustStore(() -> storage).setTrusted(receiverId, senderId, true);
        new TpaBlockStore(() -> storage).setBlocked(receiverId, senderId, true);

        assertEquals(0, service.send(source, receiver, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getRequestBlocked()), anyMap());
    }

    @Test
    @DisplayName("should list trusted players before request lines")
    void shouldListTrustedFirst() {
        Player rival = player(UUID.randomUUID(), "Rival");
        online.put(rival.getUniqueId(), rival);
        new TpaTrustStore(() -> storage).setTrusted(senderId, rival.getUniqueId(), true);
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(sender);

        assertEquals(1, service.list(source));

        var inOrder = inOrder(messenger);
        inOrder.verify(messenger).send(eq(sender), eq(messages.getTpa().getListHeader()), anyMap());
        inOrder.verify(messenger, times(2))
                .send(eq(sender), eq(messages.getTpa().getListLine()), bodyCaptor.capture());
        var lines = bodyCaptor.getAllValues();
        assertEquals("Trusted", lines.get(0).get("category"));
        assertTrue(lines.get(0).get("players").contains("/tpatrust Rival"));
        assertEquals("Outgoing TP to them", lines.get(1).get("category"));
    }

    @Test
    @DisplayName("should flip between accept-all and normal")
    void shouldFlipAutoMode() {
        assertEquals(1, service.autoMode(source));
        assertEquals(TpaMode.ACCEPT_ALL, new TpaModeStore(() -> storage).mode(senderId));

        assertEquals(1, service.autoMode(source));
        assertEquals(TpaMode.NORMAL, new TpaModeStore(() -> storage).mode(senderId));
    }

    @Test
    @DisplayName("should flip to accept-all from block-all")
    void shouldFlipAutoModeFromBlockAll() {
        new TpaModeStore(() -> storage).setMode(senderId, TpaMode.BLOCK_ALL);

        assertEquals(1, service.autoMode(source));

        assertEquals(TpaMode.ACCEPT_ALL, new TpaModeStore(() -> storage).mode(senderId));
    }

    @Test
    @DisplayName("should flip between block-all and normal")
    void shouldFlipBlockAllMode() {
        assertEquals(1, service.blockAllMode(source));
        assertEquals(TpaMode.BLOCK_ALL, new TpaModeStore(() -> storage).mode(senderId));

        assertEquals(1, service.blockAllMode(source));
        assertEquals(TpaMode.NORMAL, new TpaModeStore(() -> storage).mode(senderId));
    }

    @Test
    @DisplayName("should reject console senders")
    void shouldRejectConsole() {
        when(source.getSender()).thenReturn(mock(CommandSender.class));

        assertEquals(0, service.send(source, receiver, false));

        verify(messenger).send(any(CommandSender.class), eq(messages.getTpa().getPlayerOnly()), anyMap());
    }
}

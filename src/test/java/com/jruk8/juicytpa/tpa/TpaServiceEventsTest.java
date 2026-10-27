package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.api.TpaRequestAcceptEvent;
import com.jruk8.juicytpa.api.TpaRequestCancelEvent;
import com.jruk8.juicytpa.api.TpaRequestDenyEvent;
import com.jruk8.juicytpa.api.TpaRequestExpireEvent;
import com.jruk8.juicytpa.api.TpaRequestSendEvent;
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
import net.kyori.adventure.text.Component;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TpaServiceEventsTest {

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
    private PluginConfig pluginConfig;
    private MessagesConfig messages;
    private SoundsConfig sounds;
    private TpaService service;
    private Player sender;
    private Player receiver;
    private final List<Runnable> scheduled = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private final Map<UUID, Player> online = new HashMap<>();
    private final Map<UUID, String> metas = new HashMap<>();
    private final AtomicLong clock = new AtomicLong();
    private Consumer<Event> eventHook = event -> {
    };

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("tpa-events-test"));
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
                this::schedule, online::get, uuid -> onlineName(uuid), this::captureEvent);
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

    private void captureEvent(Event event) {
        eventHook.accept(event);
        events.add(event);
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
    @DisplayName("should fire the send event with the stored request")
    void shouldFireSendEvent() {
        assertEquals(1, service.send(source, receiver, false));

        assertEquals(1, events.size());
        var event = (TpaRequestSendEvent) events.getFirst();
        assertEquals(senderId, event.getRequest().sender());
        assertEquals(receiverId, event.getRequest().receiver());
        assertEquals(sender, event.getSender());
        assertEquals(receiver, event.getReceiver());
    }

    @Test
    @DisplayName("should veto the request when the send event is cancelled")
    void shouldVetoSend() {
        eventHook = event -> {
            if (event instanceof TpaRequestSendEvent send) {
                send.setCancelled(true);
            }
        };

        assertEquals(0, service.send(source, receiver, false));

        assertTrue(service.outgoing(senderId).isEmpty());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getExternalDenied()), anyMap());
    }

    @Test
    @DisplayName("should prefer the custom deny message on veto")
    void shouldSendCustomDenyMessage() {
        var denied = Component.text("Not today.");
        eventHook = event -> {
            if (event instanceof TpaRequestSendEvent send) {
                send.setCancelled(true);
                send.setDenyMessage(denied);
            }
        };

        assertEquals(0, service.send(source, receiver, false));

        verify(sender).sendMessage(denied);
        verify(messenger, never()).send(eq(sender), eq(messages.getTpa().getExternalDenied()), anyMap());
    }

    @Test
    @DisplayName("should veto the teleport but keep the request when the accept event is cancelled")
    void shouldVetoAccept() {
        service.send(source, receiver, false);
        eventHook = event -> {
            if (event instanceof TpaRequestAcceptEvent accept) {
                accept.setCancelled(true);
            }
        };
        when(source.getSender()).thenReturn(receiver);

        assertEquals(0, service.accept(source, Optional.of(sender)));

        assertEquals(1, service.incoming(receiverId).size());
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getExternalDenied()), anyMap());
    }

    @Test
    @DisplayName("should fire send then accept for manual accepts")
    void shouldFireAcceptEvent() {
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.accept(source, Optional.of(sender)));

        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof TpaRequestSendEvent);
        assertTrue(events.get(1) instanceof TpaRequestAcceptEvent);
    }

    @Test
    @DisplayName("should veto trusted auto-accepts and tell the sender")
    void shouldVetoTrustedAutoAccept() {
        when(source.getSender()).thenReturn(receiver);
        service.trust(source, sender);
        when(source.getSender()).thenReturn(sender);
        eventHook = event -> {
            if (event instanceof TpaRequestAcceptEvent accept) {
                accept.setCancelled(true);
            }
        };

        assertEquals(0, service.send(source, receiver, false));

        assertEquals(2, events.size());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getExternalDenied()), anyMap());
        verify(messenger, never()).send(eq(sender), eq(messages.getTpa().getAcceptSender()), anyMap());
    }

    @Test
    @DisplayName("should fire the deny event after removing the request")
    void shouldFireDenyEvent() {
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.deny(source, Optional.of(sender)));

        var event = (TpaRequestDenyEvent) events.getLast();
        assertEquals(senderId, event.getRequest().sender());
        assertEquals(sender, event.getSender());
        assertEquals(receiver, event.getReceiver());
    }

    @Test
    @DisplayName("should fire the command-cause cancel event for /tpacancel")
    void shouldFireCancelEvent() {
        service.send(source, receiver, false);

        assertEquals(1, service.cancel(source, Optional.of(receiver)));

        var event = (TpaRequestCancelEvent) events.getLast();
        assertEquals(TpaRequestCancelEvent.Cause.CANCEL_COMMAND, event.getCause());
        assertEquals(receiverId, event.getRequest().receiver());
    }

    @Test
    @DisplayName("should fire the expire event when the request times out")
    void shouldFireExpireEvent() {
        service.send(source, receiver, false);
        scheduled.getFirst().run();

        var event = (TpaRequestExpireEvent) events.getLast();
        assertEquals(senderId, event.getRequest().sender());
        assertEquals(receiverId, event.getRequest().receiver());
    }

    @Test
    @DisplayName("should fire the sweep-cause cancel event for swept requests")
    void shouldFireSweepCancelEvent() {
        pluginConfig.getTpa().getConcurrency().setAllowConcurrent(true);
        Player rival = player(UUID.randomUUID(), "Rival");
        online.put(rival.getUniqueId(), rival);
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(rival);
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.accept(source, Optional.of(sender)));

        var sweeps = events.stream()
                .filter(TpaRequestCancelEvent.class::isInstance)
                .map(TpaRequestCancelEvent.class::cast)
                .filter(event -> event.getCause() == TpaRequestCancelEvent.Cause.ACCEPT_SWEEP)
                .toList();
        assertEquals(1, sweeps.size());
        assertEquals(rival.getUniqueId(), sweeps.getFirst().getRequest().sender());
    }

    @Test
    @DisplayName("should fire the block-cause cancel event for dropped requests")
    void shouldFireBlockCancelEvent() {
        when(source.getSender()).thenReturn(receiver);
        service.send(source, sender, false);
        when(source.getSender()).thenReturn(sender);

        assertEquals(1, service.block(source, receiver));

        var event = (TpaRequestCancelEvent) events.getLast();
        assertEquals(TpaRequestCancelEvent.Cause.BLOCK, event.getCause());
        assertEquals(receiverId, event.getRequest().sender());
    }
}

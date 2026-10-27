package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.configs.PluginConfig;
import com.jruk8.juicytpa.core.messages.MessagesConfig;
import com.jruk8.juicytpa.core.messages.Messenger;
import com.jruk8.juicytpa.core.placeholders.TpaUsageTracker;
import com.jruk8.juicytpa.core.sounds.SoundPlayer;
import com.jruk8.juicytpa.core.sounds.SoundsConfig;
import com.jruk8.juicytpa.core.storage.SqliteStorage;
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

@ExtendWith(MockitoExtension.class)
class TpaServiceTest {

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
    private final AtomicLong clock = new AtomicLong();

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("tpa-service-test"));
        assertTrue(storage.init());
        clock.set(1_000_000L);
        pluginConfig = new PluginConfig();
        messages = new MessagesConfig();
        sounds = new SoundsConfig();
        var ctx = new CommandContext(messages, sounds, messenger, soundPlayer);
        var warns = new TpaWarnManager(new TpaWarnStore(() -> storage), pluginConfig, clock::get);
        service = new TpaService(
                ctx, pluginConfig, new TpaModeStore(() -> storage), new TpaBlockStore(() -> storage),
                warns, new TpaUsageTracker(() -> storage), plugin, this::schedule, online::get);
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
    @DisplayName("should store the request and notify both parties")
    void shouldSendRequest() {
        assertEquals(1, service.send(source, receiver, false));

        assertEquals(1, service.incoming(receiverId).size());
        assertEquals(1, service.outgoing(senderId).size());
        assertEquals(1, scheduled.size());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getRequestSent()), anyMap());
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getRequestReceived()), anyMap());
        verify(soundPlayer).play(receiver, sounds.getAsk());
    }

    @Test
    @DisplayName("should reject requests to self")
    void shouldRejectSelfRequests() {
        assertEquals(0, service.send(source, sender, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getSelfRequest()), anyMap());
        assertTrue(service.outgoing(senderId).isEmpty());
    }

    @Test
    @DisplayName("should reject a second outgoing request when concurrency is off")
    void shouldRejectSecondRequest() {
        pluginConfig.getTpa().getConcurrency().setAllowConcurrent(false);
        Player other = player(UUID.randomUUID(), "Other");
        online.put(other.getUniqueId(), other);

        assertEquals(1, service.send(source, receiver, false));
        assertEquals(0, service.send(source, other, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getAlreadyPending()), anyMap());
    }

    @Test
    @DisplayName("should cap outgoing requests at maxConcurrent")
    void shouldCapConcurrentRequests() {
        Player second = player(UUID.randomUUID(), "Second");
        online.put(second.getUniqueId(), second);
        Player third = player(UUID.randomUUID(), "Third");
        online.put(third.getUniqueId(), third);
        Player fourth = player(UUID.randomUUID(), "Fourth");
        online.put(fourth.getUniqueId(), fourth);

        assertEquals(1, service.send(source, receiver, false));
        assertEquals(1, service.send(source, second, false));
        assertEquals(1, service.send(source, third, false));
        assertEquals(0, service.send(source, fourth, false));

        assertEquals(3, service.outgoing(senderId).size());
    }

    @Test
    @DisplayName("should allow unlimited requests when the cap is not positive")
    void shouldAllowUnlimitedWithoutCap() {
        pluginConfig.getTpa().getConcurrency().setMaxConcurrent(0);
        Player second = player(UUID.randomUUID(), "Second");
        online.put(second.getUniqueId(), second);
        Player third = player(UUID.randomUUID(), "Third");
        online.put(third.getUniqueId(), third);
        Player fourth = player(UUID.randomUUID(), "Fourth");
        online.put(fourth.getUniqueId(), fourth);

        assertEquals(1, service.send(source, receiver, false));
        assertEquals(1, service.send(source, second, false));
        assertEquals(1, service.send(source, third, false));
        assertEquals(1, service.send(source, fourth, false));

        assertEquals(4, service.outgoing(senderId).size());
    }

    @Test
    @DisplayName("should refuse receivers that block requests")
    void shouldRefuseBlockedReceivers() {
        new TpaModeStore(() -> storage).setMode(receiverId, TpaMode.BLOCK_ALL);

        assertEquals(0, service.send(source, receiver, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getReceiverBlocked()), anyMap());
        assertTrue(service.incoming(receiverId).isEmpty());
    }

    @Test
    @DisplayName("should auto-accept receivers in accept-all mode")
    void shouldAutoAccept() {
        new TpaModeStore(() -> storage).setMode(receiverId, TpaMode.ACCEPT_ALL);

        assertEquals(1, service.send(source, receiver, false));

        assertTrue(service.incoming(receiverId).isEmpty());
        verify(sender).teleport(receiver.getLocation());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getAcceptSender()), anyMap());
        verify(soundPlayer).play(sender, sounds.getAccept());
    }

    @Test
    @DisplayName("should teleport the sender to the receiver on accept")
    void shouldTeleportSenderOnAccept() {
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.accept(source, Optional.empty()));

        assertTrue(service.incoming(receiverId).isEmpty());
        verify(sender).teleport(receiver.getLocation());
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getAcceptReceiver()), anyMap());
    }

    @Test
    @DisplayName("should teleport the receiver to the sender for here requests")
    void shouldTeleportReceiverForHereRequests() {
        service.send(source, receiver, true);
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.accept(source, Optional.empty()));

        verify(receiver).teleport(sender.getLocation());
        verify(sender, never()).teleport(any(Location.class));
    }

    @Test
    @DisplayName("should deny without teleporting")
    void shouldDenyWithoutTeleporting() {
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.deny(source, Optional.empty()));

        assertTrue(service.incoming(receiverId).isEmpty());
        verify(sender, never()).teleport(any(Location.class));
        verify(receiver, never()).teleport(any(Location.class));
        verify(messenger).send(eq(sender), eq(messages.getTpa().getDenySender()), anyMap());
        verify(soundPlayer).play(sender, sounds.getReject());
    }

    @Test
    @DisplayName("should cancel the outgoing request")
    void shouldCancelOutgoing() {
        service.send(source, receiver, false);

        assertEquals(1, service.cancel(source, Optional.empty()));

        assertTrue(service.outgoing(senderId).isEmpty());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getCancelSender()), anyMap());
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getCancelReceiver()), anyMap());
    }

    @Test
    @DisplayName("should time requests out")
    void shouldExpireRequests() {
        service.send(source, receiver, false);
        scheduled.forEach(Runnable::run);

        assertTrue(service.incoming(receiverId).isEmpty());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getTimeoutSender()), anyMap());
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getTimeoutReceiver()), anyMap());
        verify(soundPlayer).play(receiver, sounds.getTimeout());
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
    @DisplayName("should allow several requests when concurrency is on")
    void shouldAllowConcurrentRequests() {
        pluginConfig.getTpa().getConcurrency().setAllowConcurrent(true);
        Player other = player(UUID.randomUUID(), "Other");
        online.put(other.getUniqueId(), other);

        assertEquals(1, service.send(source, receiver, false));
        assertEquals(1, service.send(source, other, false));

        assertEquals(2, service.outgoing(senderId).size());
    }

    @Test
    @DisplayName("should sweep other requests when one is accepted")
    void shouldSweepOthersOnAccept() {
        pluginConfig.getTpa().getConcurrency().setAllowConcurrent(true);
        Player rival = player(UUID.randomUUID(), "Rival");
        online.put(rival.getUniqueId(), rival);
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(rival);
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.accept(source, Optional.of(sender)));

        assertTrue(service.incoming(receiverId).isEmpty());
        verify(messenger).send(eq(rival), eq(messages.getTpa().getAutoCancelled()), anyMap());
    }

    @Test
    @DisplayName("should refuse senders the receiver blocked")
    void shouldRefuseBlockedSenders() {
        new TpaBlockStore(() -> storage).setBlocked(receiverId, senderId, true);

        assertEquals(0, service.send(source, receiver, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getRequestBlocked()), anyMap());
        assertTrue(service.incoming(receiverId).isEmpty());
    }

    @Test
    @DisplayName("should toggle the block and drop pending requests")
    void shouldToggleBlock() {
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(receiver);

        assertEquals(1, service.block(source, sender));
        assertTrue(new TpaBlockStore(() -> storage).isBlocked(receiverId, senderId));
        assertTrue(service.incoming(receiverId).isEmpty());
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getBlockAdded()), anyMap());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getAutoCancelled()), anyMap());

        assertEquals(1, service.block(source, sender));
        assertFalse(new TpaBlockStore(() -> storage).isBlocked(receiverId, senderId));
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getBlockRemoved()), anyMap());
    }

    @Test
    @DisplayName("should reject blocking self")
    void shouldRejectSelfBlock() {
        assertEquals(0, service.block(source, sender));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getBlockSelf()), anyMap());
    }

    @Test
    @DisplayName("should bulk send to every sendable target")
    void shouldBulkSend() {
        pluginConfig.getTpa().getConcurrency().setAllowConcurrent(true);
        Player other = player(UUID.randomUUID(), "Other");
        online.put(other.getUniqueId(), other);

        assertEquals(1, service.sendBulk(source, List.of(sender, receiver, other), false));

        assertEquals(1, service.incoming(receiverId).size());
        assertEquals(1, service.incoming(other.getUniqueId()).size());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getTpallSent()), anyMap());
    }

    @Test
    @DisplayName("should skip blocked and duplicate targets in bulk")
    void shouldSkipUnsuitableBulkTargets() {
        pluginConfig.getTpa().getConcurrency().setAllowConcurrent(true);
        Player blocked = player(UUID.randomUUID(), "Blocked");
        online.put(blocked.getUniqueId(), blocked);
        new TpaBlockStore(() -> storage).setBlocked(blocked.getUniqueId(), senderId, true);
        service.send(source, receiver, false);

        assertEquals(1, service.sendBulk(source, List.of(receiver, blocked), false));

        assertEquals(1, service.incoming(receiverId).size());
        assertTrue(service.incoming(blocked.getUniqueId()).isEmpty());
    }

    @Test
    @DisplayName("should bypass the concurrency toggle for bulk sends")
    void shouldBypassToggleInBulk() {
        pluginConfig.getTpa().getConcurrency().setAllowConcurrent(false);

        assertEquals(1, service.sendBulk(source, List.of(receiver), false));

        assertEquals(1, service.incoming(receiverId).size());
    }

    @Test
    @DisplayName("should bypass the concurrency cap for bulk sends")
    void shouldBypassCapInBulk() {
        pluginConfig.getTpa().getConcurrency().setMaxConcurrent(1);
        Player other = player(UUID.randomUUID(), "Other");
        online.put(other.getUniqueId(), other);

        assertEquals(1, service.send(source, receiver, false));
        assertEquals(1, service.sendBulk(source, List.of(receiver, other), false));

        assertEquals(1, service.incoming(receiverId).size());
        assertEquals(1, service.incoming(other.getUniqueId()).size());
    }

    @Test
    @DisplayName("should name the most recent pending sender")
    void shouldNamePendingSender() {
        Player rival = player(UUID.randomUUID(), "Rival");
        online.put(rival.getUniqueId(), rival);
        pluginConfig.getTpa().getConcurrency().setAllowConcurrent(true);
        service.send(source, receiver, false);
        when(source.getSender()).thenReturn(rival);
        service.send(source, receiver, false);

        assertEquals(Optional.of("Rival"), service.latestPendingSender(receiverId));
        assertEquals(Optional.empty(), service.latestPendingSender(senderId));
    }

    @Test
    @DisplayName("should use the ask body for tpa requests")
    void shouldUseAskBody() {
        service.send(source, receiver, false);

        verify(messenger).send(eq(receiver), eq(messages.getTpa().getRequestReceived()), bodyCaptor.capture());
        var expected = messages.getTpa().getRequestAskBody().replace("{player}", "Sender");
        assertEquals(expected, bodyCaptor.getValue().get("body"));
    }

    @Test
    @DisplayName("should use the here body for tpahere requests")
    void shouldUseHereBody() {
        service.send(source, receiver, true);

        verify(messenger).send(eq(receiver), eq(messages.getTpa().getRequestReceived()), bodyCaptor.capture());
        var expected = messages.getTpa().getRequestHereBody().replace("{player}", "Sender");
        assertEquals(expected, bodyCaptor.getValue().get("body"));
    }

    @Test
    @DisplayName("should warn once then send to AFK receivers on resend")
    void shouldConfirmAfkWarning() {
        var stats = new TpaWarnStore(() -> storage);
        stats.recordExpiry(receiverId, 60);
        stats.recordExpiry(receiverId, 60);
        stats.recordExpiry(receiverId, 60);

        assertEquals(0, service.send(source, receiver, false));
        verify(messenger).send(eq(sender), eq(messages.getTpa().getWarnAfk()), anyMap());

        assertEquals(1, service.send(source, receiver, false));
        assertEquals(1, service.incoming(receiverId).size());
    }

    @Test
    @DisplayName("should re-warn when the confirmation expires")
    void shouldExpireConfirmations() {
        var stats = new TpaWarnStore(() -> storage);
        stats.recordExpiry(receiverId, 60);
        stats.recordExpiry(receiverId, 60);
        stats.recordExpiry(receiverId, 60);

        assertEquals(0, service.send(source, receiver, false));
        clock.addAndGet(16_000L);

        assertEquals(0, service.send(source, receiver, false));
        verify(messenger, times(2)).send(eq(sender), eq(messages.getTpa().getWarnAfk()), anyMap());
    }

    @Test
    @DisplayName("should warn before requesting from frequent cancellers")
    void shouldWarnCancellers() {
        var stats = new TpaWarnStore(() -> storage);
        stats.recordDenied(receiverId, 30);
        stats.recordDenied(receiverId, 30);
        stats.recordDenied(receiverId, 30);

        assertEquals(0, service.send(source, receiver, false));
        verify(messenger).send(eq(sender), eq(messages.getTpa().getWarnCanceller()), anyMap());
    }

    @Test
    @DisplayName("should ignore modes for tpahere requests")
    void shouldIgnoreModesForHere() {
        new TpaModeStore(() -> storage).setMode(receiverId, TpaMode.BLOCK_ALL);
        assertEquals(1, service.send(source, receiver, true));

        service.cancel(source, Optional.empty());
        new TpaModeStore(() -> storage).setMode(receiverId, TpaMode.ACCEPT_ALL);
        assertEquals(1, service.send(source, receiver, true));

        assertEquals(1, service.incoming(receiverId).size());
        verify(sender, never()).teleport(any(Location.class));
    }

    @Test
    @DisplayName("should nudge after three straight denies")
    void shouldNudgeAfterCancelStreak() {
        Player rival = player(UUID.randomUUID(), "Rival");
        online.put(rival.getUniqueId(), rival);
        Player third = player(UUID.randomUUID(), "Third");
        online.put(third.getUniqueId(), third);
        for (Player from : List.of(sender, rival, third)) {
            when(source.getSender()).thenReturn(from);
            service.send(source, receiver, false);
            when(source.getSender()).thenReturn(receiver);
            service.deny(source, Optional.empty());
        }

        verify(messenger).send(eq(receiver), eq(messages.getTpa().getNudgeCancel()), anyMap());
    }

    @Test
    @DisplayName("should nudge when one sender keeps requesting")
    void shouldNudgeHarassment() {
        for (int round = 0; round < 3; round++) {
            service.send(source, receiver, false);
            service.cancel(source, Optional.empty());
        }

        verify(messenger).send(eq(receiver), eq(messages.getTpa().getNudgeHarass()), anyMap());
    }

    @Test
    @DisplayName("should send the cancel nudge after the deny feedback")
    void shouldOrderCancelNudgeLast() {
        Player rival = player(UUID.randomUUID(), "Rival");
        online.put(rival.getUniqueId(), rival);
        Player third = player(UUID.randomUUID(), "Third");
        online.put(third.getUniqueId(), third);
        for (Player from : List.of(sender, rival, third)) {
            when(source.getSender()).thenReturn(from);
            service.send(source, receiver, false);
            when(source.getSender()).thenReturn(receiver);
            service.deny(source, Optional.empty());
        }

        var inOrder = inOrder(messenger);
        inOrder.verify(messenger, times(3))
                .send(eq(receiver), eq(messages.getTpa().getDenyReceiver()), anyMap());
        inOrder.verify(messenger).send(eq(receiver), eq(messages.getTpa().getNudgeCancel()), anyMap());
    }

    @Test
    @DisplayName("should send the harass nudge after the request notice")
    void shouldOrderHarassNudgeLast() {
        for (int round = 0; round < 3; round++) {
            service.send(source, receiver, false);
            service.cancel(source, Optional.empty());
        }

        var inOrder = inOrder(messenger);
        inOrder.verify(messenger, times(3))
                .send(eq(receiver), eq(messages.getTpa().getRequestReceived()), anyMap());
        inOrder.verify(messenger).send(eq(receiver), eq(messages.getTpa().getNudgeHarass()), anyMap());
    }

    @Test
    @DisplayName("should reject console senders")
    void shouldRejectConsole() {
        when(source.getSender()).thenReturn(mock(CommandSender.class));

        assertEquals(0, service.send(source, receiver, false));

        verify(messenger).send(any(CommandSender.class), eq(messages.getTpa().getPlayerOnly()), anyMap());
    }
}

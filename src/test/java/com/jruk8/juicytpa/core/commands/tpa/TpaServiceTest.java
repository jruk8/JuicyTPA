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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private SqliteStorage storage;
    private PluginConfig pluginConfig;
    private MessagesConfig messages;
    private SoundsConfig sounds;
    private TpaService service;
    private Player sender;
    private Player receiver;
    private final List<Runnable> scheduled = new ArrayList<>();
    private final Map<UUID, Player> online = new HashMap<>();

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("tpa-service-test"));
        assertTrue(storage.init());
        pluginConfig = new PluginConfig();
        messages = new MessagesConfig();
        sounds = new SoundsConfig();
        var ctx = new CommandContext(messages, sounds, messenger, soundPlayer);
        service = new TpaService(
                ctx, pluginConfig, new TpaModeStore(() -> storage), new TpaBlockStore(() -> storage),
                new TpaUsageTracker(() -> storage), plugin, this::schedule, online::get);
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
        lenient().when(entityScheduler.run(eq(plugin), any(Consumer.class), any())).thenAnswer(invocation -> {
            invocation.getArgument(1, Consumer.class).accept(mock(ScheduledTask.class));
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
        verify(messenger).send(eq(sender), eq(messages.getTpa().getRequestSent()), any(Map.class));
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getRequestReceived()), any(Map.class));
        verify(soundPlayer).play(receiver, sounds.getAsk());
    }

    @Test
    @DisplayName("should reject requests to self")
    void shouldRejectSelfRequests() {
        assertEquals(0, service.send(source, sender, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getSelfRequest()), any(Map.class));
        assertTrue(service.outgoing(senderId).isEmpty());
    }

    @Test
    @DisplayName("should reject a second outgoing request when concurrency is off")
    void shouldRejectSecondRequest() {
        Player other = player(UUID.randomUUID(), "Other");
        online.put(other.getUniqueId(), other);

        assertEquals(1, service.send(source, receiver, false));
        assertEquals(0, service.send(source, other, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getAlreadyPending()), any(Map.class));
    }

    @Test
    @DisplayName("should refuse receivers that block requests")
    void shouldRefuseBlockedReceivers() {
        new TpaModeStore(() -> storage).setMode(receiverId, TpaMode.BLOCK_ALL);

        assertEquals(0, service.send(source, receiver, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getReceiverBlocked()), any(Map.class));
        assertTrue(service.incoming(receiverId).isEmpty());
    }

    @Test
    @DisplayName("should auto-accept receivers in accept-all mode")
    void shouldAutoAccept() {
        new TpaModeStore(() -> storage).setMode(receiverId, TpaMode.ACCEPT_ALL);

        assertEquals(1, service.send(source, receiver, false));

        assertTrue(service.incoming(receiverId).isEmpty());
        verify(sender).teleport(receiver.getLocation());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getAcceptSender()), any(Map.class));
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
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getAcceptReceiver()), any(Map.class));
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
        verify(messenger).send(eq(sender), eq(messages.getTpa().getDenySender()), any(Map.class));
        verify(soundPlayer).play(sender, sounds.getReject());
    }

    @Test
    @DisplayName("should cancel the outgoing request")
    void shouldCancelOutgoing() {
        service.send(source, receiver, false);

        assertEquals(1, service.cancel(source, Optional.empty()));

        assertTrue(service.outgoing(senderId).isEmpty());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getCancelSender()), any(Map.class));
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getCancelReceiver()), any(Map.class));
    }

    @Test
    @DisplayName("should time requests out")
    void shouldExpireRequests() {
        service.send(source, receiver, false);
        scheduled.forEach(Runnable::run);

        assertTrue(service.incoming(receiverId).isEmpty());
        verify(messenger).send(eq(sender), eq(messages.getTpa().getTimeoutSender()), any(Map.class));
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getTimeoutReceiver()), any(Map.class));
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
                .send(eq(sender), eq(messages.getTpa().getModeSet()), any(Map.class));
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
        verify(messenger).send(eq(rival), eq(messages.getTpa().getAutoCancelled()), any(Map.class));
    }

    @Test
    @DisplayName("should refuse senders the receiver blocked")
    void shouldRefuseBlockedSenders() {
        new TpaBlockStore(() -> storage).setBlocked(receiverId, senderId, true);

        assertEquals(0, service.send(source, receiver, false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getRequestBlocked()), any(Map.class));
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
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getBlockAdded()), any(Map.class));
        verify(messenger).send(eq(sender), eq(messages.getTpa().getAutoCancelled()), any(Map.class));

        assertEquals(1, service.block(source, sender));
        assertFalse(new TpaBlockStore(() -> storage).isBlocked(receiverId, senderId));
        verify(messenger).send(eq(receiver), eq(messages.getTpa().getBlockRemoved()), any(Map.class));
    }

    @Test
    @DisplayName("should reject blocking self")
    void shouldRejectSelfBlock() {
        assertEquals(0, service.block(source, sender));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getBlockSelf()), any(Map.class));
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
        verify(messenger).send(eq(sender), eq(messages.getTpa().getTpallSent()), any(Map.class));
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
    @DisplayName("should gate bulk sends on concurrency")
    void shouldGateBulkOnConcurrency() {
        assertEquals(0, service.sendBulk(source, List.of(receiver), false));

        verify(messenger).send(eq(sender), eq(messages.getTpa().getNeedsConcurrency()), any(Map.class));
        assertTrue(service.incoming(receiverId).isEmpty());
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
    @DisplayName("should reject console senders")
    void shouldRejectConsole() {
        when(source.getSender()).thenReturn(mock(CommandSender.class));

        assertEquals(0, service.send(source, receiver, false));

        verify(messenger).send(any(CommandSender.class), eq(messages.getTpa().getPlayerOnly()), any(Map.class));
    }
}

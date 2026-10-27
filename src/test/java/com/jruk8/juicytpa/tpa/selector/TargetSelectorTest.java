package com.jruk8.juicytpa.tpa.selector;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.message.MessagesConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetSelectorTest {

    private static ServerMock server;

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    @Mock
    private CommandContext ctx;

    @Mock
    private CommandSourceStack source;

    @Mock
    private PlayerSelectorArgumentResolver selector;

    @Captor
    private ArgumentCaptor<Function<MessagesConfig, String>> messageCaptor;

    private Player player(UUID id) {
        Player player = mock(Player.class);
        lenient().when(player.getUniqueId()).thenReturn(id);
        return player;
    }

    private String selectedMessage(Function<MessagesConfig, String> messageSelector) {
        MessagesConfig messages = mock(MessagesConfig.class);
        MessagesConfig.Core core = mock(MessagesConfig.Core.class);
        MessagesConfig.Tpa tpa = mock(MessagesConfig.Tpa.class);
        lenient().when(messages.getCore()).thenReturn(core);
        lenient().when(messages.getTpa()).thenReturn(tpa);
        lenient().when(core.getNoTargets()).thenReturn("none");
        lenient().when(tpa.getNotSingle()).thenReturn("single");
        lenient().when(tpa.getSelectorUnsupported()).thenReturn("unsupported");
        return messageSelector.apply(messages);
    }

    @Test
    @DisplayName("should send no-targets when the selector resolves to nobody")
    void shouldSendNoTargetsForEmptyResolution() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        when(selector.resolve(source)).thenReturn(List.of());

        var target = TargetSelector.single(source, selector, ctx);

        assertTrue(target.isEmpty());
        verify(ctx).error(eq(source), messageCaptor.capture(), anyMap());
        assertEquals("none", selectedMessage(messageCaptor.getValue()));
    }

    @Test
    @DisplayName("should send no-targets when the selector fails to parse")
    void shouldSendNoTargetsForSyntaxError() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        when(selector.resolve(source)).thenThrow(mock(CommandSyntaxException.class));

        var target = TargetSelector.single(source, selector, ctx);

        assertTrue(target.isEmpty());
        verify(ctx).error(eq(source), messageCaptor.capture(), anyMap());
        assertEquals("none", selectedMessage(messageCaptor.getValue()));
    }

    @Test
    @DisplayName("should send not-single when the selector resolves to several players")
    void shouldSendNotSingleForMultiResolution() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        List<Player> pair = List.of(player(UUID.randomUUID()), player(UUID.randomUUID()));
        when(selector.resolve(source)).thenReturn(pair);

        var target = TargetSelector.single(source, selector, ctx);

        assertTrue(target.isEmpty());
        verify(ctx).error(eq(source), messageCaptor.capture(), anyMap());
        assertEquals("single", selectedMessage(messageCaptor.getValue()));
    }

    @Test
    @DisplayName("should resolve several players when multi is allowed")
    void shouldResolveSeveralWhenMultiAllowed() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        List<Player> pair = List.of(player(UUID.randomUUID()), player(UUID.randomUUID()));
        when(selector.resolve(source)).thenReturn(pair);

        var resolved = TargetSelector.group(source, selector, true, ctx);

        assertTrue(resolved.isPresent());
        assertEquals(pair, resolved.get());
    }

    @Test
    @DisplayName("should send not-single when multi is off")
    void shouldSendNotSingleWhenMultiOff() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        List<Player> pair = List.of(player(UUID.randomUUID()), player(UUID.randomUUID()));
        when(selector.resolve(source)).thenReturn(pair);

        var resolved = TargetSelector.group(source, selector, false, ctx);

        assertTrue(resolved.isEmpty());
        verify(ctx).error(eq(source), messageCaptor.capture(), anyMap());
        assertEquals("single", selectedMessage(messageCaptor.getValue()));
    }

    @Test
    @DisplayName("should reject at-s selectors")
    void shouldRejectAtSelf() throws Exception {
        var captured = mock(PlayerSelectorArgument.Captured.class);
        when(captured.rawInput()).thenReturn("@s");
        lenient().when(source.getSender()).thenReturn(mock(Player.class));

        var resolved = TargetSelector.single(source, captured, ctx);

        assertTrue(resolved.isEmpty());
        verify(ctx).error(eq(source), messageCaptor.capture(), anyMap());
        assertEquals("unsupported", selectedMessage(messageCaptor.getValue()));
        verify(captured, never()).resolve(source);
    }

    @Test
    @DisplayName("should reject at-p selectors")
    void shouldRejectAtNearest() throws Exception {
        var captured = mock(PlayerSelectorArgument.Captured.class);
        when(captured.rawInput()).thenReturn("@p");
        lenient().when(source.getSender()).thenReturn(mock(Player.class));

        var resolved = TargetSelector.single(source, captured, ctx);

        assertTrue(resolved.isEmpty());
        verify(ctx).error(eq(source), messageCaptor.capture(), anyMap());
        assertEquals("unsupported", selectedMessage(messageCaptor.getValue()));
    }

    @Test
    @DisplayName("should re-roll random self picks while others are online")
    void shouldRerollRandomSelf() throws Exception {
        var self = server.addPlayer("Self");
        var alex = server.addPlayer("Alex");
        try {
            var captured = mock(PlayerSelectorArgument.Captured.class);
            when(captured.rawInput()).thenReturn("@r");
            when(captured.resolve(source)).thenReturn(List.of(self));
            when(source.getSender()).thenReturn(self);

            var resolved = TargetSelector.single(source, captured, ctx);

            assertTrue(resolved.isPresent());
            assertEquals(alex, resolved.get());
        } finally {
            self.disconnect();
            alex.disconnect();
        }
    }

    @Test
    @DisplayName("should keep random self picks when alone online")
    void shouldKeepRandomSelfWhenAlone() throws Exception {
        var self = server.addPlayer("Self");
        try {
            var captured = mock(PlayerSelectorArgument.Captured.class);
            when(captured.rawInput()).thenReturn("@r");
            when(captured.resolve(source)).thenReturn(List.of(self));
            when(source.getSender()).thenReturn(self);

            var resolved = TargetSelector.single(source, captured, ctx);

            assertTrue(resolved.isPresent());
            assertEquals(self, resolved.get());
        } finally {
            self.disconnect();
        }
    }

    @Test
    @DisplayName("should resolve the single player")
    void shouldResolveSinglePlayer() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        Player target = player(UUID.randomUUID());
        when(selector.resolve(source)).thenReturn(List.of(target));

        var resolved = TargetSelector.single(source, selector, ctx);

        assertTrue(resolved.isPresent());
        assertEquals(target, resolved.get());
    }
}

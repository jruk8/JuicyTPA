package com.jruk8.juicytpa.core.commands.selectors;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.messages.MessagesConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TargetSelectorTest {

    @Mock
    private CommandContext ctx;

    @Mock
    private CommandSourceStack source;

    @Mock
    private PlayerSelectorArgumentResolver selector;

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
        return messageSelector.apply(messages);
    }

    @Test
    @DisplayName("should send no-targets when the selector resolves to nobody")
    void shouldSendNoTargetsForEmptyResolution() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        when(selector.resolve(source)).thenReturn(List.of());

        var target = TargetSelector.single(source, selector, ctx);

        assertTrue(target.isEmpty());
        var captor = ArgumentCaptor.forClass(Function.class);
        verify(ctx).error(eq(source), captor.capture(), any(Map.class));
        assertEquals("none", selectedMessage(captor.getValue()));
    }

    @Test
    @DisplayName("should send no-targets when the selector fails to parse")
    void shouldSendNoTargetsForSyntaxError() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        when(selector.resolve(source)).thenThrow(mock(CommandSyntaxException.class));

        var target = TargetSelector.single(source, selector, ctx);

        assertTrue(target.isEmpty());
        var captor = ArgumentCaptor.forClass(Function.class);
        verify(ctx).error(eq(source), captor.capture(), any(Map.class));
        assertEquals("none", selectedMessage(captor.getValue()));
    }

    @Test
    @DisplayName("should send not-single when the selector resolves to several players")
    void shouldSendNotSingleForMultiResolution() throws Exception {
        lenient().when(source.getSender()).thenReturn(mock(Player.class));
        List<Player> pair = List.of(player(UUID.randomUUID()), player(UUID.randomUUID()));
        when(selector.resolve(source)).thenReturn(pair);

        var target = TargetSelector.single(source, selector, ctx);

        assertTrue(target.isEmpty());
        var captor = ArgumentCaptor.forClass(Function.class);
        verify(ctx).error(eq(source), captor.capture(), any(Map.class));
        assertEquals("single", selectedMessage(captor.getValue()));
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

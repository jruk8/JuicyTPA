package com.jruk8.juicytpa.tpa.command;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.command.Permissions;
import com.jruk8.juicytpa.command.user.help.HelpCommand;
import com.jruk8.juicytpa.message.MessagesConfig;
import com.jruk8.juicytpa.tpa.TpaService;
import com.jruk8.juicytpa.tpa.selector.PlayerSelectorArgument;
import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TpaSendCommandTest {

    private static ServerMock server;

    @Mock
    private CommandContext ctx;

    @Mock
    private TpaService service;

    @Mock
    private HelpCommand help;

    @Mock
    private CommandSourceStack source;

    @Captor
    private ArgumentCaptor<Function<MessagesConfig, String>> selectorCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> tagsCaptor;

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    private Player sender() {
        Player sender = mock(Player.class);
        when(source.getSender()).thenReturn(sender);
        return sender;
    }

    private TpaSendCommand command() {
        return new TpaSendCommand(ctx, service, Permissions.TPA, false, help);
    }

    private PlayerSelectorArgumentResolver selector(String rawInput) {
        return new PlayerSelectorArgument.Captured(mock(PlayerSelectorArgumentResolver.class), rawInput);
    }

    private CommandDispatcher<CommandSourceStack> dispatcher() {
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        dispatcher.register(command().node("tpa"));
        return dispatcher;
    }

    @Test
    @Order(1)
    @DisplayName("should show help for the help selector")
    void shouldShowHelp() {
        Player sender = sender();
        when(sender.hasPermission(Permissions.HELP)).thenReturn(true);

        int result = command().execute(source, selector("help"));

        assertEquals(1, result);
        verify(help).showHelp(source, null);
        verify(service, never()).send(any(), any(), anyBoolean());
    }

    @Test
    @Order(2)
    @DisplayName("should send to a named player")
    void shouldSendToPlayer() throws Exception {
        Player sender = sender();
        when(sender.hasPermission(Permissions.TPALL)).thenReturn(false);
        Player bob = mock(Player.class);
        var resolver = mock(PlayerSelectorArgumentResolver.class);
        when(resolver.resolve(source)).thenReturn(List.of(bob));

        command().execute(source, new PlayerSelectorArgument.Captured(resolver, "Bob"));

        var targetCaptor = ArgumentCaptor.forClass(Player.class);
        verify(service).send(eq(source), targetCaptor.capture(), eq(false));
        assertSame(bob, targetCaptor.getValue());
        verify(help, never()).showHelp(any(), any());
    }

    @Test
    @Order(3)
    @DisplayName("should deny the help selector without the help permission")
    void shouldDenyHelpWithoutPermission() {
        Player sender = sender();
        when(sender.hasPermission(Permissions.HELP)).thenReturn(false);

        int result = command().execute(source, selector("help"));

        assertEquals(0, result);
        verify(ctx).error(eq(source), selectorCaptor.capture(), tagsCaptor.capture());
        assertEquals(Permissions.HELP, tagsCaptor.getValue().get("permission"));
        var messages = new MessagesConfig();
        assertEquals(messages.getCore().getNoPermission(), selectorCaptor.getValue().apply(messages));
        verify(help, never()).showHelp(any(), any());
    }

    @Test
    @Order(4)
    @DisplayName("should suggest help first")
    void shouldSuggestHelpFirst() throws Exception {
        Player sender = sender();
        lenient().when(sender.hasPermission(Permissions.TPA)).thenReturn(true);
        when(sender.hasPermission(Permissions.HELP)).thenReturn(true);
        when(sender.hasPermission(Permissions.TPALL)).thenReturn(false);
        when(sender.getName()).thenReturn("Alex");

        var dispatcher = dispatcher();
        var suggestions = dispatcher
                .getCompletionSuggestions(dispatcher.parse("tpa h", source))
                .join()
                .getList();

        assertFalse(suggestions.isEmpty());
        assertEquals("help", suggestions.getFirst().getText());
    }

    @Test
    @Order(5)
    @DisplayName("should hide the help suggestion without the help permission")
    void shouldHideHelpSuggestion() throws Exception {
        Player sender = sender();
        lenient().when(sender.hasPermission(Permissions.TPA)).thenReturn(true);
        when(sender.hasPermission(Permissions.HELP)).thenReturn(false);
        when(sender.hasPermission(Permissions.TPALL)).thenReturn(false);
        when(sender.getName()).thenReturn("Alex");

        var dispatcher = dispatcher();
        var suggestions = dispatcher
                .getCompletionSuggestions(dispatcher.parse("tpa h", source))
                .join()
                .getList();

        assertTrue(suggestions.stream().noneMatch(suggestion -> suggestion.getText().equals("help")));
    }

    @Test
    @Order(6)
    @DisplayName("should target a player named help over the help listing")
    void shouldPreferPlayerNamedHelp() throws Exception {
        Player sender = sender();
        when(sender.hasPermission(Permissions.TPALL)).thenReturn(false);
        Player helpPlayer = server.addPlayer("help");
        var resolver = mock(PlayerSelectorArgumentResolver.class);
        when(resolver.resolve(source)).thenReturn(List.of(helpPlayer));

        command().execute(source, new PlayerSelectorArgument.Captured(resolver, "help"));

        var targetCaptor = ArgumentCaptor.forClass(Player.class);
        verify(service).send(eq(source), targetCaptor.capture(), eq(false));
        assertSame(helpPlayer, targetCaptor.getValue());
        verify(help, never()).showHelp(any(), any());
    }
}

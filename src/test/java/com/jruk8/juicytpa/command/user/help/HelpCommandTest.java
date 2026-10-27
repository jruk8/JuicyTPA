package com.jruk8.juicytpa.command.user.help;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.message.MessagesConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpCommandTest {

    @Mock
    private CommandContext ctx;

    @Mock
    private CommandSourceStack source;

    @Mock
    private CommandSender sender;

    @Captor
    private ArgumentCaptor<Function<MessagesConfig, String>> selectorCaptor;

    private final MessagesConfig messages = new MessagesConfig();

    private HelpCommand command;

    @BeforeEach
    void setUp() {
        command = new HelpCommand(ctx, "1.0.0");
        when(source.getSender()).thenReturn(sender);
        when(ctx.messagesFor(sender)).thenReturn(messages);
    }

    @Test
    @DisplayName("should list every visible entry without a query")
    void shouldListEntries() {
        when(sender.hasPermission(anyString())).thenReturn(true);

        command.showHelp(source, null);

        verify(ctx).playHelp(source);
        List<String> resolved = resolved(17);
        assertEquals(messages.getCore().getHelpHeader(), resolved.get(0));
        assertEquals(16, resolved.stream()
                .filter(messages.getCore().getHelpEntryFormat()::equals)
                .count());
    }

    @Test
    @DisplayName("should report a query matching no commands")
    void shouldReportUnmatchedQuery() {
        command.showHelp(source, "xyzzy");

        List<String> resolved = resolved(2);
        assertEquals(messages.getCore().getHelpHeader(), resolved.get(0));
        assertEquals(messages.getCore().getHelpNoMatch(), resolved.get(1));
    }

    @Test
    @DisplayName("should report entries hidden by permission")
    void shouldReportHiddenEntries() {
        command.showHelp(source, "trust");

        List<String> resolved = resolved(2);
        assertEquals(messages.getCore().getHelpHeader(), resolved.get(0));
        assertEquals(messages.getCore().getHelpNoCommands(), resolved.get(1));
        assertFalse(resolved.contains(messages.getCore().getHelpNoMatch()));
    }

    @Test
    @DisplayName("should always show the help entry itself")
    void shouldAlwaysShowHelpEntry() {
        command.showHelp(source, null);

        List<String> resolved = resolved(2);
        assertEquals(messages.getCore().getHelpHeader(), resolved.get(0));
        assertEquals(messages.getCore().getHelpEntryFormat(), resolved.get(1));
        assertTrue(resolved.stream().noneMatch(messages.getCore().getHelpNoCommands()::equals));
    }

    private List<String> resolved(int expectedCalls) {
        verify(ctx, times(expectedCalls)).sendMessage(eq(source), selectorCaptor.capture(), anyMap());
        return selectorCaptor.getAllValues().stream()
                .map(selector -> selector.apply(messages))
                .toList();
    }
}

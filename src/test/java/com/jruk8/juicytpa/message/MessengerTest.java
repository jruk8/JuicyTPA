package com.jruk8.juicytpa.message;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessengerTest {

    @Mock
    private MessageParser messageParser;

    @Mock
    private MessageBundles bundles;

    @Mock
    private CommandSender sender;

    private Messenger messenger;

    @BeforeEach
    void setUp() {
        messenger = new Messenger(messageParser, bundles);
    }

    private Map<String, String> withLinks(Map<String, String> tags) {
        Map<String, String> merged = new HashMap<>(
                PluginLinks.buttons(new MessagesConfig()));
        merged.putAll(tags);
        return merged;
    }

    @Nested
    @DisplayName("send() guard clauses")
    class GuardClauseTests {

        @Test
        @DisplayName("should do nothing when sender is null")
        void shouldDoNothingWhenSenderIsNull() {
            messenger.send(null, "Hello", Map.of());
            verifyNoInteractions(messageParser);
        }

        @Test
        @DisplayName("should do nothing when rawMessage is null")
        void shouldDoNothingWhenMessageIsNull() {
            messenger.send(sender, null, Map.of());
            verifyNoInteractions(messageParser, sender);
        }

        @Test
        @DisplayName("should do nothing when rawMessage is empty")
        void shouldDoNothingWhenMessageIsEmpty() {
            messenger.send(sender, "", Map.of());
            verifyNoInteractions(messageParser, sender);
        }
    }

    @Nested
    @DisplayName("send() delegation")
    class DelegationTests {

        @Test
        @DisplayName("should parse and send with string placeholders")
        void shouldParseAndSendWithStringPlaceholders() {
            String rawMessage = "{prefix}Hello {player}";
            Map<String, String> placeholders = Map.of("player", "Steve");
            Component parsed = Component.text("Hello Steve");

            when(bundles.configFor(sender)).thenReturn(new MessagesConfig());
            when(messageParser.parse(eq(rawMessage), eq(withLinks(placeholders))))
                .thenReturn(parsed);

            messenger.send(sender, rawMessage, placeholders);

            verify(messageParser, times(1))
                .parse(eq(rawMessage), eq(withLinks(placeholders)));
            verify(sender, times(1)).sendMessage(parsed);
        }

        @Test
        @DisplayName("should merge link buttons into the parsed placeholders")
        void shouldMergeLinkButtons() {
            var captor = ArgumentCaptor.forClass(Map.class);
            when(bundles.configFor(sender)).thenReturn(new MessagesConfig());
            when(messageParser.parse(eq("{version}"), anyMap())).thenReturn(Component.text("v"));

            messenger.send(sender, "{version}", Map.of("player", "Steve"));

            verify(messageParser).parse(eq("{version}"), captor.capture());
            Map<?, ?> merged = captor.getValue();
            assertEquals("Steve", merged.get("player"));
            assertTrue(merged.containsKey("{modrinth}"));
            assertTrue(merged.containsKey("{github}"));
            assertTrue(merged.containsKey("{discord}"));
            assertTrue(merged.containsKey("{donate}"));
        }

        @Test
        @DisplayName("should parse and send with no placeholders")
        void shouldParseAndSendWithNoPlaceholders() {
            String rawMessage = "{prefix}Hello";
            Component parsed = Component.text("Hello");

            when(bundles.configFor(sender)).thenReturn(new MessagesConfig());
            when(messageParser.parse(eq(rawMessage), eq(withLinks(Map.of()))))
                .thenReturn(parsed);

            messenger.send(sender, rawMessage);

            verify(messageParser, times(1))
                .parse(eq(rawMessage), eq(withLinks(Map.of())));
            verify(sender, times(1)).sendMessage(parsed);
        }
    }
}

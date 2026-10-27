package com.jruk8.juicytpa.message;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageBundlesTest {

    private static ServerMock server;

    private final MessagesConfig english = new MessagesConfig();
    private final Map<UUID, String> pins = new HashMap<>();

    @BeforeAll
    static void startServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    private MessageBundles bundles(@TempDir Path dir) {
        return bundles(dir, "auto");
    }

    private MessageBundles bundles(Path dir, String defaultLanguage) {
        return new MessageBundles(
                english, pins::get, dir.toFile(), Logger.getLogger("bundles-test"), () -> defaultLanguage);
    }

    private Player player(UUID id, String locale) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        when(player.locale()).thenReturn(locale == null ? null : Locale.forLanguageTag(locale));
        return player;
    }

    @Test
    @DisplayName("should read English for non-players")
    void shouldReadEnglishForConsole(@TempDir Path dir) {
        assertSame(english, bundles(dir).configFor(mock(CommandSender.class)));
        assertSame(english, bundles(dir).english());
    }

    @Test
    @DisplayName("should prefer the pin over the client locale")
    void shouldPreferPin(@TempDir Path dir) {
        var id = UUID.randomUUID();
        pins.put(id, "fi");

        MessagesConfig bundle = bundles(dir).configFor(player(id, "de-DE"));

        assertNotSame(english, bundle);
        assertTrue(new File(dir.toFile(), "fi.yml").exists());
    }

    @Test
    @DisplayName("should follow the client locale without a pin")
    void shouldFollowClientLocale(@TempDir Path dir) {
        var id = UUID.randomUUID();

        assertNotSame(english, bundles(dir).configFor(player(id, "fi-FI")));
        assertSame(english, bundles(dir).configFor(player(id, null)));
        assertSame(english, bundles(dir).configFor(player(id, "xx-YY")));
    }

    @Test
    @DisplayName("should fall back to English on unknown codes")
    void shouldFallBackToEnglish(@TempDir Path dir) {
        var bundles = bundles(dir);

        assertSame(english, bundles.configForCode("en"));
        assertSame(english, bundles.configForCode("xx"));
        assertSame(english, bundles.configForCode(null));
    }

    @Test
    @DisplayName("should seed translations from the bundled resources")
    void shouldSeedFromResources(@TempDir Path dir) {
        MessagesConfig bundle = bundles(dir).configForCode("fi");

        assertNotSame(english, bundle);
        assertTrue(Files.exists(dir.resolve("fi.yml")));
    }

    @Test
    @DisplayName("should overlay partial files over the English schema")
    void shouldOverlayPartialFiles(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("de.yml"), "tpa:\n  requestSent: \"CUSTOM\"\n");

        MessagesConfig bundle = bundles(dir).configForCode("de");

        assertEquals("CUSTOM", bundle.getTpa().getRequestSent());
        assertEquals(english.getTpa().getRequestReceived(), bundle.getTpa().getRequestReceived());
    }

    @Test
    @DisplayName("should cache bundles until reload")
    void shouldCacheUntilReload(@TempDir Path dir) {
        var bundles = bundles(dir);

        assertSame(bundles.configForCode("fi"), bundles.configForCode("fi"));

        bundles.reload();

        assertNotSame(english, bundles.configForCode("fi"));
    }

    @Test
    @DisplayName("should use the server default without a pin")
    void shouldUseServerDefault(@TempDir Path dir) {
        var id = UUID.randomUUID();
        var bundles = bundles(dir, "fi");

        MessagesConfig bundle = bundles.configFor(player(id, "de-DE"));

        assertSame(bundles.configForCode("fi"), bundle);
    }

    @Test
    @DisplayName("should prefer the pin over the server default")
    void shouldPreferPinOverServerDefault(@TempDir Path dir) {
        var id = UUID.randomUUID();
        pins.put(id, "de");
        var bundles = bundles(dir, "fi");

        assertSame(bundles.configForCode("de"), bundles.configFor(player(id, "fi-FI")));
    }

    @Test
    @DisplayName("should follow the client on an unknown server default")
    void shouldFollowClientOnUnknownDefault(@TempDir Path dir) {
        var id = UUID.randomUUID();
        var bundles = bundles(dir, "xx");

        assertSame(bundles.configForCode("fi"), bundles.configFor(player(id, "fi-FI")));
    }

    @Test
    @DisplayName("should read English only in test mode")
    void shouldReadEnglishOnly() {
        var bundles = new MessageBundles(english);

        assertSame(english, bundles.configFor(player(UUID.randomUUID(), "fi-FI")));
        assertSame(english, bundles.configForCode("fi"));
    }
}

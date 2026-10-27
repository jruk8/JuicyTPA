package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.storage.SqliteStorage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLanguageTest {

    private final UUID player = UUID.randomUUID();

    private SqliteStorage storage;
    private PlayerLanguage languages;

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("lang-test"));
        assertTrue(storage.init());
        languages = new PlayerLanguage(new TpaLangStore(() -> storage));
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    @DisplayName("should follow the client until pinned")
    void shouldFollowClientByDefault() {
        assertNull(languages.get(player));
    }

    @Test
    @DisplayName("should pin, survive a cache miss, and clear again")
    void shouldPinAndClear() {
        languages.set(player, "fi");

        assertEquals("fi", languages.get(player));
        assertEquals(
                "fi",
                new PlayerLanguage(new TpaLangStore(() -> storage)).get(player));

        languages.clear(player);

        assertNull(languages.get(player));
        assertEquals(Optional.empty(), new TpaLangStore(() -> storage).language(player));
    }
}

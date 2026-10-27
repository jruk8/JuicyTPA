package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.storage.SqliteStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpaModeStoreTest {

    private final UUID player = UUID.randomUUID();

    private SqliteStorage storage;
    private TpaModeStore modes;

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("mode-test"));
        assertTrue(storage.init());
        modes = new TpaModeStore(() -> storage);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    @DisplayName("should default to normal")
    void shouldDefaultToNormal() {
        assertEquals(TpaMode.NORMAL, modes.mode(player));
    }

    @Test
    @DisplayName("should persist the mode")
    void shouldPersistMode() {
        modes.setMode(player, TpaMode.BLOCK_ALL);

        assertEquals(TpaMode.BLOCK_ALL, modes.mode(player));

        modes.setMode(player, TpaMode.ACCEPT_ALL);

        assertEquals(TpaMode.ACCEPT_ALL, modes.mode(player));
    }

    @Test
    @DisplayName("should read unknown ordinals as normal")
    void shouldFallBackToNormal() {
        storage.setPlayerValue(TpaModeStore.MODE_KEY, player, 99);

        assertEquals(TpaMode.NORMAL, modes.mode(player));
    }
}

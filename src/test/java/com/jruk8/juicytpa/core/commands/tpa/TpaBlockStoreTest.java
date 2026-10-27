package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.storage.SqliteStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpaBlockStoreTest {

    private final UUID blocker = UUID.randomUUID();
    private final UUID blocked = UUID.randomUUID();

    private SqliteStorage storage;
    private TpaBlockStore blocks;

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("block-test"));
        assertTrue(storage.init());
        blocks = new TpaBlockStore(() -> storage);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    @DisplayName("should start unblocked")
    void shouldStartUnblocked() {
        assertFalse(blocks.isBlocked(blocker, blocked));
    }

    @Test
    @DisplayName("should persist blocks per pair")
    void shouldPersistBlocks() {
        blocks.setBlocked(blocker, blocked, true);

        assertTrue(blocks.isBlocked(blocker, blocked));
        assertFalse(blocks.isBlocked(blocked, blocker));

        blocks.setBlocked(blocker, blocked, false);

        assertFalse(blocks.isBlocked(blocker, blocked));
    }
}

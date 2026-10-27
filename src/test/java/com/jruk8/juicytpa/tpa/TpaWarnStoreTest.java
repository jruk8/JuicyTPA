package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.storage.SqliteStorage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpaWarnStoreTest {

    private final UUID player = UUID.randomUUID();

    private SqliteStorage storage;
    private TpaWarnStore stats;

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("warn-store-test"));
        assertTrue(storage.init());
        stats = new TpaWarnStore(() -> storage);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    @DisplayName("should default every counter to zero")
    void shouldDefaultToZero() {
        assertEquals(0, stats.unanswered(player));
        assertEquals(0, stats.pendingSeconds(player));
        assertEquals(0, stats.denied(player));
        assertEquals(0, stats.deniedSeconds(player));
    }

    @Test
    @DisplayName("should accumulate expiries and their pending time")
    void shouldRecordExpiries() {
        stats.recordExpiry(player, 60);
        stats.recordExpiry(player, 30);

        assertEquals(2, stats.unanswered(player));
        assertEquals(90, stats.pendingSeconds(player));
    }

    @Test
    @DisplayName("should accumulate denies and their pending time")
    void shouldRecordDenies() {
        stats.recordDenied(player, 20);
        stats.recordDenied(player, 10);

        assertEquals(2, stats.denied(player));
        assertEquals(30, stats.deniedSeconds(player));
    }

    @Test
    @DisplayName("should accumulate window seconds without touching counts")
    void shouldAddPendingSeconds() {
        stats.addPendingSeconds(player, 45);
        stats.addPendingSeconds(player, 15);

        assertEquals(60, stats.pendingSeconds(player));
        assertEquals(0, stats.unanswered(player));
        assertEquals(0, stats.denied(player));
    }

    @Test
    @DisplayName("should reset unanswered statistics without touching denies")
    void shouldResetUnanswered() {
        stats.recordExpiry(player, 60);
        stats.recordDenied(player, 30);
        stats.resetUnanswered(player);

        assertEquals(0, stats.unanswered(player));
        assertEquals(0, stats.pendingSeconds(player));
        assertEquals(1, stats.denied(player));
        assertEquals(30, stats.deniedSeconds(player));
    }

    @Test
    @DisplayName("should reset denied statistics without touching expiries")
    void shouldResetDenied() {
        stats.recordExpiry(player, 60);
        stats.recordDenied(player, 30);
        stats.resetDenied(player);

        assertEquals(0, stats.denied(player));
        assertEquals(0, stats.deniedSeconds(player));
        assertEquals(1, stats.unanswered(player));
        assertEquals(60, stats.pendingSeconds(player));
    }
}

package com.jruk8.juicytpa.core.placeholders;

import com.jruk8.juicytpa.core.storage.SqliteStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpaUsageTrackerTest {

    private final UUID player = UUID.randomUUID();

    private SqliteStorage storage;
    private TpaUsageTracker tracker;

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("tracker-test"));
        assertTrue(storage.init());
        tracker = new TpaUsageTracker(() -> storage);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    @DisplayName("should start at zero")
    void shouldStartAtZero() {
        assertEquals(0, tracker.playerSent(player));
        assertEquals(0, tracker.playerReceived(player));
        assertEquals(0, tracker.globalSent());
        assertEquals(0, tracker.globalReceived());
    }

    @Test
    @DisplayName("should record one sent request per execution")
    void shouldRecordSent() {
        tracker.recordSent(player);

        assertEquals(1, tracker.playerSent(player));
        assertEquals(1, tracker.globalSent());
    }

    @Test
    @DisplayName("should accumulate sent requests per player over time")
    void shouldAccumulateSent() {
        tracker.recordSent(player);
        tracker.recordSent(player);
        tracker.recordSent(UUID.randomUUID());

        assertEquals(2, tracker.playerSent(player));
        assertEquals(3, tracker.globalSent());
    }

    @Test
    @DisplayName("should track received separately from sent")
    void shouldTrackReceivedSeparately() {
        tracker.recordReceived(player);
        tracker.recordReceived(player);

        assertEquals(2, tracker.playerReceived(player));
        assertEquals(2, tracker.globalReceived());
        assertEquals(0, tracker.playerSent(player));
        assertEquals(0, tracker.globalSent());
    }
}

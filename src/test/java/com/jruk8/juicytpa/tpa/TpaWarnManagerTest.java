package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.storage.SqliteStorage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.jruk8.juicytpa.api.TpaRequest;

class TpaWarnManagerTest {

    private final UUID sender = UUID.randomUUID();
    private final UUID receiver = UUID.randomUUID();
    private final AtomicLong clock = new AtomicLong();

    private SqliteStorage storage;
    private TpaWarnStore stats;
    private TpaWarnManager warns;

    @BeforeEach
    void setUp() {
        storage = new SqliteStorage("jdbc:sqlite::memory:", Logger.getLogger("warn-manager-test"));
        assertTrue(storage.init());
        clock.set(1_000_000L);
        stats = new TpaWarnStore(() -> storage);
        warns = new TpaWarnManager(stats, new PluginConfig(), clock::get);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    private TpaRequest request() {
        return new TpaRequest(sender, "Sender", receiver, "Receiver", false, clock.get());
    }

    @Test
    @DisplayName("should stay silent below both thresholds")
    void shouldStaySilentBelowThresholds() {
        stats.recordExpiry(receiver, 60);
        stats.recordExpiry(receiver, 60);

        assertEquals(Optional.empty(), warns.warnKind(receiver));
    }

    @Test
    @DisplayName("should need pending time as well as unanswered count")
    void shouldNeedPendingTimeForAfk() {
        stats.recordExpiry(receiver, 0);
        stats.recordExpiry(receiver, 0);
        stats.recordExpiry(receiver, 0);

        assertEquals(Optional.empty(), warns.warnKind(receiver));
    }

    @Test
    @DisplayName("should flag AFK past both thresholds")
    void shouldFlagAfk() {
        stats.recordExpiry(receiver, 60);
        stats.recordExpiry(receiver, 60);
        stats.recordExpiry(receiver, 60);

        assertEquals(Optional.of(TpaWarnManager.WarnKind.AFK), warns.warnKind(receiver));
    }

    @Test
    @DisplayName("should flag cancellers past both thresholds")
    void shouldFlagCancellers() {
        stats.recordDenied(receiver, 30);
        stats.recordDenied(receiver, 30);
        stats.recordDenied(receiver, 30);

        assertEquals(Optional.of(TpaWarnManager.WarnKind.CANCELLER), warns.warnKind(receiver));
    }

    @Test
    @DisplayName("should prefer AFK when both thresholds cross")
    void shouldPreferAfk() {
        stats.recordExpiry(receiver, 60);
        stats.recordExpiry(receiver, 60);
        stats.recordExpiry(receiver, 60);
        stats.recordDenied(receiver, 30);
        stats.recordDenied(receiver, 30);
        stats.recordDenied(receiver, 30);

        assertEquals(Optional.of(TpaWarnManager.WarnKind.AFK), warns.warnKind(receiver));
    }

    @Test
    @DisplayName("should consume a confirmation only once")
    void shouldConsumeConfirmations() {
        warns.noteWarned(sender, receiver, false);

        assertTrue(warns.confirm(sender, receiver, false));
        assertFalse(warns.confirm(sender, receiver, false));
    }

    @Test
    @DisplayName("should expire confirmations past the window")
    void shouldExpireConfirmations() {
        warns.noteWarned(sender, receiver, false);
        clock.addAndGet(16_000L);

        assertFalse(warns.confirm(sender, receiver, false));
    }

    @Test
    @DisplayName("should keep confirmations apart per request type")
    void shouldSplitConfirmationsByType() {
        warns.noteWarned(sender, receiver, false);

        assertFalse(warns.confirm(sender, receiver, true));
        assertTrue(warns.confirm(sender, receiver, false));
    }

    @Test
    @DisplayName("should count overlapping pending requests once per second")
    void shouldCountOverlappingPendingOnce() {
        warns.delivered(request());
        clock.addAndGet(10_000L);
        warns.delivered(request());
        clock.addAndGet(10_000L);
        warns.removed(request());
        clock.addAndGet(10_000L);
        warns.removed(request());

        assertEquals(30, stats.pendingSeconds(receiver));
    }

    @Test
    @DisplayName("should ignore removals without an open window")
    void shouldIgnoreOrphanRemovals() {
        warns.removed(request());

        assertEquals(0, stats.pendingSeconds(receiver));
    }

    @Test
    @DisplayName("should nudge after three straight denies")
    void shouldNudgeCancelStreak() {
        warns.denied(request());
        warns.denied(request());
        assertEquals(Optional.empty(), warns.nudgeForCancel(receiver));

        warns.denied(request());

        assertEquals(Optional.of(TpaWarnManager.NudgeKind.CANCEL), warns.nudgeForCancel(receiver));
    }

    @Test
    @DisplayName("should nudge after three straight expiries")
    void shouldNudgeExpiryStreak() {
        warns.expired(request());
        warns.expired(request());
        warns.expired(request());

        assertEquals(Optional.of(TpaWarnManager.NudgeKind.EXPIRY), warns.nudgeForExpiry(receiver));
    }

    @Test
    @DisplayName("should reset streaks on accept")
    void shouldResetStreaksOnAccept() {
        warns.denied(request());
        warns.denied(request());
        warns.accepted(request());
        warns.denied(request());

        assertEquals(Optional.empty(), warns.nudgeForCancel(receiver));
    }

    @Test
    @DisplayName("should reset the streak when the nudge shows")
    void shouldResetStreakOnNudge() {
        warns.denied(request());
        warns.denied(request());
        warns.denied(request());
        warns.nudgeForCancel(receiver);
        clock.addAndGet(301_000L);

        assertEquals(Optional.empty(), warns.nudgeForCancel(receiver));
    }

    @Test
    @DisplayName("should throttle nudges to one per cooldown")
    void shouldThrottleNudges() {
        warns.denied(request());
        warns.denied(request());
        warns.denied(request());
        warns.nudgeForCancel(receiver);
        warns.denied(request());
        warns.denied(request());
        warns.denied(request());

        assertEquals(Optional.empty(), warns.nudgeForCancel(receiver));

        clock.addAndGet(300_000L);

        assertEquals(Optional.of(TpaWarnManager.NudgeKind.CANCEL), warns.nudgeForCancel(receiver));
    }

    @Test
    @DisplayName("should nudge when one sender fills the harassment window")
    void shouldNudgeHarassment() {
        warns.delivered(request());
        warns.delivered(request());
        assertEquals(Optional.empty(), warns.nudgeForHarass(receiver, sender));

        warns.delivered(request());

        assertEquals(Optional.of(sender), warns.nudgeForHarass(receiver, sender));
    }

    @Test
    @DisplayName("should slide old requests out of the harassment window")
    void shouldSlideHarassmentWindow() {
        warns.delivered(request());
        warns.delivered(request());
        clock.addAndGet(601_000L);
        warns.delivered(request());

        assertEquals(Optional.empty(), warns.nudgeForHarass(receiver, sender));
    }

    @Test
    @DisplayName("should throttle harassment nudges too")
    void shouldThrottleHarassment() {
        warns.delivered(request());
        warns.delivered(request());
        warns.delivered(request());
        warns.nudgeForHarass(receiver, sender);
        warns.delivered(request());

        assertEquals(Optional.empty(), warns.nudgeForHarass(receiver, sender));
    }
}

package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.config.PluginConfig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;
import com.jruk8.juicytpa.api.TpaRequest;

/**
 * Tracks warning statistics and decides when senders get warned or
 * receivers get nudged. Long-term counters live in {@link TpaWarnStore};
 * live pending windows, resend confirmations, answer streaks, nudge
 * throttling and harassment hits live in memory.
 */
public final class TpaWarnManager {

    /**
     * Why a sender is warned before requesting.
     */
    public enum WarnKind {
        AFK,
        CANCELLER
    }

    /**
     * Why a receiver is nudged towards blocking.
     */
    public enum NudgeKind {
        CANCEL,
        EXPIRY
    }

    private final TpaWarnStore stats;
    private final PluginConfig pluginConfig;
    private final LongSupplier clock;
    private final Map<UUID, PendingWindow> windows = new HashMap<>();
    private final Map<ConfirmKey, Long> confirmations = new HashMap<>();
    private final Map<UUID, Integer> cancelStreaks = new HashMap<>();
    private final Map<UUID, Integer> expiryStreaks = new HashMap<>();
    private final Map<UUID, Long> lastNudge = new HashMap<>();
    private final Map<HarassKey, Deque<Long>> harassHits = new HashMap<>();

    /**
     * @param stats the persisted warning statistics
     * @param pluginConfig the live plugin config
     * @param clock the epoch-millis clock, injectable for tests
     */
    public TpaWarnManager(TpaWarnStore stats, PluginConfig pluginConfig, LongSupplier clock) {
        this.stats = stats;
        this.pluginConfig = pluginConfig;
        this.clock = clock;
    }

    /**
     * @return the current epoch milliseconds
     */
    public long now() {
        return this.clock.getAsLong();
    }

    /**
     * Opens (or extends) the receiver's pending window and records the
     * delivery for harassment detection.
     *
     * @param request the delivered request
     */
    public void delivered(TpaRequest request) {
        long at = now();
        PendingWindow window = this.windows.computeIfAbsent(request.receiver(), ignored -> new PendingWindow());
        if (window.open == 0) {
            window.startedAt = at;
        }
        window.open++;
        Deque<Long> hits = this.harassHits.computeIfAbsent(
                new HarassKey(request.receiver(), request.sender()), ignored -> new ArrayDeque<>());
        hits.addLast(at);
        pruneHarass(hits, at);
    }

    /**
     * Closes one pending slot; when the receiver's last request clears,
     * the window's wall-clock seconds flush to storage.
     *
     * @param request the removed request
     */
    public void removed(TpaRequest request) {
        PendingWindow window = this.windows.get(request.receiver());
        if (window == null || window.open == 0) {
            return;
        }
        window.open--;
        if (window.open == 0) {
            this.stats.addPendingSeconds(request.receiver(), elapsedSeconds(window.startedAt, now()));
            this.windows.remove(request.receiver());
        }
    }

    /**
     * Records an expiry and grows the receiver's expiry streak.
     *
     * @param request the expired request
     */
    public void expired(TpaRequest request) {
        this.stats.recordExpiry(request.receiver(), elapsedSeconds(request.createdAt(), now()));
        this.expiryStreaks.merge(request.receiver(), 1, Integer::sum);
    }

    /**
     * Records a deny and grows the receiver's cancel streak.
     *
     * @param request the denied request
     */
    public void denied(TpaRequest request) {
        this.stats.recordDenied(request.receiver(), elapsedSeconds(request.createdAt(), now()));
        this.cancelStreaks.merge(request.receiver(), 1, Integer::sum);
    }

    /**
     * Resets both answer streaks: any accept breaks the chain.
     *
     * @param request the accepted request
     */
    public void accepted(TpaRequest request) {
        this.cancelStreaks.remove(request.receiver());
        this.expiryStreaks.remove(request.receiver());
    }

    /**
     * @param receiver the receiving player
     * @return the warning kind when the receiver's stats cross a
     * threshold, AFK first, or empty when no warning applies
     */
    public Optional<WarnKind> warnKind(UUID receiver) {
        var warnings = this.pluginConfig.getTpa().getWarnings();
        if (this.stats.unanswered(receiver) >= warnings.getAfkMinUnanswered()
                && this.stats.pendingSeconds(receiver) >= warnings.getAfkMinPendingSeconds()) {
            return Optional.of(WarnKind.AFK);
        }
        if (this.stats.denied(receiver) >= warnings.getCancellerMinDenied()
                && this.stats.deniedSeconds(receiver) >= warnings.getCancellerMinPendingSeconds()) {
            return Optional.of(WarnKind.CANCELLER);
        }
        return Optional.empty();
    }

    /**
     * Consumes a fresh confirmation for this sender-target pair.
     *
     * @param sender the sending player
     * @param receiver the receiving player
     * @param here whether the request is a /tpahere
     * @return true when the pair re-sends inside the confirm window
     */
    public boolean confirm(UUID sender, UUID receiver, boolean here) {
        pruneConfirmations();
        return this.confirmations.remove(new ConfirmKey(sender, receiver, here)) != null;
    }

    /**
     * Starts the confirm window after showing a warning.
     *
     * @param sender the sending player
     * @param receiver the receiving player
     * @param here whether the request is a /tpahere
     */
    public void noteWarned(UUID sender, UUID receiver, boolean here) {
        this.confirmations.put(new ConfirmKey(sender, receiver, here), now());
    }

    /**
     * @param receiver the receiving player
     * @return the cancel nudge when the streak and cooldown allow, consuming both
     */
    public Optional<NudgeKind> nudgeForCancel(UUID receiver) {
        int minimum = this.pluginConfig.getTpa().getWarnings().getNudgeMinCancelStreak();
        return pollNudge(receiver, this.cancelStreaks, minimum, NudgeKind.CANCEL);
    }

    /**
     * @param receiver the receiving player
     * @return the expiry nudge when the streak and cooldown allow, consuming both
     */
    public Optional<NudgeKind> nudgeForExpiry(UUID receiver) {
        int minimum = this.pluginConfig.getTpa().getWarnings().getNudgeMinExpiryStreak();
        return pollNudge(receiver, this.expiryStreaks, minimum, NudgeKind.EXPIRY);
    }

    /**
     * @param receiver the receiving player
     * @param sender the sending player
     * @return the sender when their request rate and the cooldown allow a
     * harassment nudge, consuming the cooldown
     */
    public Optional<UUID> nudgeForHarass(UUID receiver, UUID sender) {
        Deque<Long> hits = this.harassHits.get(new HarassKey(receiver, sender));
        if (hits == null || hits.size() < this.pluginConfig.getTpa().getWarnings().getHarassMinRequests()) {
            return Optional.empty();
        }
        if (!throttleFree(receiver)) {
            return Optional.empty();
        }
        stampNudge(receiver);
        return Optional.of(sender);
    }

    private Optional<NudgeKind> pollNudge(
            UUID receiver, Map<UUID, Integer> streaks, int minimum, NudgeKind kind) {
        if (streaks.getOrDefault(receiver, 0) < minimum || !throttleFree(receiver)) {
            return Optional.empty();
        }
        streaks.remove(receiver);
        stampNudge(receiver);
        return Optional.of(kind);
    }

    private boolean throttleFree(UUID receiver) {
        Long last = this.lastNudge.get(receiver);
        if (last == null) {
            return true;
        }
        long cooldown = (long) this.pluginConfig.getTpa().getWarnings().getNudgeCooldownSeconds() * 1000L;
        return now() - last >= cooldown;
    }

    private void stampNudge(UUID receiver) {
        this.lastNudge.put(receiver, now());
    }

    private void pruneHarass(Deque<Long> hits, long at) {
        long window = (long) this.pluginConfig.getTpa().getWarnings().getHarassWindowSeconds() * 1000L;
        while (!hits.isEmpty() && at - hits.getFirst() > window) {
            hits.removeFirst();
        }
    }

    private void pruneConfirmations() {
        long at = now();
        long window = (long) this.pluginConfig.getTpa().getWarnings().getConfirmWindowSeconds() * 1000L;
        this.confirmations.entrySet().removeIf(entry -> at - entry.getValue() > window);
    }

    private static long elapsedSeconds(long start, long end) {
        return Math.max(0, (end - start) / 1000L);
    }

    private static final class PendingWindow {
        private int open;
        private long startedAt;
    }

    private record ConfirmKey(UUID sender, UUID receiver, boolean here) {
    }

    private record HarassKey(UUID receiver, UUID sender) {
    }
}

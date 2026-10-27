package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.message.MessagesConfig;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import com.jruk8.juicytpa.api.TpaMode;
import com.jruk8.juicytpa.api.TpaRequest;
import com.jruk8.juicytpa.api.TpaRequestCancelEvent;
import com.jruk8.juicytpa.api.TpaRequestDenyEvent;
import com.jruk8.juicytpa.api.TpaRequestExpireEvent;

/**
 * Owns every pending teleport request: sending, answering, cancelling,
 * expiry and the teleport itself. Requests live in memory and expire via
 * the global-region scheduler; teleports run on the mover's entity
 * scheduler, so the whole flow is Folia-safe.
 */
public class TpaService {

    private final CommandContext ctx;
    private final PluginConfig pluginConfig;
    private final TpaModeStore modes;
    private final TpaBlockStore blocks;
    private final TpaTrustStore trusts;
    private final TpaReceiverService receiverService;
    private final TpaWarnManager warns;
    private final TpaCooldowns cooldowns;
    private final TpaUsageTracker tracker;
    private final Plugin plugin;
    private final TaskScheduler scheduler;
    private final Function<UUID, Player> playerLookup;
    private final Function<UUID, String> nameLookup;
    private final Consumer<Event> events;
    private final TpaEventGate gate;
    private final TpaListService lists;
    private final TpaFeedback feedback;
    private final Map<RequestKey, Entry> pending = new LinkedHashMap<>();

    public TpaService(
            CommandContext ctx,
            PluginConfig pluginConfig,
            TpaModeStore modes,
            TpaBlockStore blocks,
            TpaTrustStore trusts,
            TpaWarnManager warns,
            TpaCooldowns cooldowns,
            TpaUsageTracker tracker,
            Plugin plugin,
            TaskScheduler scheduler,
            Function<UUID, Player> playerLookup,
            Function<UUID, String> nameLookup,
            Consumer<Event> events
    ) {
        this.ctx = ctx;
        this.pluginConfig = pluginConfig;
        this.modes = modes;
        this.blocks = blocks;
        this.trusts = trusts;
        this.receiverService = new TpaReceiverService(ctx, modes, trusts);
        this.warns = warns;
        this.cooldowns = cooldowns;
        this.tracker = tracker;
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.playerLookup = playerLookup;
        this.nameLookup = nameLookup;
        this.events = events;
        this.gate = new TpaEventGate(ctx, events);
        this.lists = new TpaListService(ctx, trusts, nameLookup, this::incoming, this::outgoing);
        this.feedback = new TpaFeedback(ctx, pluginConfig, playerLookup);
    }

    /**
     * Sends a request from the executor to the target.
     *
     * @param source the command source
     * @param target the receiving player
     * @param here whether the receiver teleports to the sender
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int send(CommandSourceStack source, Player target, boolean here) {
        if (!(source.getSender() instanceof Player sender)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            ctx.error(source, m -> m.getTpa().getSelfRequest(), Map.of());
            return 0;
        }
        if (hasPendingPair(sender.getUniqueId(), target.getUniqueId())
                || atConcurrencyCap(sender.getUniqueId())) {
            ctx.error(source, m -> m.getTpa().getAlreadyPending(), Map.of());
            return 0;
        }
        long cooldownLeft = cooldowns.remaining(sender);
        if (cooldownLeft > 0) {
            String time = DurationFormat.format((cooldownLeft + 999L) / 1000L);
            ctx.error(source, m -> m.getTpa().getSendCooldown(), Map.of("time", time));
            return 0;
        }
        if (blocks.isBlocked(target.getUniqueId(), sender.getUniqueId())) {
            ctx.error(source, m -> m.getTpa().getRequestBlocked(), Map.of("player", target.getName()));
            return 0;
        }
        if (!here && modes.mode(target.getUniqueId()) == TpaMode.BLOCK_ALL) {
            ctx.error(source, m -> m.getTpa().getReceiverBlocked(), Map.of("player", target.getName()));
            return 0;
        }
        var warn = warns.warnKind(target.getUniqueId());
        if (warn.isPresent() && !warns.confirm(sender.getUniqueId(), target.getUniqueId(), here)) {
            warns.noteWarned(sender.getUniqueId(), target.getUniqueId(), here);
            feedback.warnFeedback(source, target, here, warn.get());
            return 0;
        }
        if (!deliver(sender, target, here, true)) {
            return 0;
        }
        cooldowns.record(sender);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Sends one request to every listed target, skipping the sender,
     * duplicates and blocked pairs. Block-all mode only skips ask
     * requests. Bulk sends bypass concurrency entirely: the toggle, the
     * cap and warning confirmations never apply, and the sender only
     * gets the final summary instead of per-target confirmations.
     *
     * @param source the command source
     * @param targets the receiving players
     * @param here whether the receivers teleport to the sender
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int sendBulk(CommandSourceStack source, List<Player> targets, boolean here) {
        if (!(source.getSender() instanceof Player sender)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        int delivered = 0;
        for (Player target : targets) {
            if (sendable(sender, target, here) && deliver(sender, target, here, false)) {
                delivered++;
            }
        }
        ctx.send(source, m -> m.getTpa().getTpallSent(), Map.of("count", String.valueOf(delivered)));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Toggles the block between the executor and the target. Blocking
     * drops the target's pending requests to the executor.
     *
     * @param source the command source
     * @param target the blocked or unblocked player
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int block(CommandSourceStack source, Player target) {
        if (!(source.getSender() instanceof Player blocker)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        if (target.getUniqueId().equals(blocker.getUniqueId())) {
            ctx.error(source, m -> m.getTpa().getBlockSelf(), Map.of());
            return 0;
        }
        if (blocks.isBlocked(blocker.getUniqueId(), target.getUniqueId())) {
            blocks.setBlocked(blocker.getUniqueId(), target.getUniqueId(), false);
            ctx.send(source, m -> m.getTpa().getBlockRemoved(), Map.of("player", target.getName()));
            return Command.SINGLE_SUCCESS;
        }
        blocks.setBlocked(blocker.getUniqueId(), target.getUniqueId(), true);
        dropIncoming(blocker, target);
        ctx.send(source, m -> m.getTpa().getBlockAdded(), Map.of("player", target.getName()));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Toggles the trust between the executor and the target.
     *
     * @param source the command source
     * @param target the trusted or untrusted player
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int trust(CommandSourceStack source, Player target) {
        return receiverService.trust(source, target);
    }

    /**
     * Accepts one incoming request of the executor.
     *
     * @param source the command source
     * @param named the named sender, or empty for argless
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int accept(CommandSourceStack source, Optional<Player> named) {
        return answer(source, named, true);
    }

    /**
     * Denies one incoming request of the executor.
     *
     * @param source the command source
     * @param named the named sender, or empty for argless
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int deny(CommandSourceStack source, Optional<Player> named) {
        return answer(source, named, false);
    }

    /**
     * Cancels one outgoing request of the executor.
     *
     * @param source the command source
     * @param named the named receiver, or empty for argless
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int cancel(CommandSourceStack source, Optional<Player> named) {
        if (!(source.getSender() instanceof Player sender)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        Optional<TpaRequest> match = named
                .map(target -> latestTo(sender.getUniqueId(), target.getUniqueId()))
                .orElseGet(() -> singleOutgoing(source, sender));
        if (match.isEmpty() && named.isPresent()) {
            Player target = named.get();
            ctx.error(source, m -> m.getTpa().getNoOutgoingTo(), Map.of("player", target.getName()));
            return 0;
        }
        if (match.isEmpty()) {
            return 0;
        }
        cancelRequest(sender, match.get());
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Lists the executor's pending incoming and outgoing requests,
     * grouped by direction and request type with clickable names.
     *
     * @param source the command source
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int list(CommandSourceStack source) {
        return lists.list(source);
    }

    /**
     * Cycles the executor's mode, or sets it from the argument.
     *
     * @param source the command source
     * @param input the raw mode argument, or empty for argless
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int mode(CommandSourceStack source, Optional<String> input) {
        return receiverService.mode(source, input);
    }

    /**
     * Flips the executor between accept-all mode and normal mode.
     *
     * @param source the command source
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int autoMode(CommandSourceStack source) {
        return receiverService.autoMode(source);
    }

    /**
     * Flips the executor between block-all mode and normal mode.
     *
     * @param source the command source
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int blockAllMode(CommandSourceStack source) {
        return receiverService.blockAllMode(source);
    }

    /**
     * Lists the executor's pending incoming requests, oldest first.
     *
     * @param receiver the receiving player
     * @return the matching requests
     */
    public List<TpaRequest> incoming(UUID receiver) {
        List<TpaRequest> matches = new ArrayList<>();
        for (Entry entry : pending.values()) {
            if (entry.request().receiver().equals(receiver)) {
                matches.add(entry.request());
            }
        }
        return matches;
    }

    /**
     * Lists the executor's pending outgoing requests, oldest first.
     *
     * @param sender the sending player
     * @return the matching requests
     */
    public List<TpaRequest> outgoing(UUID sender) {
        List<TpaRequest> matches = new ArrayList<>();
        for (Entry entry : pending.values()) {
            if (entry.request().sender().equals(sender)) {
                matches.add(entry.request());
            }
        }
        return matches;
    }

    /**
     * Names the most recent pending incoming request's sender.
     *
     * @param receiver the receiving player
     * @return the sender name, or empty when nothing is pending
     */
    public Optional<String> latestPendingSender(UUID receiver) {
        List<TpaRequest> matches = incoming(receiver);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matches.getLast().senderName());
    }

    private int answer(CommandSourceStack source, Optional<Player> named, boolean accept) {
        if (!(source.getSender() instanceof Player receiver)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        Optional<TpaRequest> match = named
                .map(sender -> latestFrom(receiver.getUniqueId(), sender.getUniqueId()))
                .orElseGet(() -> singleIncoming(source, receiver));
        if (match.isEmpty() && named.isPresent()) {
            Player sender = named.get();
            ctx.error(source, m -> m.getTpa().getNoPendingFrom(), Map.of("player", sender.getName()));
            return 0;
        }
        if (match.isEmpty()) {
            return 0;
        }
        if (accept) {
            Player sender = playerLookup.apply(match.get().sender());
            if (sender == null) {
                remove(match.get());
                ctx.error(source, m -> m.getTpa().getNoPendingFrom(), Map.of("player", match.get().senderName()));
                return 0;
            }
            if (gate.vetoedAccept(match.get(), sender, receiver, receiver)) {
                return 0;
            }
            acceptRequest(match.get());
        } else {
            denyRequest(receiver, match.get());
        }
        return Command.SINGLE_SUCCESS;
    }

    /**
     * @return the event gate, shared with the API implementation
     */
    TpaEventGate eventGate() {
        return gate;
    }

    private boolean deliver(Player sender, Player target, boolean here, boolean senderFeedback) {
        var key = new RequestKey(sender.getUniqueId(), target.getUniqueId(), here);
        var request = new TpaRequest(
                sender.getUniqueId(), sender.getName(), target.getUniqueId(), target.getName(), here,
                warns.now());
        if (gate.vetoedSend(request, sender, target)) {
            return false;
        }
        tracker.recordSent(sender.getUniqueId());
        tracker.recordReceived(target.getUniqueId());
        if (!here
                && (modes.mode(target.getUniqueId()) == TpaMode.ACCEPT_ALL
                        || trusts.isTrusted(target.getUniqueId(), sender.getUniqueId()))) {
            if (gate.vetoedAccept(request, sender, target, sender)) {
                return false;
            }
            acceptRequest(request);
            return true;
        }
        ScheduledTask task = scheduler.runLater(() -> expire(key), durationTicks());
        pending.put(key, new Entry(request, task));
        warns.delivered(request);
        feedback.requestFeedback(sender, target, here, senderFeedback);
        warns.nudgeForHarass(target.getUniqueId(), sender.getUniqueId())
                .ifPresent(ignored -> feedback.harassNudge(target, sender.getName()));
        return true;
    }

    /**
     * Removes the request, fires a command-cause cancel event and
     * tells both parties.
     *
     * @param sender the original sender
     * @param request the request to cancel
     */
    void cancelRequest(Player sender, TpaRequest request) {
        remove(request);
        Player receiver = playerLookup.apply(request.receiver());
        events.accept(new TpaRequestCancelEvent(
                request, sender, receiver, TpaRequestCancelEvent.Cause.CANCEL_COMMAND));
        feedback.cancelFeedback(sender, request);
    }

    private boolean sendable(Player sender, Player target, boolean here) {
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            return false;
        }
        return !hasPendingPair(sender.getUniqueId(), target.getUniqueId())
                && !blocks.isBlocked(target.getUniqueId(), sender.getUniqueId())
                && (here || modes.mode(target.getUniqueId()) != TpaMode.BLOCK_ALL);
    }

    private boolean hasPendingPair(UUID sender, UUID receiver) {
        return pending.containsKey(new RequestKey(sender, receiver, false))
                || pending.containsKey(new RequestKey(sender, receiver, true));
    }

    private void dropIncoming(Player blocker, Player target) {
        for (TpaRequest request : incoming(blocker.getUniqueId())) {
            if (!request.sender().equals(target.getUniqueId())) {
                continue;
            }
            remove(request);
            events.accept(new TpaRequestCancelEvent(
                    request, target, blocker, TpaRequestCancelEvent.Cause.BLOCK));
            chat(target, m -> m.getTpa().getAutoCancelled(), Map.of("player", blocker.getName()));
            ctx.soundPlayer().play(target, ctx.sounds().getNeutral());
        }
    }

    /**
     * Removes the request, sweeps the receiver's others, teleports
     * and tells both parties.
     *
     * @param request the accepted request
     */
    void acceptRequest(TpaRequest request) {
        remove(request);
        warns.accepted(request);
        Player sender = playerLookup.apply(request.sender());
        Player receiver = playerLookup.apply(request.receiver());
        if (sender == null || receiver == null) {
            return;
        }
        sweepOthers(request);
        teleport(request, sender, receiver);
        Player anchor = request.here() ? sender : receiver;
        chat(receiver, m -> m.getTpa().getAcceptReceiver(), Map.of("player", request.senderName()));
        chat(sender, m -> m.getTpa().getAcceptSender(), Map.of("player", request.receiverName()));
        ctx.soundPlayer().play(anchor, ctx.sounds().getAccept());
    }

    /**
     * Removes the request, fires the deny event and tells both parties.
     *
     * @param receiver the answering player
     * @param request the denied request
     */
    void denyRequest(Player receiver, TpaRequest request) {
        remove(request);
        warns.denied(request);
        Player sender = playerLookup.apply(request.sender());
        events.accept(new TpaRequestDenyEvent(request, sender, receiver));
        var feedback = pluginConfig.getTpa().getRejectFeedback();
        if (feedback.isMessage()) {
            chat(receiver, m -> m.getTpa().getDenyReceiver(), Map.of("player", request.senderName()));
        }
        if (feedback.isActionbar()) {
            bar(receiver, m -> m.getTpa().getDenyReceiverActionbar(), Map.of("player", request.senderName()));
        }
        ctx.soundPlayer().play(receiver, ctx.sounds().getNeutral());
        if (sender == null) {
            return;
        }
        if (feedback.isMessage()) {
            chat(sender, m -> m.getTpa().getDenySender(), Map.of("player", request.receiverName()));
        }
        if (feedback.isActionbar()) {
            bar(sender, m -> m.getTpa().getDenySenderActionbar(), Map.of("player", request.receiverName()));
        }
        ctx.soundPlayer().play(sender, ctx.sounds().getReject());
        warns.nudgeForCancel(receiver.getUniqueId())
                .ifPresent(ignored -> this.feedback.blockAllNudge(receiver, m -> m.getTpa().getNudgeCancel()));
    }

    private void expire(RequestKey key) {
        Entry entry = pending.remove(key);
        if (entry == null) {
            return;
        }
        TpaRequest request = entry.request();
        warns.removed(request);
        warns.expired(request);
        Player sender = playerLookup.apply(request.sender());
        Player receiver = playerLookup.apply(request.receiver());
        events.accept(new TpaRequestExpireEvent(request, sender, receiver));
        var feedback = pluginConfig.getTpa().getTimeoutFeedback();
        if (sender != null) {
            if (feedback.isMessage()) {
                chat(sender, m -> m.getTpa().getTimeoutSender(), Map.of("player", request.receiverName()));
            }
            if (feedback.isActionbar()) {
                bar(sender, m -> m.getTpa().getTimeoutSenderActionbar(), Map.of("player", request.receiverName()));
            }
            ctx.soundPlayer().play(sender, ctx.sounds().getTimeout());
        }
        if (receiver != null) {
            if (feedback.isMessage()) {
                chat(receiver, m -> m.getTpa().getTimeoutReceiver(), Map.of("player", request.senderName()));
            }
            if (feedback.isActionbar()) {
                bar(receiver, m -> m.getTpa().getTimeoutReceiverActionbar(), Map.of("player", request.senderName()));
            }
            ctx.soundPlayer().play(receiver, ctx.sounds().getTimeout());
            warns.nudgeForExpiry(receiver.getUniqueId())
                    .ifPresent(ignored -> this.feedback.blockAllNudge(receiver, m -> m.getTpa().getNudgeAfk()));
        }
    }

    private void sweepOthers(TpaRequest accepted) {
        if (!concurrency().isAllowConcurrent() || !concurrency().isCancelExistingOnAccept()) {
            return;
        }
        Player receiver = playerLookup.apply(accepted.receiver());
        for (TpaRequest other : incoming(accepted.receiver())) {
            remove(other);
            events.accept(new TpaRequestCancelEvent(
                    other, playerLookup.apply(other.sender()), receiver,
                    TpaRequestCancelEvent.Cause.ACCEPT_SWEEP));
            Player sender = playerLookup.apply(other.sender());
            if (sender != null) {
                chat(sender, m -> m.getTpa().getAutoCancelled(), Map.of("player", other.receiverName()));
                ctx.soundPlayer().play(sender, ctx.sounds().getNeutral());
            }
        }
    }

    private void teleport(TpaRequest request, Player sender, Player receiver) {
        Player mover = request.here() ? receiver : sender;
        Player anchor = request.here() ? sender : receiver;
        mover.getScheduler().run(plugin, task -> {
            mover.teleport(anchor.getLocation());
            ctx.soundPlayer().play(mover, ctx.sounds().getTeleport());
        }, null);
    }

    private Optional<TpaRequest> singleIncoming(CommandSourceStack source, Player receiver) {
        List<TpaRequest> matches = incoming(receiver.getUniqueId());
        if (matches.isEmpty()) {
            ctx.error(source, m -> m.getTpa().getNoPending(), Map.of());
            return Optional.empty();
        }
        if (matches.size() > 1) {
            ctx.error(source, m -> m.getTpa().getMultiplePending(), Map.of());
            return Optional.empty();
        }
        return Optional.of(matches.getFirst());
    }

    private Optional<TpaRequest> singleOutgoing(CommandSourceStack source, Player sender) {
        List<TpaRequest> matches = outgoing(sender.getUniqueId());
        if (matches.isEmpty()) {
            ctx.error(source, m -> m.getTpa().getNoOutgoing(), Map.of());
            return Optional.empty();
        }
        if (matches.size() > 1) {
            ctx.error(source, m -> m.getTpa().getMultiplePending(), Map.of());
            return Optional.empty();
        }
        return Optional.of(matches.getFirst());
    }

    /**
     * @param receiver the receiving player
     * @param sender the original sender
     * @return their latest pending request, or empty
     */
    Optional<TpaRequest> latestFrom(UUID receiver, UUID sender) {
        List<TpaRequest> matches = incoming(receiver);
        Optional<TpaRequest> latest = Optional.empty();
        for (TpaRequest request : matches) {
            if (request.sender().equals(sender)) {
                latest = Optional.of(request);
            }
        }
        return latest;
    }

    /**
     * @param sender the original sender
     * @param receiver the receiving player
     * @return their latest pending request, or empty
     */
    Optional<TpaRequest> latestTo(UUID sender, UUID receiver) {
        List<TpaRequest> matches = outgoing(sender);
        Optional<TpaRequest> latest = Optional.empty();
        for (TpaRequest request : matches) {
            if (request.receiver().equals(receiver)) {
                latest = Optional.of(request);
            }
        }
        return latest;
    }

    private boolean hasOutgoing(UUID sender) {
        return !outgoing(sender).isEmpty();
    }

    private boolean atConcurrencyCap(UUID sender) {
        var concurrency = concurrency();
        if (!concurrency.isAllowConcurrent()) {
            return hasOutgoing(sender);
        }
        int max = concurrency.getMaxConcurrent();
        return max > 0 && outgoing(sender).size() >= max;
    }

    /**
     * Drops the request and cancels its expiry task.
     *
     * @param request the request to drop
     */
    void remove(TpaRequest request) {
        var key = new RequestKey(request.sender(), request.receiver(), request.here());
        Entry entry = pending.remove(key);
        if (entry == null) {
            return;
        }
        warns.removed(request);
        if (entry.task() != null) {
            entry.task().cancel();
        }
    }

    private void chat(CommandSender to,
                      Function<MessagesConfig, String> selector,
                      Map<String, String> tags) {
        ctx.messenger().send(to, selector.apply(ctx.messages()), tags);
    }

    private void bar(CommandSender to,
                     Function<MessagesConfig, String> selector,
                     Map<String, String> tags) {
        ctx.messenger().sendActionBar(to, selector.apply(ctx.messages()), tags);
    }

    private PluginConfig.Concurrency concurrency() {
        return pluginConfig.getTpa().getConcurrency();
    }

    private long durationTicks() {
        return Math.max(0, pluginConfig.getTpa().getRequestDuration()) * 20L;
    }

    private record RequestKey(UUID sender, UUID receiver, boolean here) {
    }

    private record Entry(TpaRequest request, ScheduledTask task) {
    }
}

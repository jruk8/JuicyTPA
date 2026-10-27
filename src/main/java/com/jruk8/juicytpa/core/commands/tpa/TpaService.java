package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.commands.CommandContext;
import com.jruk8.juicytpa.core.configs.PluginConfig;
import com.jruk8.juicytpa.core.messages.MessagesConfig;
import com.jruk8.juicytpa.core.placeholders.TpaUsageTracker;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

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
    private final TpaUsageTracker tracker;
    private final Plugin plugin;
    private final TaskScheduler scheduler;
    private final Function<UUID, Player> playerLookup;
    private final Map<RequestKey, Entry> pending = new LinkedHashMap<>();

    public TpaService(
            CommandContext ctx,
            PluginConfig pluginConfig,
            TpaModeStore modes,
            TpaBlockStore blocks,
            TpaUsageTracker tracker,
            Plugin plugin,
            TaskScheduler scheduler,
            Function<UUID, Player> playerLookup
    ) {
        this.ctx = ctx;
        this.pluginConfig = pluginConfig;
        this.modes = modes;
        this.blocks = blocks;
        this.tracker = tracker;
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.playerLookup = playerLookup;
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
        var key = new RequestKey(sender.getUniqueId(), target.getUniqueId(), here);
        if (pending.containsKey(key) || (!concurrency().isAllowConcurrent() && hasOutgoing(sender.getUniqueId()))) {
            ctx.error(source, m -> m.getTpa().getAlreadyPending(), Map.of());
            return 0;
        }
        if (blocks.isBlocked(target.getUniqueId(), sender.getUniqueId())) {
            ctx.error(source, m -> m.getTpa().getRequestBlocked(), Map.of("player", target.getName()));
            return 0;
        }
        if (modes.mode(target.getUniqueId()) == TpaMode.BLOCK_ALL) {
            ctx.error(source, m -> m.getTpa().getReceiverBlocked(), Map.of("player", target.getName()));
            return 0;
        }
        deliver(sender, target, here);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Sends one request to every listed target, skipping the sender,
     * duplicates, blocked pairs and receivers in block-all mode. Needs
     * concurrent requests enabled.
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
        if (!concurrency().isAllowConcurrent()) {
            ctx.error(source, m -> m.getTpa().getNeedsConcurrency(), Map.of());
            return 0;
        }
        int delivered = 0;
        for (Player target : targets) {
            if (sendable(sender, target, here)) {
                deliver(sender, target, here);
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
        remove(match.get());
        sendCancelFeedback(sender, match.get());
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Cycles the executor's mode, or sets it from the argument.
     *
     * @param source the command source
     * @param input the raw mode argument, or empty for argless
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int mode(CommandSourceStack source, Optional<String> input) {
        if (!(source.getSender() instanceof Player player)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        TpaMode mode;
        if (input.isEmpty()) {
            mode = modes.mode(player.getUniqueId()).next();
        } else {
            Optional<TpaMode> parsed = TpaMode.parse(input.get());
            if (parsed.isEmpty()) {
                ctx.error(source, m -> m.getTpa().getModeInvalid(), Map.of("input", input.get()));
                return 0;
            }
            mode = parsed.get();
        }
        modes.setMode(player.getUniqueId(), mode);
        ctx.send(source, m -> m.getTpa().getModeSet(), Map.of("mode", mode.display()));
        return Command.SINGLE_SUCCESS;
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
            if (playerLookup.apply(match.get().sender()) == null) {
                remove(match.get());
                ctx.error(source, m -> m.getTpa().getNoPendingFrom(), Map.of("player", match.get().senderName()));
                return 0;
            }
            acceptRequest(match.get());
        } else {
            denyRequest(receiver, match.get());
        }
        return Command.SINGLE_SUCCESS;
    }

    private void deliver(Player sender, Player target, boolean here) {
        var key = new RequestKey(sender.getUniqueId(), target.getUniqueId(), here);
        var request = new TpaRequest(
                sender.getUniqueId(), sender.getName(), target.getUniqueId(), target.getName(), here);
        tracker.recordSent(sender.getUniqueId());
        tracker.recordReceived(target.getUniqueId());
        if (modes.mode(target.getUniqueId()) == TpaMode.ACCEPT_ALL) {
            acceptRequest(request);
            return;
        }
        ScheduledTask task = scheduler.runLater(() -> expire(key), durationTicks());
        pending.put(key, new Entry(request, task));
        sendRequestFeedback(sender, target, here);
    }

    private boolean sendable(Player sender, Player target, boolean here) {
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            return false;
        }
        var key = new RequestKey(sender.getUniqueId(), target.getUniqueId(), here);
        return !pending.containsKey(key)
                && !blocks.isBlocked(target.getUniqueId(), sender.getUniqueId())
                && modes.mode(target.getUniqueId()) != TpaMode.BLOCK_ALL;
    }

    private void dropIncoming(Player blocker, Player target) {
        for (TpaRequest request : incoming(blocker.getUniqueId())) {
            if (!request.sender().equals(target.getUniqueId())) {
                continue;
            }
            remove(request);
            chat(target, m -> m.getTpa().getAutoCancelled(), Map.of("player", blocker.getName()));
            ctx.soundPlayer().play(target, ctx.sounds().getNeutral());
        }
    }

    private void acceptRequest(TpaRequest request) {
        remove(request);
        Player sender = playerLookup.apply(request.sender());
        Player receiver = playerLookup.apply(request.receiver());
        if (sender == null || receiver == null) {
            return;
        }
        sweepOthers(request);
        teleport(request, sender, receiver);
        chat(receiver, m -> m.getTpa().getAcceptReceiver(), Map.of("player", request.senderName()));
        ctx.soundPlayer().play(receiver, ctx.sounds().getAccept());
        chat(sender, m -> m.getTpa().getAcceptSender(), Map.of("player", request.receiverName()));
        ctx.soundPlayer().play(sender, ctx.sounds().getAccept());
    }

    private void denyRequest(Player receiver, TpaRequest request) {
        remove(request);
        var feedback = pluginConfig.getTpa().getRejectFeedback();
        if (feedback.isMessage()) {
            chat(receiver, m -> m.getTpa().getDenyReceiver(), Map.of("player", request.senderName()));
        }
        if (feedback.isActionbar()) {
            bar(receiver, m -> m.getTpa().getDenyReceiverActionbar(), Map.of("player", request.senderName()));
        }
        ctx.soundPlayer().play(receiver, ctx.sounds().getNeutral());
        Player sender = playerLookup.apply(request.sender());
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
    }

    private void expire(RequestKey key) {
        Entry entry = pending.remove(key);
        if (entry == null) {
            return;
        }
        TpaRequest request = entry.request();
        var feedback = pluginConfig.getTpa().getTimeoutFeedback();
        Player sender = playerLookup.apply(request.sender());
        if (sender != null) {
            if (feedback.isMessage()) {
                chat(sender, m -> m.getTpa().getTimeoutSender(), Map.of("player", request.receiverName()));
            }
            if (feedback.isActionbar()) {
                bar(sender, m -> m.getTpa().getTimeoutSenderActionbar(), Map.of("player", request.receiverName()));
            }
            ctx.soundPlayer().play(sender, ctx.sounds().getTimeout());
        }
        Player receiver = playerLookup.apply(request.receiver());
        if (receiver != null) {
            if (feedback.isMessage()) {
                chat(receiver, m -> m.getTpa().getTimeoutReceiver(), Map.of("player", request.senderName()));
            }
            if (feedback.isActionbar()) {
                bar(receiver, m -> m.getTpa().getTimeoutReceiverActionbar(), Map.of("player", request.senderName()));
            }
            ctx.soundPlayer().play(receiver, ctx.sounds().getTimeout());
        }
    }

    private void sweepOthers(TpaRequest accepted) {
        if (!concurrency().isAllowConcurrent() || !concurrency().isCancelExistingOnAccept()) {
            return;
        }
        for (TpaRequest other : incoming(accepted.receiver())) {
            remove(other);
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
        mover.getScheduler().run(plugin, task -> mover.teleport(anchor.getLocation()), null);
    }

    private void sendRequestFeedback(Player sender, Player target, boolean here) {
        String time = DurationFormat.format(pluginConfig.getTpa().getRequestDuration());
        var senderFeedback = pluginConfig.getTpa().getSenderFeedback();
        if (senderFeedback.isMessage()) {
            chat(sender, m -> m.getTpa().getRequestSent(),
                    Map.of("player", target.getName(), "time", time));
        }
        if (senderFeedback.isActionbar()) {
            bar(sender, m -> m.getTpa().getRequestSentActionbar(), Map.of("player", target.getName()));
        }
        ctx.soundPlayer().play(sender, ctx.sounds().getNeutral());
        var receiverFeedback = pluginConfig.getTpa().getReceiverFeedback();
        if (receiverFeedback.isMessage()) {
            String template = here
                    ? ctx.messages().getTpa().getRequestHereBody()
                    : ctx.messages().getTpa().getRequestAskBody();
            chat(target, m -> m.getTpa().getRequestReceived(), Map.of(
                    "body", template.replace("{player}", sender.getName()),
                    "time", time,
                    "accept", RequestPrompts.accept(sender.getName(), ctx.messages().getTpa().getAcceptText()),
                    "reject", RequestPrompts.reject(sender.getName(), ctx.messages().getTpa().getRejectText())));
        }
        if (receiverFeedback.isActionbar()) {
            bar(target, m -> m.getTpa().getRequestReceivedActionbar(), Map.of("player", sender.getName()));
        }
        ctx.soundPlayer().play(target, ctx.sounds().getAsk());
    }

    private void sendCancelFeedback(Player sender, TpaRequest request) {
        chat(sender, m -> m.getTpa().getCancelSender(), Map.of("player", request.receiverName()));
        ctx.soundPlayer().play(sender, ctx.sounds().getNeutral());
        Player receiver = playerLookup.apply(request.receiver());
        if (receiver != null) {
            chat(receiver, m -> m.getTpa().getCancelReceiver(), Map.of("player", request.senderName()));
            ctx.soundPlayer().play(receiver, ctx.sounds().getNeutral());
        }
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

    private Optional<TpaRequest> latestFrom(UUID receiver, UUID sender) {
        List<TpaRequest> matches = incoming(receiver);
        Optional<TpaRequest> latest = Optional.empty();
        for (TpaRequest request : matches) {
            if (request.sender().equals(sender)) {
                latest = Optional.of(request);
            }
        }
        return latest;
    }

    private Optional<TpaRequest> latestTo(UUID sender, UUID receiver) {
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

    private void remove(TpaRequest request) {
        var key = new RequestKey(request.sender(), request.receiver(), request.here());
        Entry entry = pending.remove(key);
        if (entry != null && entry.task() != null) {
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

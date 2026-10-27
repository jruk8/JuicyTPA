package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.api.TpaRequest;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.config.PluginConfig;
import com.jruk8.juicytpa.message.MessagesConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Sends every request message, actionbar and sound: delivery
 * feedback, cancellations, warning notices and nudges.
 */
public final class TpaFeedback {

    private final CommandContext ctx;
    private final PluginConfig pluginConfig;
    private final Function<UUID, Player> playerLookup;

    public TpaFeedback(
            CommandContext ctx, PluginConfig pluginConfig, Function<UUID, Player> playerLookup) {
        this.ctx = ctx;
        this.pluginConfig = pluginConfig;
        this.playerLookup = playerLookup;
    }

    /**
     * Tells the sender (optionally) and the receiver about a stored
     * request, with clickable accept and reject prompts.
     *
     * @param sender the sending player
     * @param target the receiving player
     * @param here whether the receiver teleports to the sender
     * @param announce whether to confirm to the sender
     */
    public void requestFeedback(Player sender, Player target, boolean here, boolean announce) {
        String time = DurationFormat.format(pluginConfig.getTpa().getRequestDuration());
        if (announce) {
            var senderFeedback = pluginConfig.getTpa().getSenderFeedback();
            if (senderFeedback.isMessage()) {
                chat(sender, m -> m.getTpa().getRequestSent(),
                        Map.of("player", target.getName(), "time", time));
            }
            if (senderFeedback.isActionbar()) {
                bar(sender, m -> m.getTpa().getRequestSentActionbar(), Map.of("player", target.getName()));
            }
            ctx.soundPlayer().play(sender, ctx.sounds().getNeutral());
        }
        var receiverFeedback = pluginConfig.getTpa().getReceiverFeedback();
        if (receiverFeedback.isMessage()) {
            var tpa = ctx.messagesFor(target).getTpa();
            String template = here ? tpa.getRequestHereBody() : tpa.getRequestAskBody();
            chat(target, m -> m.getTpa().getRequestReceived(), Map.of(
                    "body", template.replace("{player}", sender.getName()),
                    "time", time,
                    "accept", RequestPrompts.accept(sender.getName(), tpa.getAcceptText(), tpa.getAcceptHover()),
                    "reject", RequestPrompts.reject(sender.getName(), tpa.getRejectText(), tpa.getRejectHover())));
        }
        if (receiverFeedback.isActionbar()) {
            bar(target, m -> m.getTpa().getRequestReceivedActionbar(), Map.of("player", sender.getName()));
        }
        ctx.soundPlayer().play(target, ctx.sounds().getAsk());
    }

    /**
     * Tells the sender and the receiver about a cancelled request.
     *
     * @param sender the original sender
     * @param request the cancelled request
     */
    public void cancelFeedback(Player sender, TpaRequest request) {
        chat(sender, m -> m.getTpa().getCancelSender(), Map.of("player", request.receiverName()));
        ctx.soundPlayer().play(sender, ctx.sounds().getNeutral());
        Player receiver = playerLookup.apply(request.receiver());
        if (receiver != null) {
            chat(receiver, m -> m.getTpa().getCancelReceiver(), Map.of("player", request.senderName()));
            ctx.soundPlayer().play(receiver, ctx.sounds().getNeutral());
        }
    }

    /**
     * Warns the sender about an unresponsive receiver before the
     * request goes out.
     *
     * @param source the command source
     * @param target the receiving player
     * @param here whether the receiver teleports to the sender
     * @param kind the warning kind
     */
    public void warnFeedback(
            CommandSourceStack source, Player target, boolean here, TpaWarnManager.WarnKind kind) {
        Function<MessagesConfig, String> selector = kind == TpaWarnManager.WarnKind.AFK
                ? m -> m.getTpa().getWarnAfk()
                : m -> m.getTpa().getWarnCanceller();
        var tpa = ctx.messagesFor(source.getSender()).getTpa();
        String type = here ? tpa.getRequestTypeHere() : tpa.getRequestTypeAsk();
        ctx.send(source, selector, Map.of("player", target.getName(), "type", type));
    }

    /**
     * Nudges the receiver towards block-all mode with a clickable
     * button.
     *
     * @param receiver the receiving player
     * @param selector the nudge message
     */
    public void blockAllNudge(Player receiver, Function<MessagesConfig, String> selector) {
        var tpa = ctx.messagesFor(receiver).getTpa();
        String button = RequestPrompts.blockAll(tpa.getWarnBlockAllButton(), tpa.getBlockAllHover());
        chat(receiver, selector, Map.of("button", button));
        ctx.soundPlayer().play(receiver, ctx.sounds().getNeutral());
    }

    /**
     * Nudges the receiver towards blocking a repeated sender with a
     * clickable button.
     *
     * @param receiver the receiving player
     * @param senderName the repeated sender's name
     */
    public void harassNudge(Player receiver, String senderName) {
        var tpa = ctx.messagesFor(receiver).getTpa();
        String text = tpa.getWarnBlockPlayerButton().replace("{player}", senderName);
        chat(receiver, m -> m.getTpa().getNudgeHarass(), Map.of(
                "player",
                senderName,
                "button",
                RequestPrompts.blockPlayer(senderName, text, tpa.getBlockPlayerHover())));
        ctx.soundPlayer().play(receiver, ctx.sounds().getNeutral());
    }

    private void chat(CommandSender to,
                      Function<MessagesConfig, String> selector,
                      Map<String, String> tags) {
        ctx.messenger().send(to, selector.apply(ctx.messagesFor(to)), tags);
    }

    private void bar(CommandSender to,
                     Function<MessagesConfig, String> selector,
                     Map<String, String> tags) {
        ctx.messenger().sendActionBar(to, selector.apply(ctx.messagesFor(to)), tags);
    }
}

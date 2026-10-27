package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.api.TpaRequest;
import com.jruk8.juicytpa.api.TpaRequestAcceptEvent;
import com.jruk8.juicytpa.api.TpaRequestSendEvent;
import com.jruk8.juicytpa.command.CommandContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Fires the cancellable request events and tells the acting player
 * when a listener vetoes the action.
 */
public final class TpaEventGate {

    private final CommandContext ctx;
    private final Consumer<Event> events;

    public TpaEventGate(CommandContext ctx, Consumer<Event> events) {
        this.ctx = ctx;
        this.events = events;
    }

    /**
     * Fires the send event for a validated request.
     *
     * @param request the request about to be delivered
     * @param sender the acting player, told on veto
     * @param target the receiving player
     * @return whether a listener vetoed the request
     */
    public boolean vetoedSend(TpaRequest request, Player sender, Player target) {
        var event = new TpaRequestSendEvent(request, sender, target);
        events.accept(event);
        if (!event.isCancelled()) {
            return false;
        }
        sendVetoFeedback(sender, event.getDenyMessage());
        return true;
    }

    /**
     * Fires the accept event for a pending request.
     *
     * @param request the request about to teleport
     * @param sender the original sender
     * @param receiver the answering player
     * @param notify the acting player, told on veto
     * @return whether a listener vetoed the teleport
     */
    public boolean vetoedAccept(TpaRequest request, Player sender, Player receiver, Player notify) {
        var event = new TpaRequestAcceptEvent(request, sender, receiver);
        events.accept(event);
        if (!event.isCancelled()) {
            return false;
        }
        sendVetoFeedback(notify, event.getDenyMessage());
        return true;
    }

    private void sendVetoFeedback(Player notify, Component denyMessage) {
        if (denyMessage != null) {
            notify.sendMessage(denyMessage);
            return;
        }
        ctx.messenger().send(notify, ctx.messagesFor(notify).getTpa().getExternalDenied(), Map.of());
    }
}

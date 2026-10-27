package com.jruk8.juicytpa.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Fires after the internal checks pass and before a request is stored
 * or announced. This is the eligibility gate: cancel it to veto the
 * request (from /tpa, /tpahere, /tpall or @a), optionally with a custom
 * deny message for the sender. A vetoed request is never stored, never
 * announced and never starts a cooldown.
 */
public class TpaRequestSendEvent extends TpaRequestEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;
    private Component denyMessage;

    public TpaRequestSendEvent(TpaRequest request, Player sender, Player receiver) {
        super(request, sender, receiver);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * @return the custom veto feedback for the sender, or null for the default message
     */
    public Component getDenyMessage() {
        return denyMessage;
    }

    /**
     * @param denyMessage the custom veto feedback for the sender, or null for the default message
     */
    public void setDenyMessage(Component denyMessage) {
        this.denyMessage = denyMessage;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

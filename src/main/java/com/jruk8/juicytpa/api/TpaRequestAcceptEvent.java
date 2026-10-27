package com.jruk8.juicytpa.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Fires when a pending request is accepted and before the teleport runs,
 * for manual accepts (/tpaccept), accept-all mode and trusted asks alike.
 * Cancel it to veto the teleport: the request stays pending for manual
 * accepts, or is dropped for instant accepts, and the acting player is
 * told with the deny message.
 */
public class TpaRequestAcceptEvent extends TpaRequestEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;
    private Component denyMessage;

    public TpaRequestAcceptEvent(TpaRequest request, Player sender, Player receiver) {
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
     * @return the custom veto feedback, or null for the default message
     */
    public Component getDenyMessage() {
        return denyMessage;
    }

    /**
     * @param denyMessage the custom veto feedback, or null for the default message
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

package com.jruk8.juicytpa.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fires after a pending request is denied with /tpadeny. This is a
 * notification only; the request is already gone.
 */
public class TpaRequestDenyEvent extends TpaRequestEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public TpaRequestDenyEvent(TpaRequest request, Player sender, Player receiver) {
        super(request, sender, receiver);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

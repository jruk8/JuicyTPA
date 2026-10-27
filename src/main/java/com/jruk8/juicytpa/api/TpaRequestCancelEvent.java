package com.jruk8.juicytpa.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fires after a pending request is removed without a teleport and
 * without a deny: an explicit /tpacancel, an accept-sweep of the
 * receiver's other requests, or a block dropping incoming requests.
 * This is a notification only; the request is already gone.
 */
public class TpaRequestCancelEvent extends TpaRequestEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Cause cause;

    public TpaRequestCancelEvent(TpaRequest request, Player sender, Player receiver, Cause cause) {
        super(request, sender, receiver);
        this.cause = cause;
    }

    /**
     * @return why the request was removed, never null
     */
    public Cause getCause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Why a request was cancelled.
     */
    public enum Cause {

        /** The sender ran /tpacancel. */
        CANCEL_COMMAND,

        /** The receiver accepted another request and sweeps were on. */
        ACCEPT_SWEEP,

        /** The receiver blocked the sender. */
        BLOCK
    }
}

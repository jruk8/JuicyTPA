package com.jruk8.juicytpa.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

/**
 * Base of every JuicyTPA request event. Carries the request plus the
 * two players involved; either player is null when they went offline
 * before the event fired.
 */
public abstract class TpaRequestEvent extends Event {

    private final TpaRequest request;
    private final Player sender;
    private final Player receiver;

    protected TpaRequestEvent(TpaRequest request, Player sender, Player receiver) {
        this.request = request;
        this.sender = sender;
        this.receiver = receiver;
    }

    /**
     * @return the request this event is about, never null
     */
    public TpaRequest getRequest() {
        return request;
    }

    /**
     * @return the player that sent the request, or null when offline
     */
    public Player getSender() {
        return sender;
    }

    /**
     * @return the player that must answer the request, or null when offline
     */
    public Player getReceiver() {
        return receiver;
    }
}

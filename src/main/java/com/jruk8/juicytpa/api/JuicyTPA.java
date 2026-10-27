package com.jruk8.juicytpa.api;

import java.util.List;
import java.util.UUID;

/**
 * The public JuicyTPA API. Look it up through the Bukkit services
 * manager (see the API docs); every implementation is backed by the
 * live request service, so queries always see current state.
 * <p>
 * Call every method from the server thread. Listeners of the request
 * events and Brigadier executors already run there.
 */
public interface JuicyTPA {

    /**
     * @param receiver the receiving player
     * @return their pending incoming requests, oldest first
     */
    List<TpaRequest> incoming(UUID receiver);

    /**
     * @param sender the sending player
     * @return their pending outgoing requests, oldest first
     */
    List<TpaRequest> outgoing(UUID sender);

    /**
     * @param player the player
     * @return how they receive TP requests
     */
    TpaMode mode(UUID player);

    /**
     * @param owner the blocking player
     * @param player the maybe-blocked player
     * @return whether the owner blocked the player
     */
    boolean isBlocked(UUID owner, UUID player);

    /**
     * @param owner the trusting player
     * @param player the maybe-trusted player
     * @return whether the owner trusts the player's TP asks
     */
    boolean isTrusted(UUID owner, UUID player);

    /**
     * Accepts the receiver's latest request from the sender, exactly as
     * /tpaccept would: the accept event fires (a veto keeps the request
     * pending), then the teleport runs. Fails when nothing matches, when
     * the receiver is offline, or when the sender went offline.
     *
     * @param receiver the answering player
     * @param sender the original sender
     * @return whether a request was accepted
     */
    boolean accept(UUID receiver, UUID sender);

    /**
     * Denies the receiver's latest request from the sender, exactly as
     * /tpadeny would: the request is removed and the deny event fires.
     *
     * @param receiver the answering player
     * @param sender the original sender
     * @return whether a request was denied
     */
    boolean deny(UUID receiver, UUID sender);

    /**
     * Cancels the sender's latest request to the receiver, exactly as
     * /tpacancel would: the request is removed and a command-cause
     * cancel event fires.
     *
     * @param sender the original sender
     * @param receiver the receiving player
     * @return whether a request was cancelled
     */
    boolean cancel(UUID sender, UUID receiver);
}

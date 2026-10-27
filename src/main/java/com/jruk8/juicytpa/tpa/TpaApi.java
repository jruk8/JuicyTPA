package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.api.JuicyTPA;
import com.jruk8.juicytpa.api.TpaMode;
import com.jruk8.juicytpa.api.TpaRequest;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * The live {@link JuicyTPA} implementation, registered with the
 * Bukkit services manager. Queries read current state; actions run
 * the same flows as the matching commands.
 */
public final class TpaApi implements JuicyTPA {

    private final TpaService service;
    private final TpaModeStore modes;
    private final TpaBlockStore blocks;
    private final TpaTrustStore trusts;
    private final Function<UUID, Player> playerLookup;

    public TpaApi(
            TpaService service,
            TpaModeStore modes,
            TpaBlockStore blocks,
            TpaTrustStore trusts,
            Function<UUID, Player> playerLookup
    ) {
        this.service = service;
        this.modes = modes;
        this.blocks = blocks;
        this.trusts = trusts;
        this.playerLookup = playerLookup;
    }

    @Override
    public List<TpaRequest> incoming(UUID receiver) {
        return service.incoming(receiver);
    }

    @Override
    public List<TpaRequest> outgoing(UUID sender) {
        return service.outgoing(sender);
    }

    @Override
    public TpaMode mode(UUID player) {
        return modes.mode(player);
    }

    @Override
    public boolean isBlocked(UUID owner, UUID player) {
        return blocks.isBlocked(owner, player);
    }

    @Override
    public boolean isTrusted(UUID owner, UUID player) {
        return trusts.isTrusted(owner, player);
    }

    @Override
    public boolean accept(UUID receiver, UUID sender) {
        Player actor = playerLookup.apply(receiver);
        if (actor == null) {
            return false;
        }
        var match = service.latestFrom(receiver, sender);
        if (match.isEmpty()) {
            return false;
        }
        Player senderPlayer = playerLookup.apply(sender);
        if (senderPlayer == null) {
            service.remove(match.get());
            return false;
        }
        if (service.eventGate().vetoedAccept(match.get(), senderPlayer, actor, actor)) {
            return false;
        }
        service.acceptRequest(match.get());
        return true;
    }

    @Override
    public boolean deny(UUID receiver, UUID sender) {
        Player actor = playerLookup.apply(receiver);
        if (actor == null) {
            return false;
        }
        var match = service.latestFrom(receiver, sender);
        if (match.isEmpty()) {
            return false;
        }
        service.denyRequest(actor, match.get());
        return true;
    }

    @Override
    public boolean cancel(UUID sender, UUID receiver) {
        Player actor = playerLookup.apply(sender);
        if (actor == null) {
            return false;
        }
        var match = service.latestTo(sender, receiver);
        if (match.isEmpty()) {
            return false;
        }
        service.cancelRequest(actor, match.get());
        return true;
    }
}

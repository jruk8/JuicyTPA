package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.api.TpaRequest;
import com.jruk8.juicytpa.command.CommandContext;
import com.jruk8.juicytpa.message.MessagesConfig;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Lists a player's pending requests grouped by direction and type,
 * with clickable names, plus their trusted players first.
 */
public final class TpaListService {

    private final CommandContext ctx;
    private final TpaTrustStore trusts;
    private final Function<UUID, String> nameLookup;
    private final Function<UUID, List<TpaRequest>> incoming;
    private final Function<UUID, List<TpaRequest>> outgoing;

    public TpaListService(
            CommandContext ctx,
            TpaTrustStore trusts,
            Function<UUID, String> nameLookup,
            Function<UUID, List<TpaRequest>> incoming,
            Function<UUID, List<TpaRequest>> outgoing
    ) {
        this.ctx = ctx;
        this.trusts = trusts;
        this.nameLookup = nameLookup;
        this.incoming = incoming;
        this.outgoing = outgoing;
    }

    /**
     * Lists the executor's pending incoming and outgoing requests,
     * grouped by direction and request type with clickable names.
     *
     * @param source the command source
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int list(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player viewer)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        List<TpaRequest> in = incoming.apply(viewer.getUniqueId());
        List<TpaRequest> out = outgoing.apply(viewer.getUniqueId());
        List<UUID> trustedIds = trusts.trusted(viewer.getUniqueId());
        if (in.isEmpty() && out.isEmpty() && trustedIds.isEmpty()) {
            ctx.send(source, m -> m.getTpa().getListEmpty(), Map.of());
            return Command.SINGLE_SUCCESS;
        }
        chat(viewer, m -> m.getTpa().getListHeader(), Map.of());
        sendTrustedLine(viewer, trustedIds);
        sendListLines(viewer, in, true);
        sendListLines(viewer, out, false);
        ctx.soundPlayer().play(viewer, ctx.sounds().getNeutral());
        return Command.SINGLE_SUCCESS;
    }

    private void sendListLines(Player viewer, List<TpaRequest> requests, boolean inbound) {
        sendListLine(viewer, requests, false, inbound);
        sendListLine(viewer, requests, true, inbound);
    }

    private void sendTrustedLine(Player viewer, List<UUID> trustedIds) {
        List<String> names = new ArrayList<>();
        for (String name : resolveNames(trustedIds)) {
            names.add(RequestPrompts.listTrust(name));
        }
        if (names.isEmpty()) {
            return;
        }
        var tpa = ctx.messages().getTpa();
        chat(viewer, m -> m.getTpa().getListLine(), Map.of(
                "category", tpa.getListTrusted(),
                "players", TpaListFormatter.join(names, tpa.getListComma(), tpa.getListAnd(), tpa.getListMore())));
    }

    private List<String> resolveNames(List<UUID> ids) {
        List<String> names = new ArrayList<>();
        for (UUID id : ids) {
            String name = this.nameLookup.apply(id);
            if (name != null) {
                names.add(name);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private void sendListLine(Player viewer, List<TpaRequest> requests, boolean here, boolean inbound) {
        List<String> names = new ArrayList<>();
        for (TpaRequest request : requests) {
            if (request.here() != here) {
                continue;
            }
            String name = inbound ? request.senderName() : request.receiverName();
            names.add(inbound ? RequestPrompts.listAccept(name) : RequestPrompts.listCancel(name));
        }
        if (names.isEmpty()) {
            return;
        }
        var tpa = ctx.messages().getTpa();
        String category;
        if (inbound) {
            category = here ? tpa.getListIncomingHere() : tpa.getListIncomingAsk();
        } else {
            category = here ? tpa.getListOutgoingAsk() : tpa.getListOutgoingHere();
        }
        chat(viewer, m -> m.getTpa().getListLine(), Map.of(
                "category", category,
                "players", TpaListFormatter.join(names, tpa.getListComma(), tpa.getListAnd(), tpa.getListMore())));
    }

    private void chat(CommandSender to,
                      Function<MessagesConfig, String> selector,
                      Map<String, String> tags) {
        ctx.messenger().send(to, selector.apply(ctx.messages()), tags);
    }
}

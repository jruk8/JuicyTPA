package com.jruk8.juicytpa.core.commands.tpa;

import com.jruk8.juicytpa.core.commands.TpaPermissions;
import com.jruk8.juicytpa.core.commands.selectors.ValueSuggestions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab-completion candidates for the TPA commands. Request targets list
 * every online player but the sender; answering and cancelling list only
 * the players with a matching pending request. Candidates are always
 * prefix-filtered.
 */
public final class TpaSuggestions {

    private TpaSuggestions() {
    }

    /**
     * Candidates for /tpa and /tpahere: vanilla selectors, then every
     * online player but the sender. {@code @a} only shows for senders
     * holding the tpall permission.
     *
     * @param source the command source completing the argument
     * @param prefix the already-typed input used for filtering (may be null)
     * @return the filtered candidates
     */
    public static List<String> suggestTargets(CommandSourceStack source, String prefix) {
        CommandSender sender = source.getSender();
        String name = sender instanceof Player player ? player.getName() : "";
        List<String> candidates = new ArrayList<>(List.of("@p", "@r", "@s"));
        if (sender.hasPermission(TpaPermissions.TPALL)) {
            candidates.add(1, "@a");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getName().equals(name)) {
                candidates.add(player.getName());
            }
        }
        return ValueSuggestions.suggest(candidates, prefix);
    }

    /**
     * Candidates for /tpablock: every online player but the sender.
     *
     * @param source the command source completing the argument
     * @param prefix the already-typed input used for filtering (may be null)
     * @return the filtered candidates
     */
    public static List<String> suggestNames(CommandSourceStack source, String prefix) {
        CommandSender sender = source.getSender();
        String name = sender instanceof Player player ? player.getName() : "";
        List<String> candidates = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getName().equals(name)) {
                candidates.add(player.getName());
            }
        }
        return ValueSuggestions.suggest(candidates, prefix);
    }

    /**
     * Candidates for /tpaccept and /tpadeny: senders with a pending
     * incoming request, most recent first.
     *
     * @param service the request service
     * @param source the command source completing the argument
     * @param prefix the already-typed input used for filtering (may be null)
     * @return the filtered candidates
     */
    public static List<String> suggestIncoming(
            TpaService service, CommandSourceStack source, String prefix) {
        if (!(source.getSender() instanceof Player player)) {
            return List.of();
        }
        List<String> candidates = new ArrayList<>();
        List<TpaRequest> incoming = service.incoming(player.getUniqueId());
        for (int index = incoming.size() - 1; index >= 0; index--) {
            String name = incoming.get(index).senderName();
            if (!candidates.contains(name)) {
                candidates.add(name);
            }
        }
        return ValueSuggestions.suggest(candidates, prefix);
    }

    /**
     * Candidates for /tpacancel: receivers with a pending outgoing
     * request, most recent first.
     *
     * @param service the request service
     * @param source the command source completing the argument
     * @param prefix the already-typed input used for filtering (may be null)
     * @return the filtered candidates
     */
    public static List<String> suggestOutgoing(
            TpaService service, CommandSourceStack source, String prefix) {
        if (!(source.getSender() instanceof Player player)) {
            return List.of();
        }
        List<String> candidates = new ArrayList<>();
        List<TpaRequest> outgoing = service.outgoing(player.getUniqueId());
        for (int index = outgoing.size() - 1; index >= 0; index--) {
            String name = outgoing.get(index).receiverName();
            if (!candidates.contains(name)) {
                candidates.add(name);
            }
        }
        return ValueSuggestions.suggest(candidates, prefix);
    }

    /**
     * Candidates for /tpamode: the three mode names.
     *
     * @param prefix the already-typed input used for filtering (may be null)
     * @return the filtered candidates
     */
    public static List<String> suggestModes(String prefix) {
        return ValueSuggestions.suggest(TpaMode.names(), prefix);
    }
}

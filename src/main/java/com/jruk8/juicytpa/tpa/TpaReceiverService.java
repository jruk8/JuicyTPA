package com.jruk8.juicytpa.tpa;

import com.jruk8.juicytpa.command.CommandContext;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import com.jruk8.juicytpa.api.TpaMode;

/**
 * Receiver-side preferences: TPA modes and per-player trust. Split
 * out of {@link TpaService} so request lifecycle and receiving
 * preferences evolve separately.
 */
public final class TpaReceiverService {

    private final CommandContext ctx;
    private final TpaModeStore modes;
    private final TpaTrustStore trusts;

    /**
     * @param ctx the shared command context
     * @param modes the persisted mode store
     * @param trusts the persisted trust store
     */
    public TpaReceiverService(CommandContext ctx, TpaModeStore modes, TpaTrustStore trusts) {
        this.ctx = ctx;
        this.modes = modes;
        this.trusts = trusts;
    }

    /**
     * Cycles the executor's mode, or sets it from the argument.
     *
     * @param source the command source
     * @param input the raw mode argument, or empty for argless
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int mode(CommandSourceStack source, Optional<String> input) {
        if (!(source.getSender() instanceof Player player)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        TpaMode mode;
        if (input.isEmpty()) {
            mode = modes.mode(player.getUniqueId()).next();
        } else {
            Optional<TpaMode> parsed = TpaMode.parse(input.get());
            if (parsed.isEmpty()) {
                ctx.error(source, m -> m.getTpa().getModeInvalid(), Map.of("input", input.get()));
                return 0;
            }
            mode = parsed.get();
        }
        modes.setMode(player.getUniqueId(), mode);
        ctx.send(source, m -> m.getTpa().getModeSet(), Map.of("mode", ctx.messages().getTpa().modeName(mode)));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Flips the executor between accept-all mode and normal mode.
     *
     * @param source the command source
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int autoMode(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        TpaMode mode = modes.mode(player.getUniqueId()) == TpaMode.ACCEPT_ALL ? TpaMode.NORMAL : TpaMode.ACCEPT_ALL;
        modes.setMode(player.getUniqueId(), mode);
        ctx.send(source, m -> m.getTpa().getModeSet(), Map.of("mode", ctx.messages().getTpa().modeName(mode)));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Flips the executor between block-all mode and normal mode.
     *
     * @param source the command source
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int blockAllMode(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        TpaMode mode = modes.mode(player.getUniqueId()) == TpaMode.BLOCK_ALL ? TpaMode.NORMAL : TpaMode.BLOCK_ALL;
        modes.setMode(player.getUniqueId(), mode);
        ctx.send(source, m -> m.getTpa().getModeSet(), Map.of("mode", ctx.messages().getTpa().modeName(mode)));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Toggles the trust between the executor and the target. Trusting
     * auto-accepts the target's future asks; pending requests stay put.
     *
     * @param source the command source
     * @param target the trusted or untrusted player
     * @return {@link Command#SINGLE_SUCCESS}, or 0 when a check fails
     */
    public int trust(CommandSourceStack source, Player target) {
        if (!(source.getSender() instanceof Player truster)) {
            ctx.error(source, m -> m.getTpa().getPlayerOnly(), Map.of());
            return 0;
        }
        if (target.getUniqueId().equals(truster.getUniqueId())) {
            ctx.error(source, m -> m.getTpa().getTrustSelf(), Map.of());
            return 0;
        }
        if (trusts.isTrusted(truster.getUniqueId(), target.getUniqueId())) {
            trusts.setTrusted(truster.getUniqueId(), target.getUniqueId(), false);
            ctx.send(source, m -> m.getTpa().getTrustRemoved(), Map.of("player", target.getName()));
            return Command.SINGLE_SUCCESS;
        }
        trusts.setTrusted(truster.getUniqueId(), target.getUniqueId(), true);
        ctx.send(source, m -> m.getTpa().getTrustAdded(), Map.of("player", target.getName()));
        return Command.SINGLE_SUCCESS;
    }
}

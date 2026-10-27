package com.jruk8.juicytpa.core.commands.tpa;

import java.util.UUID;

/**
 * One pending teleport request. A plain {@code /tpa} moves the sender to
 * the receiver; a {@code /tpahere} request ({@code here}) moves the
 * receiver to the sender instead.
 *
 * @param sender the player that sent the request
 * @param senderName the sender's name when the request was sent
 * @param receiver the player that must answer the request
 * @param receiverName the receiver's name when the request was sent
 * @param here whether the receiver teleports to the sender
 */
public record TpaRequest(
        UUID sender,
        String senderName,
        UUID receiver,
        String receiverName,
        boolean here
) {
}

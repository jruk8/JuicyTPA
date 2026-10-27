package com.jruk8.juicytpa.core.commands.tpa;

/**
 * Builds the clickable {@code {accept}} and {@code {reject}} values for the
 * request notice, plus the {@code {button}} values for warning nudges. The
 * links run {@code /tpaccept}, {@code /tpadeny}, {@code /tpamode} and
 * {@code /tpablock} by name, so they keep working when several requests
 * are pending. Color references ({@code {ac}}, {@code {hl}}, {@code {rc}},
 * {@code {lh}}) resolve later in the message pipeline.
 */
public final class RequestPrompts {

    private RequestPrompts() {
    }

    /**
     * Builds the accept prompt for one sender.
     *
     * @param senderName the request sender's name
     * @param text the clickable button text from messages.yml
     * @return the raw MiniMessage accept prompt
     */
    public static String accept(String senderName, String text) {
        return "<click:run_command:'/tpaccept " + senderName + "'>"
                + "<hover:show_text:'{ac}Click to accept {hl}" + senderName + "{ac}.'>"
                + "{ac}" + text + "</hover></click>";
    }

    /**
     * Builds the reject prompt for one sender.
     *
     * @param senderName the request sender's name
     * @param text the clickable button text from messages.yml
     * @return the raw MiniMessage reject prompt
     */
    public static String reject(String senderName, String text) {
        return "<click:run_command:'/tpadeny " + senderName + "'>"
                + "<hover:show_text:'{rc}Click to reject {hl}" + senderName + "{rc}.'>"
                + "{rc}" + text + "</hover></click>";
    }

    /**
     * Builds the block-all prompt for warning nudges.
     *
     * @param text the clickable button text from messages.yml
     * @return the raw MiniMessage block-all prompt
     */
    public static String blockAll(String text) {
        return "<click:run_command:'/tpamode block'>"
                + "<hover:show_text:'{lh}Click to block all requests.'>"
                + text + "</hover></click>";
    }

    /**
     * Builds the block-player prompt for harassment nudges.
     *
     * @param targetName the player the button blocks
     * @param text the clickable button text from messages.yml
     * @return the raw MiniMessage block-player prompt
     */
    public static String blockPlayer(String targetName, String text) {
        return "<click:run_command:'/tpablock " + targetName + "'>"
                + "<hover:show_text:'{lh}Click to block {hl}" + targetName + "{lh}.'>"
                + text + "</hover></click>";
    }

    /**
     * Builds one clickable /tpalist name running /tpaccept.
     *
     * @param playerName the player to accept
     * @return the raw MiniMessage list entry
     */
    public static String listAccept(String playerName) {
        return "<click:run_command:'/tpaccept " + playerName + "'>"
                + "<hover:show_text:'{ac}Click to accept {hl}" + playerName + "{ac}.'>"
                + playerName + "</hover></click>";
    }

    /**
     * Builds one clickable /tpalist name running /tpacancel.
     *
     * @param playerName the player to cancel
     * @return the raw MiniMessage list entry
     */
    public static String listCancel(String playerName) {
        return "<click:run_command:'/tpacancel " + playerName + "'>"
                + "<hover:show_text:'{sc}Click to cancel {hl}" + playerName + "{sc}.'>"
                + playerName + "</hover></click>";
    }
}

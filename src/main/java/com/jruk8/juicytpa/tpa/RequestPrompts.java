package com.jruk8.juicytpa.tpa;

/**
 * Builds the clickable {@code {accept}} and {@code {reject}} values for the
 * request notice, plus the {@code {button}} values for warning nudges. The
 * links run {@code /tpaccept}, {@code /tpadeny}, {@code /tpamode} and
 * {@code /tpblock} by name, so they keep working when several requests
 * are pending. Button text and hover tooltips come from the recipient's
 * bundle; color references resolve later in the message pipeline.
 */
public final class RequestPrompts {

    private RequestPrompts() {
    }

    /**
     * Builds the accept prompt for one sender.
     *
     * @param senderName the request sender's name
     * @param text the clickable button text from messages.yml
     * @param hover the hover tooltip template from messages.yml (use {player})
     * @return the raw MiniMessage accept prompt
     */
    public static String accept(String senderName, String text, String hover) {
        return "<click:run_command:'/tpaccept " + senderName + "'>"
                + "<hover:show_text:'" + hover.replace("{player}", senderName) + "'>"
                + "{ac}" + text + "</hover></click>";
    }

    /**
     * Builds the reject prompt for one sender.
     *
     * @param senderName the request sender's name
     * @param text the clickable button text from messages.yml
     * @param hover the hover tooltip template from messages.yml (use {player})
     * @return the raw MiniMessage reject prompt
     */
    public static String reject(String senderName, String text, String hover) {
        return "<click:run_command:'/tpadeny " + senderName + "'>"
                + "<hover:show_text:'" + hover.replace("{player}", senderName) + "'>"
                + "{rc}" + text + "</hover></click>";
    }

    /**
     * Builds the block-all prompt for warning nudges.
     *
     * @param text the clickable button text from messages.yml
     * @param hover the hover tooltip template from messages.yml
     * @return the raw MiniMessage block-all prompt
     */
    public static String blockAll(String text, String hover) {
        return "<click:run_command:'/tpamode block'>"
                + "<hover:show_text:'" + hover + "'>"
                + text + "</hover></click>";
    }

    /**
     * Builds the block-player prompt for harassment nudges.
     *
     * @param targetName the player the button blocks
     * @param text the clickable button text from messages.yml
     * @param hover the hover tooltip template from messages.yml (use {player})
     * @return the raw MiniMessage block-player prompt
     */
    public static String blockPlayer(String targetName, String text, String hover) {
        return "<click:run_command:'/tpablock " + targetName + "'>"
                + "<hover:show_text:'" + hover.replace("{player}", targetName) + "'>"
                + text + "</hover></click>";
    }

    /**
     * Builds one clickable /tpalist name running /tpaccept.
     *
     * @param playerName the player to accept
     * @param hover the hover tooltip template from messages.yml (use {player})
     * @return the raw MiniMessage list entry
     */
    public static String listAccept(String playerName, String hover) {
        return "<click:run_command:'/tpaccept " + playerName + "'>"
                + "<hover:show_text:'" + hover.replace("{player}", playerName) + "'>"
                + playerName + "</hover></click>";
    }

    /**
     * Builds one clickable /tpalist name running /tpacancel.
     *
     * @param playerName the player to cancel
     * @param hover the hover tooltip template from messages.yml (use {player})
     * @return the raw MiniMessage list entry
     */
    public static String listCancel(String playerName, String hover) {
        return "<click:run_command:'/tpacancel " + playerName + "'>"
                + "<hover:show_text:'" + hover.replace("{player}", playerName) + "'>"
                + playerName + "</hover></click>";
    }

    /**
     * Builds one clickable /tpalist name running /tpatrust.
     *
     * @param playerName the player to untrust
     * @param hover the hover tooltip template from messages.yml (use {player})
     * @return the raw MiniMessage list entry
     */
    public static String listTrust(String playerName, String hover) {
        return "<click:run_command:'/tpatrust " + playerName + "'>"
                + "<hover:show_text:'" + hover.replace("{player}", playerName) + "'>"
                + playerName + "</hover></click>";
    }
}

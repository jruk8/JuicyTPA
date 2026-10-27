package com.jruk8.juicytpa.core.commands.tpa;

/**
 * Builds the clickable {@code {accept}} and {@code {reject}} values for the
 * request notice. The links run {@code /tpaccept} and {@code /tpadeny}
 * against the sender by name, so they keep working when several requests
 * are pending. Color references ({@code {ac}}, {@code {hl}}, {@code {rc}})
 * resolve later in the message pipeline.
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
}

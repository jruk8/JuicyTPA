package com.jruk8.juicytpa.core.commands;

/**
 * Permission nodes used by the JuicyTPA commands, declared in
 * {@code paper-plugin.yml}.
 */
public final class TpaPermissions {

    /**
     * Send /tpa requests.
     */
    public static final String TPA = "juicytpa.tpa";

    /**
     * Send /tpahere requests.
     */
    public static final String TPAHERE = "juicytpa.tpahere";

    /**
     * Accept incoming TP requests with /tpaccept.
     */
    public static final String ACCEPT = "juicytpa.accept";

    /**
     * Deny incoming TP requests with /tpadeny.
     */
    public static final String DENY = "juicytpa.deny";

    /**
     * Cancel outgoing TP requests with /tpacancel.
     */
    public static final String CANCEL = "juicytpa.cancel";

    /**
     * List pending TP requests with /tpalist.
     */
    public static final String LIST = "juicytpa.list";

    /**
     * Change your TPA mode with /tpamode.
     */
    public static final String MODE = "juicytpa.mode";

    /**
     * Block requests from specific players with /tpablock.
     */
    public static final String BLOCK = "juicytpa.block";

    /**
     * Trust specific players with /tpatrust.
     */
    public static final String TRUST = "juicytpa.trust";

    /**
     * Send requests to everyone with /tpall, and use @a in /tpa and /tpahere.
     */
    public static final String TPALL = "juicytpa.tpall";

    /**
     * Reload the plugin configuration.
     */
    public static final String RELOAD = "juicytpa.reload";

    /**
     * View the plugin help listing. Entries are filtered by permission.
     */
    public static final String HELP = "juicytpa.help";

    /**
     * View the plugin version.
     */
    public static final String VERSION = "juicytpa.version";

    private TpaPermissions() {
    }
}

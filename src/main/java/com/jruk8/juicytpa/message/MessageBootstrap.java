package com.jruk8.juicytpa.message;

import com.jruk8.juicytpa.config.ConfigRegistrar;
import com.jruk8.juicytpa.core.Bootstrap;

import lombok.Getter;

public class MessageBootstrap implements Bootstrap {

    @Getter
    private Messenger messenger;
    private final ConfigRegistrar config;
    private final MessageBundles bundles;

    public MessageBootstrap(ConfigRegistrar config, MessageBundles bundles) {
        this.config = config;
        this.bundles = bundles;
    }

    @Override
    public void register() {
        var parser = new MessageParser(config.getPluginConfig(), config.getMessagesConfig());
        this.messenger = new Messenger(parser, bundles);
    }
}

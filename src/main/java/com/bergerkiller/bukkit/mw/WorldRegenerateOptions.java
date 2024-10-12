package com.bergerkiller.bukkit.mw;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Configuration options for resetting a world before regenerating.
 * Tells what to do with the world.
 */
public class WorldRegenerateOptions implements Cloneable {
    private boolean resetSeed = false;

    public static WorldRegenerateOptions create() {
        return new WorldRegenerateOptions();
    }

    private WorldRegenerateOptions() {
    }

    public boolean isResetSeed() {
        return resetSeed;
    }

    public WorldRegenerateOptions resetSeed(boolean reset) {
        this.resetSeed = reset;
        return this;
    }

    @Override
    public WorldRegenerateOptions clone() {
        return create().resetSeed(this.isResetSeed());
    }

    public static WorldRegenerateOptions readFromConfig(ConfigurationNode config) {
        ConfigurationNode regenerateConfig = config.getNodeIfExists("regenerateOnStartup");
        if (regenerateConfig == null || !regenerateConfig.getOrDefault("enabled", false)) {
            return null;
        }

        WorldRegenerateOptions options = create();
        options.resetSeed(regenerateConfig.getOrDefault("resetSeed", options.isResetSeed()));
        return options;
    }

    public static void writeToConfig(ConfigurationNode config, WorldRegenerateOptions options) {
        if (options == null) {
            config.remove("regenerateOnStartup");
            return;
        }

        ConfigurationNode regenerateConfig = config.getNode("regenerateOnStartup");
        regenerateConfig.set("enabled", true);
        regenerateConfig.set("resetSeed", options.isResetSeed());
    }

    public static List<String> getExtraArgNames() {
        return Collections.singletonList("newseed");
    }

    public static WorldRegenerateOptions parseExtraArgs(CommandSender sender, String[] args) {
        WorldRegenerateOptions opt = create();
        for (String arg : args) {
            if ("newseed".equalsIgnoreCase(arg)) {
                opt.resetSeed(true);
                sender.sendMessage(ChatColor.GREEN + "A new random seed will be set for the world");
            } else {
                sender.sendMessage(ChatColor.RED + "Unknown option: " + arg);
                return null;
            }
        }
        return opt;
    }
}

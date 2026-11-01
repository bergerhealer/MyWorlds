package com.bergerkiller.bukkit.mw;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration options for resetting a world before regenerating.
 * Tells what to do with the world.
 */
public class WorldRegenerateOptions implements Cloneable {
    private boolean resetSeed = false;
    private boolean resetSpawn = true;

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

    public boolean isResetSpawn() {
        return resetSpawn;
    }

    public WorldRegenerateOptions resetSpawn(boolean reset) {
        this.resetSpawn = reset;
        return this;
    }

    @Override
    public WorldRegenerateOptions clone() {
        return create()
                .resetSeed(this.isResetSeed())
                .resetSpawn(this.isResetSpawn());
    }

    public static WorldRegenerateOptions readFromConfig(ConfigurationNode config) {
        ConfigurationNode regenerateConfig = config.getNodeIfExists("regenerateOnStartup");
        if (regenerateConfig == null || !regenerateConfig.getOrDefault("enabled", false)) {
            return null;
        }

        WorldRegenerateOptions options = create();
        options.resetSeed(regenerateConfig.getOrDefault("resetSeed", options.isResetSeed()));
        options.resetSpawn(regenerateConfig.getOrDefault("resetSpawn", options.isResetSpawn()));
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
        regenerateConfig.set("resetSpawn", options.isResetSpawn());
    }

    public static List<String> getExtraArgNames() {
        return Arrays.asList("newseed", "keepspawn", "resetspawn");
    }

    public static WorldRegenerateOptions parseExtraArgs(CommandSender sender, String[] args) {
        WorldRegenerateOptions opt = create();
        for (String arg : args) {
            if ("newseed".equalsIgnoreCase(arg)) {
                opt.resetSeed(true);
                sender.sendMessage(ChatColor.GREEN + "A new random seed will be set for the world");
            } else if ("keepspawn".equalsIgnoreCase(arg)) {
                opt.resetSpawn(false);
                sender.sendMessage(ChatColor.GREEN + "World spawn point will be kept");
            } else if ("resetspawn".equalsIgnoreCase(arg)) {
                opt.resetSpawn(true);
                sender.sendMessage(ChatColor.GREEN + "World spawn point will be reset");
            } else {
                sender.sendMessage(ChatColor.RED + "Unknown option: " + arg);
                return null;
            }
        }
        return opt;
    }
}

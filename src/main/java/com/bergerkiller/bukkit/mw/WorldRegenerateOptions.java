package com.bergerkiller.bukkit.mw;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Configuration options for resetting a world before regenerating.
 * Tells what to do with the world.
 */
public class WorldRegenerateOptions {
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

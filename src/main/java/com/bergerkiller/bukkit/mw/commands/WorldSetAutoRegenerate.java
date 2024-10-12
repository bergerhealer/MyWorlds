package com.bergerkiller.bukkit.mw.commands;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.WorldRegenerateOptions;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WorldSetAutoRegenerate extends Command {

    public WorldSetAutoRegenerate() {
        super(Permission.COMMAND_AUTOREGENERATE, "world.setautoregenerate");
    }

    public void execute() {
        if (args.length < 1) {
            showInv();
            return;
        }

        worldname = args[0];
        if (!WorldManager.worldExists(worldname)) {
            worldname = null;
        }
        if (!this.handleWorld()) {
            return;
        }
        this.removeArg(0);

        WorldConfig wc = WorldConfig.get(worldname);

        if (args.length > 0) {
            if (!ParseUtil.isBool(args[0])) {
                showInv();
                return;
            }

            WorldRegenerateOptions options = null;
            if (ParseUtil.parseBool(args[0])) {
                removeArg(0);

                options = WorldRegenerateOptions.parseExtraArgs(sender, args);
                if (options == null) {
                    return; // Invalid
                }
            }
            wc.setStartupRegenerateOptions(options);

            if (options != null) {
                logAction("Set world '" + worldname + "' to be regenerated on next server startup");
                showState(options, true);
                sender.sendMessage("");
                message(ChatColor.YELLOW + "This will delete the world's chunk data when the server reboots next time!");
                message(ChatColor.YELLOW + "If this was in error, run /mw setautoregenerate " + worldname + " disable");
                return; // No need to show same state again
            }
        }

        // Show current state
        {
            WorldRegenerateOptions options = wc.getStartupRegenerateOptions();
            if (options == null) {
                message(ChatColor.YELLOW + "World '" + worldname + "' is not set to be regenerated on startup");
            } else {
                showState(options, false);
            }
        }
    }

    private void showState(WorldRegenerateOptions options, boolean set) {
        MessageBuilder builder = new MessageBuilder();
        builder.yellow("World '").white(worldname).yellow("' ");
        if (set) {
            builder.red("has been set to be regenerated");
        } else {
            builder.red("is set to be regenerated");
        }
        builder.yellow(" on startup");
        if (options.isResetSeed()) {
            builder.blue(" with a new seed");
        }
        builder.yellow("!");
        builder.send(sender);
    }

    @Override
    public List<String> autocomplete() {
        if (args.length <= 1) {
            return processWorldNameAutocomplete();
        } else if (args.length >= 3 && "disable".equalsIgnoreCase(args[2])) {
            return Collections.emptyList();
        } else if (args.length == 2) {
            return Arrays.asList("enable", "disable");
        } else {
            return WorldRegenerateOptions.getExtraArgNames();
        }
    }
}

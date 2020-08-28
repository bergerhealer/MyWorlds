package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldReloadWE extends Command {
    
    public WorldReloadWE() {
        super(Permission.COMMAND_RELOADWE, "world.reloadwhenempty");
    }
    
    public void execute() {
        if (args.length != 0) {
            this.genWorldname(1);
            if (this.handleWorld()) {
                WorldConfig wc = WorldConfig.get(worldname);
                if (ParseUtil.isBool(args[0])) {
                    wc.reloadWhenEmpty = ParseUtil.parseBool(args[0]);
                    if (wc.reloadWhenEmpty) {
                        message(ChatColor.YELLOW + "The world '" + worldname + "' will now reload when it has no players.");
                    } else {
                        message(ChatColor.YELLOW + "The world '" + worldname + "' will no longer reload when it has no players.");
                    }
                } else {
                    if (wc.reloadWhenEmpty) {
                        message(ChatColor.YELLOW + "The world '" + worldname + "' reloads when it has no players.");
                    } else {
                        message(ChatColor.YELLOW + "The world '" + worldname + "' does not reload when it has no players.");
                    }
                }
            }
        } else {
            showInv();
        }
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName("yes", "no");
    }
}

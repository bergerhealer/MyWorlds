package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSetPVP extends Command {

    public WorldSetPVP() {
        super(Permission.COMMAND_TOGGLEPVP, "world.pvp");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length == 0) {
                // Display
                if (wc.pvp) {
                    message(ChatColor.GREEN + "PvP on World: '" + worldname + "' is enabled");
                } else {
                    message(ChatColor.YELLOW + "PvP on World: '" + worldname + "' is disabled");
                }
            } else {
                // Set
                wc.pvp = ParseUtil.parseBool(args[0]);
                wc.updatePVP(wc.getWorld());
                if (wc.pvp) {
                    message(ChatColor.GREEN + "PvP on World: '" + worldname + "' enabled!");
                } else {
                    message(ChatColor.YELLOW + "PvP on World: '" + worldname + "' disabled!");
                }
                if (!WorldManager.isLoaded(worldname)) {
                    message(ChatColor.YELLOW + "Please note that this world is not loaded!");
                }
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName("enabled", "disabled");
    }
}

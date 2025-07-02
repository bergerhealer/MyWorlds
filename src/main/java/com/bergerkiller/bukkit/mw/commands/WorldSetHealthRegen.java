package com.bergerkiller.bukkit.mw.commands;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;
import org.bukkit.ChatColor;

import java.util.List;

public class WorldSetHealthRegen extends Command {

    public WorldSetHealthRegen() {
        super(Permission.COMMAND_HEALTH_REGEN, "world.healthregen");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length == 0) {
                // Display
                if (wc.allowHealthRegen) {
                    message(ChatColor.GREEN + "Natural health regeneration on World: '" + worldname + "' is enabled");
                } else {
                    message(ChatColor.YELLOW + "Natural health regeneration on World: '" + worldname + "' is disabled");
                }
            } else {
                // Set
                wc.allowHealthRegen = ParseUtil.parseBool(args[0]);
                wc.updateHealthRegen(wc.getWorld());
                if (wc.allowHealthRegen) {
                    message(ChatColor.GREEN + "Natural health regeneration on World: '" + worldname + "' enabled!");
                } else {
                    message(ChatColor.YELLOW + "Natural health regeneration on World: '" + worldname + "' disabled!");
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

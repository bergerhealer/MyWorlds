package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldSetForcedRespawn extends Command {

    public WorldSetForcedRespawn() {
        super(Permission.COMMAND_TOGGLERESPAWN, "world.setforcedrespawn");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length > 0) {
                wc.forcedRespawn = ParseUtil.parseBool(args[0]);
            }
            if (wc.forcedRespawn) {
                message(ChatColor.GREEN + "Forced respawning on World: '" + worldname + "' is enabled");
                message(ChatColor.GREEN + "Players will respawn at the world's spawn");
            } else {
                message(ChatColor.YELLOW + "Forced respawning on World: '" + worldname + "' is disabled");
                message(ChatColor.GREEN + "Players will respawn at their bed");
            }
        }
    }
}

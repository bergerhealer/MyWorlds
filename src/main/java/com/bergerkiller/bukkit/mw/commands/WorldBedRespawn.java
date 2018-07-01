package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldBedRespawn extends Command {

    public WorldBedRespawn() {
        super(Permission.COMMAND_CHANGE_BEDRESPAWN, "world.bedrespawn");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length > 0) {
                wc.bedRespawnEnabled = ParseUtil.parseBool(args[0]);
                if (!wc.bedRespawnEnabled) {
                    World w = wc.getWorld();
                    if (w != null) {
                        for (Player p : w.getPlayers()) {
                            wc.updateBedSpawnPoint(p);
                        }
                    }
                }
            }
            if (wc.bedRespawnEnabled) {
                message(ChatColor.GREEN + "Respawning at last slept beds on World: '" + worldname + "' is enabled");
                message(ChatColor.GREEN + "Players will respawn at the bed they last slept in, if set");
            } else {
                message(ChatColor.GREEN + "Respawning at last slept beds on World: '" + worldname + "' is disabled");
                message(ChatColor.GREEN + "Players will respawn at the world's spawn or home point when dying");
            }
        }
    }
}

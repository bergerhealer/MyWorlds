package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldBedRespawn extends Command {

    public WorldBedRespawn() {
        super(Permission.COMMAND_SETSPAWN, "world.bedrespawn");
    }

    public void execute() {
        this.genWorldname(1);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (args.length > 0) {
                wc.bedRespawnEnabled = ParseUtil.parseBool(args[0]);
                if (!wc.bedRespawnEnabled) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        WorldManager.removeInvalidBedSpawnPoint(player);
                    }
                }
            }

            if (wc.bedRespawnEnabled) {
                message(ChatColor.YELLOW + "Respawning at last slept beds on World: '" + ChatColor.WHITE + worldname +
                        ChatColor.YELLOW + "' is " + ChatColor.GREEN + "ENABLED");
                message(ChatColor.GREEN + "Players will respawn at the bed they last slept in, if set");
            } else {
                message(ChatColor.YELLOW + "Respawning at last slept beds on World: '" + ChatColor.WHITE + worldname +
                        ChatColor.YELLOW + "' is " + ChatColor.RED + "DISABLED");
                message(ChatColor.YELLOW + "Players will respawn at the world's spawn or home point when dying");
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName("enabled", "disabled");
    }
}

package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.RespawnPoint;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldSetSpawn extends Command {

    public WorldSetSpawn() {
        super(Permission.COMMAND_SETSPAWN, "world.setspawn");
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    public void execute() {
        this.genWorldname(0);
        if (this.handleWorld()) {
            WorldConfig config = WorldConfig.get(worldname);
            if (config.getWorld() == player.getWorld()) {
                // Set spawn point of this World
                config.setSpawnLocation(player.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Spawn location of world '" + worldname + "' set to your position!");
            } else {
                // Set a respawn point from that world to the player's Location
                config.respawnPoint = new RespawnPoint.RespawnPointLocation(player.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Respawn location for world '" + worldname + "' set to your position!");
                sender.sendMessage(ChatColor.YELLOW + "Note: You can also use /world respawn [options...]");
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processWorldNameAutocomplete();
    }
}

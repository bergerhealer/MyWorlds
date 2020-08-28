package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Position;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSetSpawn extends Command {

    public WorldSetSpawn() {
        super(Permission.COMMAND_SETSPAWN, "world.setspawn");
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    public void execute() {
        Position pos = new Position(player.getLocation());
        this.genWorldname(0);
        if (this.handleWorld()) {
            WorldManager.setSpawn(worldname, pos);
            if (worldname.equalsIgnoreCase(player.getWorld().getName())) {
                player.getWorld().setSpawnLocation(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
            }
            sender.sendMessage(ChatColor.GREEN + "Spawn location of world '" + worldname + "' set to your position!");
        }
    }

    @Override
    public List<String> autocomplete() {
        return processWorldNameAutocomplete();
    }
}

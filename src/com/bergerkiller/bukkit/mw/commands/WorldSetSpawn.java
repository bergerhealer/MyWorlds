package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.Position;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSetSpawn extends Command {

	public WorldSetSpawn(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.setspawn";
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
	
}

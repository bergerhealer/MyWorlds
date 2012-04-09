package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSpawn extends Command {
	
	public WorldSpawn(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.spawn";
	}
		
	public void execute() {
		this.genWorldname(0);
		if (this.handleWorld()) {
			World world = WorldManager.getWorld(worldname);
			if (world != null) {
				if (Permission.handleTeleport(player, WorldManager.getSpawnLocation(world))) {
					//Success
				}
			} else {
				message(Localization.getWorldNotLoaded(worldname));
			}
		}
	}
	
}

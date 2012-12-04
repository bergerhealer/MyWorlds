package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.AsyncHandler;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldDelete extends Command {

	public WorldDelete() {
		super(Permission.COMMAND_DELETE, "world.delete");
	}

	public void execute() {
		if (args.length == 1) {
			worldname = args[0];
			if (!WorldManager.worldExists(worldname)) worldname = null;
			if (this.handleWorld()) {
				if (!WorldManager.isLoaded(worldname)) {
					logAction("Issued a world deletion command for world: " + worldname);
					WorldConfig.remove(worldname);
					AsyncHandler.delete(sender, worldname);
				} else {
					message(ChatColor.RED + "World is loaded, please unload the world first!");
				}
			}
		} else {
			showInv();
		}
	}
	
}

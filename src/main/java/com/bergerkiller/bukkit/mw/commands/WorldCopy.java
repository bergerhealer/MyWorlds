package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.AsyncHandler;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldCopy extends Command {

	public WorldCopy() {
		super(Permission.COMMAND_COPY, "world.copy");
	}
	
	public void execute() {
		if (args.length == 2) {
			worldname = args[0];
			if (!WorldManager.worldExists(worldname)) worldname = null;
			if (this.handleWorld()) {
				String newname = args[1];
				if (!WorldManager.worldExists(newname)) {
					logAction("Issued a world copy command for world: " + worldname + " to '" + newname + "'!");
					message(ChatColor.YELLOW + "Copying world '" + worldname + "' to '" + newname + "'...");
					AsyncHandler.copy(this.sender, this.worldname, newname);
				} else {
					message(ChatColor.RED + "Can not copy to an existing world!");
				}
			}
		} else {
			showInv();
		}
	}
	
}

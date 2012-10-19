package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldLoad extends Command {

	public WorldLoad() {
		super(Permission.COMMAND_LOAD, "world.load");
	}

	public void execute() {
		if (args.length != 0) {
			worldname = WorldManager.matchWorld(args[0]);
			if (this.handleWorld()) {
				if (WorldManager.isLoaded(worldname)) {
					message(ChatColor.YELLOW + "World '" + worldname + "' is already loaded!");
				} else {
					notifyConsole("Issued a load command for world: " + worldname);
					message(ChatColor.YELLOW + "Loading world: '" + worldname + "'...");
					if (WorldManager.createWorld(worldname, 0) != null) {
						message(ChatColor.GREEN + "World loaded!");
					} else {
						message(ChatColor.RED + "Failed to load world, it is probably broken!");
					}
				}
			}
		} else {
			showInv();
		}
	}
}

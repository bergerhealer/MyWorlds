package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.AsyncHandler;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldRepair extends Command {

	public WorldRepair() {
		super(Permission.COMMAND_REPAIR, "world.repair");
	}

	public void execute() {
		if (args.length != 0) {
			worldname = WorldManager.matchWorld(args[0]);
			//get seed
			String seed = "";
			for (int i = 1;i < args.length;i++) {
				if (seed != "") seed += " ";
				seed += args[i];
			}
			if (this.handleWorld()) {
				if (!WorldManager.isLoaded(worldname)) {
					AsyncHandler.repair(sender, worldname, WorldManager.getRandomSeed(seed));
				} else {
					message(ChatColor.YELLOW + "Can't repair a loaded world!");
				}
			}
		} else {
			showInv();
		}
	}
}

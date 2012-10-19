package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldPortals extends Command {

	public WorldPortals() {
		super(Permission.COMMAND_PORTALS, "world.portals");
	}

	public void execute() {
		if (args.length == 1) {
			World w = WorldManager.getWorld(WorldManager.matchWorld(args[0]));
			if (w != null) {
				listPortals(Portal.getPortals(w));
			} else {
				message(ChatColor.RED + "World not found!");
				return;
			}
		} else {
			listPortals(Portal.getPortals());
		}
	}
}

package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSetEnderPortal extends Command {
	public WorldSetEnderPortal() {
		super(Permission.COMMAND_SETPORTAL, "world.setendportal");
	}

	public void execute() {
		String dest = args.length > 0 ? args[0] : "";
		this.genWorldname(1);
		if (this.handleWorld()) {
			if (dest.isEmpty()) {
				WorldConfig.get(worldname).setEnderPortal("");
				message(ChatColor.GREEN + "Default ender portal teleportation of world '" + worldname + "' disabled! (no default teleport)");
			} else if (Portal.getPortalLocation(dest, null) != null) {
				WorldConfig.get(worldname).setEnderPortal(dest);
				message(ChatColor.GREEN + "Default ender portal destination of world '" + worldname + "' set to portal: '" + dest + "'!");
			} else if ((dest = WorldManager.matchWorld(dest)) != null) {
				WorldConfig.get(worldname).setEnderPortal(dest);
				message(ChatColor.GREEN + "Default ender portal destination of world '" + worldname + "' set to world: '" + dest + "'!");
				if (!WorldManager.isLoaded(dest)) {
					message(ChatColor.YELLOW + "Note that this world is not loaded, so nothing happens yet!");
				}
			} else {
				message(ChatColor.RED + "Destination is not a valid world or portal!");
			}
		}
	}
}

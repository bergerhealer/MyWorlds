package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSetPortal extends Command {

	public WorldSetPortal(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.setportal";
	}
		
	public void execute() {
		if (args.length != 0) {
			String dest = args[0];
			this.genWorldname(1);
			if (this.handleWorld()) {
				if (dest.equals("")) {
					WorldConfig.get(worldname).defaultPortal = null;
					message(ChatColor.GREEN + "Default destination of world '" + worldname + "' cleared!");
				} else if (Portal.getPortalLocation(dest, null) != null) {
					WorldConfig.get(worldname).defaultPortal = dest;
					message(ChatColor.GREEN + "Default destination of world '" + worldname + "' set to portal: '" + dest + "'!");
				} else if ((dest = WorldManager.matchWorld(dest)) != null) {
					WorldConfig.get(worldname).defaultPortal = dest;
					message(ChatColor.GREEN + "Default destination of world '" + worldname + "' set to world: '" + dest + "'!");
					if (!WorldManager.isLoaded(dest)) {
						message(ChatColor.YELLOW + "Note that this world is not loaded, so nothing happens yet!");
					}
				} else {
					message(ChatColor.RED + "Destination is not a world or portal!");
				}
			}
		} else {
			showInv();
		}
	}

}

package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldUnload extends Command {
	
	public WorldUnload(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.unload";
	}
	
	public void execute() {
		if (args.length != 0) {
			worldname = WorldManager.matchWorld(args[0]);
			if (this.handleWorld()) {
				World w = Bukkit.getServer().getWorld(worldname);
				if (w != null) {
					notifyConsole("Issued an unload command for world: " + worldname);
					if (WorldManager.unload(w)) {
						message(ChatColor.GREEN + "World '" + worldname + "' has been unloaded!");
					} else {
						message(ChatColor.RED + "Failed to unload the world (main world or online players?)");
					}
				} else {
					message(Localization.getWorldNotLoaded(worldname));
				}
			}
		} else {
			showInv();
		}
	}
	
}

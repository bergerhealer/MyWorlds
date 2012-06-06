package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldInventory extends Command {

	public WorldInventory(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.inventory";
	}
	
	public World world = null;
	
	public boolean prepare() {
		this.genWorldname(1);
		if (this.handleWorld()) {
			this.world = WorldManager.getWorld(worldname);
		}
		return false;
	}
	
	public void merge() {
		
	}
	
	public void split() {
		
	}
	
	public void execute() {
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("merge")) {
				if (prepare()) {
					merge();
				}
			} else if (args[0].equalsIgnoreCase("split")) {
				if (prepare()) {
					split();
				}
			}
		}
		//usage
		message(ChatColor.YELLOW + "/world inventory [split/merge] [worldnames]");
	}

}

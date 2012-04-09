package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.MyWorlds;

public class WorldConfig extends Command {

	public WorldConfig(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.config";
	}
	
	public void execute() {
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("load")) {
				com.bergerkiller.bukkit.mw.WorldConfig.init(MyWorlds.plugin.root() + "worlds.yml");
				message(ChatColor.GREEN + "World configuration has been loaded!");
			} else if (args[0].equalsIgnoreCase("save")) {
				com.bergerkiller.bukkit.mw.WorldConfig.saveAll(MyWorlds.plugin.root() + "worlds.yml");
				message(ChatColor.GREEN + "World configuration has been saved!");
			} else {
				this.showInv();
			}
		} else {
			this.showInv();
		}
	}
	
}

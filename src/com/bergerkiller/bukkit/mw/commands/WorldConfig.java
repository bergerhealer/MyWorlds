package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;

public class WorldConfig extends Command {

	public WorldConfig() {
		super(Permission.COMMAND_CONFIG, "world.config");
	}

	public void execute() {
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("load")) {
				com.bergerkiller.bukkit.mw.WorldConfig.init();
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

package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSetHunger extends Command {

	public WorldSetHunger() {
		super(Permission.COMMAND_HUNGER, "world.hunger");
	}

	public void execute() {
		this.genWorldname(1);
		if (this.handleWorld()) {
			WorldConfig wc = WorldConfig.get(worldname);
			if (args.length == 0) {
				// Display
				if (wc.allowHunger) {
					message(ChatColor.GREEN + "Hunger on World: '" + worldname + "' is enabled");
				} else {
					message(ChatColor.YELLOW + "Hunger on World: '" + worldname + "' is disabled");
				}
			} else {
				// Set
				wc.allowHunger = ParseUtil.parseBool(args[0]);
				wc.updateHunger(wc.getWorld());
				if (wc.allowHunger) {
					message(ChatColor.GREEN + "Hunger on World: '" + worldname + "' enabled!");
				} else {
					message(ChatColor.YELLOW + "Hunger on World: '" + worldname + "' disabled!");
				}
				if (!WorldManager.isLoaded(worldname)) {
					message(ChatColor.YELLOW + "Please note that this world is not loaded!");
				}
			}
		}
	}
}

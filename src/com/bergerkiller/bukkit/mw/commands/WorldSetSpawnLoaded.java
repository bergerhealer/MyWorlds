package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSetSpawnLoaded extends Command {

	public WorldSetSpawnLoaded() {
		super(Permission.COMMAND_TOGGLESPAWNLOADED, "world.spawnloaded");
	}

	public void execute() {
		this.genWorldname(1);
		if (this.handleWorld()) {
			WorldConfig wc = WorldConfig.get(worldname);
			if (args.length == 0) {
				// Display
				if (wc.keepSpawnInMemory) {
					message(ChatColor.GREEN + "The spawn area on World: '" + worldname + "' is kept loaded");
				} else {
					message(ChatColor.YELLOW + "The spawn area on World: '" + worldname + "' is not kept loaded");
				}
			} else {
				// Set
				wc.keepSpawnInMemory = ParseUtil.parseBool(args[0]);
				wc.updateKeepSpawnInMemory(wc.getWorld());
				if (wc.keepSpawnInMemory) {
					message(ChatColor.GREEN + "The spawn area on World: '" + worldname + "' is now kept loaded!");
				} else {
					message(ChatColor.YELLOW + "The spawn area on World: '" + worldname + "' is no longer kept loaded!");
				}
				if (!WorldManager.isLoaded(worldname)) {
					message(ChatColor.YELLOW + "These settings will be used as soon this world is loaded.");
				}
			}
		}
	}
}

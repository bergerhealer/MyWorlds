package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldToggleSpawnLoaded extends Command {

	public WorldToggleSpawnLoaded(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.togglespawnloaded";
	}
	
	public void execute() {
		this.genWorldname(0);
		if (this.handleWorld()) {
			WorldConfig wc = WorldConfig.get(worldname);
			wc.keepSpawnInMemory = !wc.keepSpawnInMemory;
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

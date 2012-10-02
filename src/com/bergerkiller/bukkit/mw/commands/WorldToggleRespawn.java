package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldToggleRespawn extends Command {

	public WorldToggleRespawn(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.togglerespawn";
	}

	public void execute() {
		this.genWorldname(0);
		if (this.handleWorld()) {
			WorldConfig wc = WorldConfig.get(worldname);
			wc.forcedRespawn = !wc.forcedRespawn;
			if (wc.forcedRespawn) {
				message(ChatColor.GREEN + "Forced respawning to world spawn on World: '" + worldname + "' enabled!");
			} else {
				message(ChatColor.YELLOW + "Forced respawning to world spawn on World: '" + worldname + "' disabled!");
			}
		}
	}
}

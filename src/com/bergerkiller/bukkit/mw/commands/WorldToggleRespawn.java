package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldToggleRespawn extends Command {

	public WorldToggleRespawn() {
		super(Permission.COMMAND_TOGGLERESPAWN, "world.togglerespawn");
	}

	public void execute() {
		this.genWorldname(0);
		if (this.handleWorld()) {
			WorldConfig wc = WorldConfig.get(worldname);
			wc.forcedRespawn = !wc.forcedRespawn;
			if (wc.forcedRespawn) {
				message(ChatColor.GREEN + "Forced respawning to world spawn on World: '" + worldname + "' enabled!");
				message(ChatColor.GREEN + "People will return to the world spawn instead of their custom (bed) spawn.");
			} else {
				message(ChatColor.YELLOW + "Forced respawning to world spawn on World: '" + worldname + "' disabled!");
				message(ChatColor.GREEN + "People will return to their custom (bed) spawn instead of the world spawn.");
			}
		}
	}
}

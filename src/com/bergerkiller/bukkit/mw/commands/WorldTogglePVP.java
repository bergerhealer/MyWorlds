package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldTogglePVP extends Command {

	public WorldTogglePVP(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.togglepvp";
	}
	
	public void execute() {
		this.genWorldname(0);
		if (this.handleWorld()) {
			WorldConfig wc = WorldConfig.get(worldname);
			wc.pvp = !wc.pvp;
			wc.updatePVP(wc.getWorld());
			if (wc.pvp) {
				message(ChatColor.GREEN + "PvP on World: '" + worldname + "' enabled!");
			} else {
				message(ChatColor.YELLOW + "PvP on World: '" + worldname + "' disabled!");
			}
			if (!WorldManager.isLoaded(worldname)) {
				message(ChatColor.YELLOW + "Please note that this world is not loaded!");
			}
		}
	}
	
}

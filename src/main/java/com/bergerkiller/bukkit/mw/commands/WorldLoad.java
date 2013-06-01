package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfigStore;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldLoad extends Command {

	public WorldLoad() {
		super(Permission.COMMAND_LOAD, "world.load");
	}

	public void execute() {
		if (args.length != 0) {
			this.worldname = this.removeArg(0);
			final String gen = this.getGeneratorName();
			this.worldname = WorldManager.matchWorld(this.worldname);
			if (this.handleWorld()) {
				if (WorldManager.isLoaded(worldname)) {
					message(ChatColor.YELLOW + "World '" + worldname + "' is already loaded!");
				} else {
					com.bergerkiller.bukkit.mw.WorldConfig config = WorldConfigStore.get(this.worldname);
					String msg = "Loading world: '" + this.worldname + "'...";
					if (gen != null) {
						// Permission handling to change chunk generator
						if (this.player != null && !Permission.COMMAND_LOADSPECIAL.has(this.player)) {
							this.player.sendMessage(ChatColor.RED + "You are not allowed to change world chunk generators!");
							return;
						}
						if (gen.equalsIgnoreCase("none")) {
							if (config.chunkGeneratorName != null) {
								msg = "Cleared old chunk generator set and loading world: '" + this.worldname + "'...";
							}
						} else {
							String fixgen = WorldManager.fixGeneratorName(gen);
							if (fixgen == null) {
								message(ChatColor.RED + "Can not load world: Chunk generator '" + gen + "' does not exist on this server!");
								return;
							} else {
								config.chunkGeneratorName = fixgen;
								message(ChatColor.YELLOW + "Loading world: '" + this.worldname + "' using chunk generator '" + gen + "'...");
							}
						}
					}
					message(ChatColor.YELLOW + msg);
					logAction("Issued a load command for world: " + this.worldname);
					if (WorldManager.createWorld(worldname, 0, sender) != null) {
						message(ChatColor.GREEN + "World loaded!");
					}
				}
			}
		} else {
			showInv();
		}
	}
}

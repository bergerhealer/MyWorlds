package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldInfo extends Command {
	
	public WorldInfo(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.info";
	}
	
	public void execute() {
		this.genWorldname(0);
		if (worldname != null) {
			com.bergerkiller.bukkit.mw.WorldInfo info = WorldManager.getInfo(worldname);
			if (info == null) {
				message(ChatColor.RED + "' " + worldname + "' is broken, no information can be shown!");
			} else {
				WorldConfig wc = WorldConfig.get(worldname);
				message(ChatColor.YELLOW + "Information about the world: " + worldname);
				message(ChatColor.WHITE + "Internal name: " + ChatColor.YELLOW + info.name);
				message(ChatColor.WHITE + "Environment: " + ChatColor.YELLOW + wc.worldmode.toString());
				if (wc.chunkGeneratorName == null) {
					message(ChatColor.WHITE + "Chunk generator: " + ChatColor.YELLOW + "Default");
				} else {
					message(ChatColor.WHITE + "Chunk generator: " + ChatColor.YELLOW + wc.chunkGeneratorName);
				}
				message(ChatColor.WHITE + "Auto-saving: " + ChatColor.YELLOW + wc.autosave);
				message(ChatColor.WHITE + "Keep spawn loaded: " + ChatColor.YELLOW + wc.keepSpawnInMemory);
				message(ChatColor.WHITE + "World seed: " + ChatColor.YELLOW + info.seed);
				if (info.size > 1000000) {
					message(ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size / 1000000) + " Megabytes");
				} else if (info.size > 1000) {
					message(ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size / 1000) + " Kilobytes");
				} else {
					message(ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size) + " Bytes");
				}
				//PvP
				if (wc.pvp) { 
					message(ChatColor.WHITE + "PvP: " + ChatColor.GREEN + "Enabled");
				} else {
					message(ChatColor.WHITE + "PvP: " + ChatColor.YELLOW + "Disabled");
				}
				//Difficulty
				message(ChatColor.WHITE + "Difficulty: " + ChatColor.YELLOW + wc.difficulty.toString().toLowerCase());
				//Game mode
				GameMode mode = wc.gameMode;
				if (mode == null) {
					message(ChatColor.WHITE + "Game mode: " + ChatColor.YELLOW + "Not set");
				} else {
					message(ChatColor.WHITE + "Game mode: " + ChatColor.YELLOW + mode.name().toLowerCase());
				}
				//Time
				String timestr = wc.timeControl.getTime(info.time);
				message(ChatColor.WHITE + "Time: " + ChatColor.YELLOW + timestr);
				//Weather
				if (wc.holdWeather) {
					if (info.raining) {
						if (info.thundering) {
							message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Endless storm with lightning");
						} else {
							message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Endless rain and snow");
						}
					} else {
						message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "No bad weather expected");
					}
				} else {
					if (info.raining) {
						if (info.thundering) {
							message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Stormy with lightning");
						} else {
							message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Rain and snow");
						}
					} else {
						message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "The sky is clear");
					}
				}							
				World w = Bukkit.getServer().getWorld(worldname);
				if (w != null) {
					int playercount = w.getPlayers().size();
					if (playercount > 0) {
						String msg = ChatColor.WHITE + "Status: " + ChatColor.GREEN + "Loaded" + ChatColor.WHITE + " with ";
						msg += playercount + ((playercount <= 1) ? " player" : " players");
						message(msg);
					} else {
						message(ChatColor.WHITE + "Status: " + ChatColor.YELLOW + "Stand-by");
					}
				} else {
					message(ChatColor.WHITE + "Status: " + ChatColor.RED + "Unloaded");
				}
			}
		} else {
			message(ChatColor.RED + "World not found!");
		}
	}
	
}

package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.MWListener;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldWeather extends Command {

	public WorldWeather(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.weather";
	}
	
	public void execute() {
		if (args.length != 0) {
			boolean setStorm = false;
			boolean setSun = false;
			boolean setHold = false;
			boolean useWorld = true;
			boolean setThunder = false;
			for (String command : args) {
				if (command.equalsIgnoreCase("hold")) {
					setHold = true;
				} else if (command.equalsIgnoreCase("always")) {
					setHold = true;
				} else if (command.equalsIgnoreCase("endless")) { 
					setHold = true;
				} else if (command.equalsIgnoreCase("sun")) {
					setSun = true;
				} else if (command.equalsIgnoreCase("sunny")) { 
					setSun = true;
				} else if (command.equalsIgnoreCase("storm")) {
					setStorm = true;
					setThunder = true;
				} else if (command.equalsIgnoreCase("stormy")) {
					setStorm = true;
					setThunder = true;
				} else if (command.equalsIgnoreCase("rain")) {
					setStorm = true;
				} else if (command.equalsIgnoreCase("rainy")) {
					setStorm = true;
				} else if (command.equalsIgnoreCase("snow")) {
					setStorm = true;
				} else if (command.equalsIgnoreCase("snowy")) {
					setStorm = true;
				} else if (command.equalsIgnoreCase("thunder")) {
					setThunder = true;
				} else if (command.equalsIgnoreCase("lightning")) {
					setThunder = true;
				} else if (command.equalsIgnoreCase("heavy")) {
					setThunder = true;
				} else if (command.equalsIgnoreCase("big")) {
					setThunder = true;
				} else if (command.equalsIgnoreCase("huge")) {
					setThunder = true;
				} else {
					continue;
				}
				//Used the last argument as command?
				if (command == args[args.length - 1]) useWorld = false;
			}
			String worldname = WorldManager.getWorldName(sender, args, useWorld);
			if (worldname != null) {
				WorldConfig wc = WorldConfig.get(worldname);
				World w = wc.getWorld();
				if (w != null) {
					boolean holdchange = wc.holdWeather != setHold;
					wc.holdWeather = setHold;
					if (setStorm && ((!w.hasStorm()) || (setThunder && !w.isThundering()) || holdchange)) {
						MWListener.setWeather(w, true);
						if (setThunder) {
							 w.setThundering(true);
						}
						if (setHold) {
							if (setThunder) {
								w.setThunderDuration(Integer.MAX_VALUE);
								message(ChatColor.GREEN + "You started an endless storm on world: '" + worldname + "'!");
							} else {
								message(ChatColor.GREEN + "You started a never ending rainfall and snowfall on world: '" + worldname + "'!");
							}
						} else {
							if (setThunder) {
								message(ChatColor.GREEN + "You started a storm on world: '" + worldname + "'!");
							} else {
								message(ChatColor.GREEN + "You started rainfall and snowfall on world: '" + worldname + "'!");
							}
						}
					} else if (setSun && (w.hasStorm() || holdchange)) {
						MWListener.setWeather(w, false);
						if (setHold) {
							message(ChatColor.GREEN + "You stopped the formation of storms on world: '" + worldname + "'!");
						} else {
							message(ChatColor.GREEN + "You stopped a storm on world: '" + worldname + "'!");
						}
					} else if (setHold) {
						message(ChatColor.GREEN + "Weather changes on world: '" + worldname + "' are now being prevented!");
					} else {
						message(ChatColor.YELLOW + "Unknown syntax or the settings were already applied!");
					}
				} else {
					message(ChatColor.YELLOW + "World: '" + worldname + "' is not loaded, only hold settings are applied!");
				}
			} else {
				message(ChatColor.RED + "World not found!");
			}
		} else {
			showInv();
		}
	}
	
}

package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.TimeControl;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldInfo;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldTime extends Command {

	public WorldTime(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.time";
	}
	
	public void execute() {
		boolean lock = false;
		boolean useWorld = false;
		long time = -1;
		for (String command : args) {
			//Time reading
			if (command.equalsIgnoreCase("lock")) {
				lock = true;
			} else if (command.equalsIgnoreCase("locked")) {
				lock = true;
			} else if (command.equalsIgnoreCase("always")) {
				lock = true;
			} else if (command.equalsIgnoreCase("endless")) {
				lock = true;
			} else if (command.equalsIgnoreCase("l")) {
				lock = true;
			} else if (command.equalsIgnoreCase("-l")) {
				lock = true;	
			} else if (command.equalsIgnoreCase("stop")) {
				lock = true;
			} else if (command.equalsIgnoreCase("freeze")) {
				lock = true;
			} else {
				long newtime = TimeControl.getTime(command);
				if (newtime != -1) {
					time = newtime;
				} else {
					//Used the last argument as command?
					if (command == args[args.length - 1]) useWorld = true;
				}
			}
		}
		worldname = WorldManager.getWorldName(sender, args, useWorld);
		if (this.handleWorld()) {
			if (time == -1) {
				World w = WorldManager.getWorld(worldname);
				if (w == null) {
					WorldInfo i = WorldManager.getInfo(worldname);
					if (i == null) {
						time = 0;
					} else {
						time = i.time;
					}
				} else {
					time = w.getFullTime();
				}
			}
			if (args.length == 0) {
				message(ChatColor.YELLOW + "The current time of world '" + 
						worldname + "' is " + TimeControl.getTimeString(time));
			} else {
				TimeControl tc = WorldConfig.get(worldname).timeControl;
				if (lock) {
					tc.setTime(time);
					if (!WorldManager.isLoaded(worldname)) {
						tc.setLocking(false);
						message(Localization.getWorldNotLoaded(worldname));
						message(ChatColor.YELLOW + "Time will be locked to " + 
								TimeControl.getTimeString(time) + " as soon it is loaded!");
					} else {
						tc.setLocking(true);
						message(ChatColor.GREEN + "Time of world '" + worldname + "' locked to " + 
						        TimeControl.getTimeString(time) + "!");
					}
				} else {
					World w = WorldManager.getWorld(worldname);
					if (w != null) {
						if (tc.isLocked()) {
							tc.setLocking(false);
							WorldManager.setTime(w, time);
							message(ChatColor.GREEN + "Time of world '" + worldname + "' unlocked and set to " + 
							        TimeControl.getTimeString(time) + "!");
						} else {
							WorldManager.setTime(w, time);
							message(ChatColor.GREEN + "Time of world '" + worldname + "' set to " + 
							        TimeControl.getTimeString(time) + "!");
						}
					} else {
						message(Localization.getWorldNotLoaded(worldname));
						message(ChatColor.YELLOW + "Time has not been changed!");
					}
				}
			}
		}
	}
	
}

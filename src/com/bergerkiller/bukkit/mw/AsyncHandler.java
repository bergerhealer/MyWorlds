package com.bergerkiller.bukkit.mw;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class AsyncHandler {
	public static void delete(CommandSender sender, String worldname) {
		args = new Object[2];
		args[0] = worldname;
		args[1] = sender;
		command = "delete";
		run();
	}
	public static void copy(CommandSender sender, String worldname, String newname) {
		args = new Object[3];
		args[0] = worldname;
		args[1] = newname;
		args[2] = sender;
		command = "copy";
		run();
	}
	
	public static String command;
	public static Object[] args;
	
	public static void run() {
		MyWorlds.plugin.getServer().getScheduler().scheduleAsyncDelayedTask(MyWorlds.plugin, new Runnable() {
			public void run() {
				if (command.equals("delete")) {
					CommandSender sender = (CommandSender) args[1];
					if (WorldManager.deleteWorld(args[0].toString())) {
						MyWorlds.message(sender, ChatColor.GREEN + "World '" + args[0].toString() + "' has been removed!");
					} else {
						MyWorlds.message(sender, ChatColor.RED + "Failed to (completely) remove the world!");
					}
				} else if (command.equals("copy")) {
					String oldworld = args[0].toString();
					String newworld = args[1].toString();
					CommandSender sender = (CommandSender) args[2];
					if (WorldManager.copyWorld(oldworld, newworld)) {
						MyWorlds.message(sender, ChatColor.GREEN + "World '" + oldworld + "' has been copied as '" + newworld + "'!");
					} else {
						MyWorlds.message(sender, ChatColor.RED + "Failed to copy world to '" + newworld + "'!");
					}
				}
			}
		});
	}

}

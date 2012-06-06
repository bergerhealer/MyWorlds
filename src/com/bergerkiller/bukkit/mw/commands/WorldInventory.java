package com.bergerkiller.bukkit.mw.commands;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldInventory extends Command {

	public WorldInventory(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.inventory";
	}
	
	public Set<String> worlds = new HashSet<String>();

	public boolean prepareWorlds() {
		this.removeArg(0);
		for (String world : this.args) {
			world = WorldManager.matchWorld(world);
			if (world != null) {
				this.worlds.add(world);
			}
		}
		if (this.worlds.isEmpty()) {
			message(ChatColor.RED + "Failed to find any of the worlds specified!");
			return false;
		} else {
			return true;
		}
	}

	public void execute() {
		if (args.length > 1) {
			if (args[0].equalsIgnoreCase("merge")) {
				if (this.prepareWorlds()) {
					Set<String> invWorlds = new HashSet<String>();
					for (String world : worlds) {
						invWorlds.add(world.toLowerCase());
						invWorlds.addAll(com.bergerkiller.bukkit.mw.WorldConfig.get(world).inventory.getWorlds());
					}
					if (invWorlds.size() <= 1) {
						message(ChatColor.RED + "You need to specify more than one world to merge!");
					} else {
						com.bergerkiller.bukkit.mw.WorldInventory.merge(invWorlds);
						MessageBuilder builder = new MessageBuilder();
						builder.green("Worlds ").setSeparator(ChatColor.WHITE, "/");
						for (String world : invWorlds) {
							builder.yellow(world);
						}
						builder.setSeparator(null).green(" now share the same player inventory!");
						builder.send(this.sender);
					}
				}
				return;
			} else if (args[0].equalsIgnoreCase("split") || args[0].equalsIgnoreCase("detach")) {
				if (this.prepareWorlds()) {
					com.bergerkiller.bukkit.mw.WorldInventory.detach(this.worlds);
					if (this.worlds.size() > 1) {
						MessageBuilder builder = new MessageBuilder();
						builder.green("Worlds ").setSeparator(ChatColor.WHITE, "/");
						for (String world : worlds) {
							builder.yellow(world);
						}
						builder.setSeparator(null).green(" now have their own player inventories!");
						builder.send(this.sender);
					} else {
						for (String world : this.worlds) {
							message(ChatColor.GREEN + "World " + ChatColor.WHITE + world + ChatColor.GREEN + " now has it's own player inventory!");
						}
					}
				}
				return;
			} else {
				message(ChatColor.RED + "Unknown command: \\inventory " + args[0]);
			}
		}
		//usage
		message(ChatColor.YELLOW + "/world inventory [split/merge] [worldnames]");
	}

}

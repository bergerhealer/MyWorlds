package com.bergerkiller.bukkit.mw.commands;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldOpping extends Command {
	
	public WorldOpping(CommandSender sender, String[] args, boolean op) {
		super(sender, args);
		if (op) {
			this.node = "world.op";
		} else {
			this.node = "world.deop";
		}
	}
	
	public void execute() {
		if (args.length >= 2) {
			boolean all = args.length == 2 && (args[1].startsWith("*") || args[1].equalsIgnoreCase("all"));
			this.genWorldname(1);
			if (all) {
				String playername = args[0];
				if (playername.startsWith("*")) {
					for (WorldConfig wc : WorldConfig.all()) {
						wc.OPlist.clear();
						if (node == "world.op") {
							wc.OPlist.add("*");
						}
						wc.updateOP(wc.getWorld());
					}
					if (node == "world.op") {
						message(ChatColor.YELLOW + "Everyone is now an operator on all worlds!");
						if (sender instanceof Player) {
							MyWorlds.plugin.log(Level.INFO, "Player '" + ((Player) sender).getName() + " opped everyone on all worlds!");
						}
					} else {
						message(ChatColor.YELLOW + "Everyone is no longer an operator on any world!");
						if (sender instanceof Player) {
							MyWorlds.plugin.log(Level.INFO, "Player '" + ((Player) sender).getName() + " de-opped everyone on all worlds!");
						}
					}
				} else {
					for (WorldConfig wc : WorldConfig.all()) {
						if (node == "world.op") {
							wc.OPlist.add(playername.toLowerCase());
						} else {
							wc.OPlist.remove(playername.toLowerCase());
						}
						wc.updateOP(wc.getWorld());
					}
					if (node == "world.op") {
						message(ChatColor.WHITE + playername + ChatColor.YELLOW + " is now an operator on all worlds!");
						if (sender instanceof Player) {
							MyWorlds.plugin.log(Level.INFO, "Player '" + ((Player) sender).getName() + " opped '" + playername + "' on all worlds!");
						}
					} else {
						message(ChatColor.WHITE + playername + ChatColor.YELLOW + " is no longer an operator on any world!");
						if (sender instanceof Player) {
							MyWorlds.plugin.log(Level.INFO, "Player '" + ((Player) sender).getName() + " de-opped '" + playername + "' on all worlds!");
						}
					}
				}
			} else if (this.handleWorld()) {
				WorldConfig wc = WorldConfig.get(worldname);
				String playername = args[0];
				if (playername.startsWith("*")) {
					wc.OPlist.clear();
					if (node == "world.op") {
						wc.OPlist.add("*");
						message(ChatColor.YELLOW + "Everyone on world '" + worldname + "' is an operator now!");
						if (sender instanceof Player) {
							MyWorlds.plugin.log(Level.INFO, "Player '" + ((Player) sender).getName() + " opped everyone on world '" + worldname + "'!");
						}
					} else {
						message(ChatColor.YELLOW + "Operators on world '" + worldname + "' have been cleared!");
						if (sender instanceof Player) {
							MyWorlds.plugin.log(Level.INFO, "Player '" + ((Player) sender).getName() + " de-opped everyone on world '" + worldname + "'!");
						}
					}
				} else {
					if (node == "world.op") {
						wc.OPlist.add(playername.toLowerCase());
						message(ChatColor.WHITE + playername + ChatColor.YELLOW + " is now an operator on world '" + worldname + "'!");
						if (sender instanceof Player) {
							MyWorlds.plugin.log(Level.INFO, "Player '" + ((Player) sender).getName() + " opped '" + playername + "' on world '" + worldname + "'!");
						}
					} else {
						wc.OPlist.remove(playername.toLowerCase());
						message(ChatColor.WHITE + playername + ChatColor.YELLOW + " is no longer an operator on world '" + worldname + "'!");
						if (sender instanceof Player) {
							MyWorlds.plugin.log(Level.INFO, "Player '" + ((Player) sender).getName() + " de-opped '" + playername + "' on world '" + worldname + "'!");
						}
					}
				}
				wc.updateOP(wc.getWorld());
			}
		} else {
			//list Operators
			WorldConfig wc = WorldConfig.get(WorldManager.getWorldName(sender, args, false));
			message(ChatColor.YELLOW + "Operators of world '" + wc.worldname + "':");
			if (sender instanceof Player) {
				//perform some nice layout coloring
				String msgpart = "";
				for (String player : wc.OPlist) {
					//prepare it
					if (Bukkit.getServer().getPlayer(player) != null) {
						player = ChatColor.GREEN + player;
					} else {
						player = ChatColor.RED + player;
					}
					//display it
					if (msgpart.length() + player.length() < 70) {
						if (msgpart != "") msgpart += ChatColor.WHITE + " / ";
						msgpart += player;
					} else {
						sender.sendMessage(msgpart);
						msgpart = player;
					}
				}
				//possibly forgot one?
				if (msgpart != "") sender.sendMessage(msgpart);
			} else {
				//plain world per line
				for (String player : wc.OPlist) {
					String status = "[Offline]";
					if (Bukkit.getServer().getPlayer(player) != null) {
						status = "[Online]";
					}
					sender.sendMessage("    " + player + " " + status);
				}
			}
		}
	}
}

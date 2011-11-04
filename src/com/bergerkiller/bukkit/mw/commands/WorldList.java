package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldList extends Command {

	public WorldList(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.list";
	}
	
	public void execute() {
		if (sender instanceof Player) {
			//perform some nice layout coloring
			sender.sendMessage("");
			sender.sendMessage(ChatColor.GREEN + "[Loaded/Online] " + ChatColor.RED + "[Unloaded/Offline] " + ChatColor.DARK_RED + "[Broken/Dead]");
			sender.sendMessage(ChatColor.YELLOW + "Available worlds: ");
			String msgpart = "";
			for (String world : WorldManager.getWorlds()) {
				//prepare it
				if (WorldManager.isLoaded(world)) {
					world = ChatColor.GREEN + world;
				} else if (WorldManager.getData(world) == null) {
					world = ChatColor.DARK_RED + world;
				} else {
					world = ChatColor.RED + world;
				}
				//display it
				if (msgpart.length() + world.length() < 70) {
					if (msgpart != "") msgpart += ChatColor.WHITE + " / ";
					msgpart += world;
				} else {
					sender.sendMessage(msgpart);
					msgpart = world;
				}
			}
			//possibly forgot one?
			if (msgpart != "") sender.sendMessage(msgpart);
		} else {
			//plain world per line
			sender.sendMessage("Available worlds:");
			for (String world : WorldManager.getWorlds()) {
				String status = "[Unloaded]";
				if (WorldManager.isLoaded(world)) {
					status = "[Loaded]";
				} else if (WorldManager.getData(world) == null) {
					status = "[Broken]";
				}
				sender.sendMessage("    " + world + " " + status);
			}
		}
	}

}

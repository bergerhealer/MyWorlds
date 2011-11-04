package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldListGenerators extends Command {

	public WorldListGenerators(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.listgenerators";
	}
	
	public void execute() {
		message(ChatColor.YELLOW + "Available chunk generators:");
		String msgpart = "";
		for (String plugin : WorldManager.getGeneratorPlugins()) {
			plugin = ChatColor.GREEN + plugin;
			//display it
			if (msgpart.length() + plugin.length() < 70) {
				if (msgpart != "") msgpart += ChatColor.WHITE + " / ";
				msgpart += plugin;
			} else {
				sender.sendMessage(msgpart);
				msgpart = plugin;
			}
		}
		message(msgpart);
	}
	
}

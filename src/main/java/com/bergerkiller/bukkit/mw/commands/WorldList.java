package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfigStore;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldList extends Command {

	public WorldList() {
		super(Permission.COMMAND_LIST, "world.list");
	}
	
	public void execute() {
		if (sender instanceof Player) {
			//perform some nice layout coloring
			MessageBuilder builder = new MessageBuilder();
			builder.newLine().green("[Loaded/Online] ").red("[Unloaded/Offline] ").dark_red("[Broken/Dead]");
			builder.newLine().yellow("Available worlds: ");
			builder.setSeparator(ChatColor.WHITE, " / ").setIndent(2).newLine();
			for (String world : WorldUtil.getLoadableWorlds()) {
				if (WorldManager.isLoaded(world)) {
					builder.green(world);
				} else if (WorldConfigStore.get(world).isBroken()) {
					builder.dark_red(world);
				} else {
					builder.red(world);
				}
			}
			builder.send(sender);
		} else {
			//plain world per line
			sender.sendMessage("Available worlds:");
			for (String world : WorldUtil.getLoadableWorlds()) {
				String status = "[Unloaded]";
				if (WorldManager.isLoaded(world)) {
					status = "[Loaded]";
				} else if (WorldConfigStore.get(world).isBroken()) {
					status = "[Broken]";
				}
				sender.sendMessage("    " + world + " " + status);
			}
		}
	}

}

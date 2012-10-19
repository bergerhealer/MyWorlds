package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldListGenerators extends Command {

	public WorldListGenerators() {
		super(Permission.COMMAND_LISTGEN, "world.listgenerators");
	}

	public void execute() {
		MessageBuilder builder = new MessageBuilder();
		builder.yellow("Available chunk generators:").newLine();
		builder.setIndent(2).setSeparator(ChatColor.WHITE, " / ");
		for (String plugin : WorldManager.getGeneratorPlugins()) {
			builder.green(plugin);
		}
		builder.send(sender);
	}
}

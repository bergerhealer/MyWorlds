package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldReloadWE extends Command {
	
	public WorldReloadWE(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.reloadwhenempty";
	}
	
	public void execute() {
		if (args.length != 0) {
			this.genWorldname(1);
			if (this.handleWorld()) {
				WorldConfig wc = WorldConfig.get(worldname);
				if (StringUtil.isBool(args[0])) {
					wc.reloadWhenEmpty = StringUtil.getBool(args[0]);
					if (wc.reloadWhenEmpty) {
						message(ChatColor.YELLOW + "The world '" + worldname + "' will now reload when it has no players.");
					} else {
						message(ChatColor.YELLOW + "The world '" + worldname + "' will no longer reload when it has no players.");
					}
				} else {
					if (wc.reloadWhenEmpty) {
						message(ChatColor.YELLOW + "The world '" + worldname + "' reloads when it has no players.");
					} else {
						message(ChatColor.YELLOW + "The world '" + worldname + "' does not reload when it has no players.");
					}
				}
			}
		} else {
			showInv();
		}
	}
}

package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldSetSaving extends Command {

	public WorldSetSaving(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.setsaving";
	}
	
	public void execute() {
		if (args.length != 0) {
			this.genWorldname(1);
			if (this.handleWorld()) {
				WorldConfig wc = WorldConfig.get(worldname);
				if (StringUtil.isBool(args[0])) {
					boolean set = StringUtil.getBool(args[0]);
					wc.autosave = set;
					wc.updateAutoSave(wc.getWorld());
					if (set) {
						message(ChatColor.YELLOW + "Auto-saving on world '" + worldname + "' is now enabled!");
					} else {
						message(ChatColor.YELLOW + "Auto-saving on world '" + worldname + "' is now disabled!");
					}
				} else {
					if (wc.autosave) {
						message(ChatColor.YELLOW + "Auto-saving on world '" + worldname + "' is enabled!");
					} else {
						message(ChatColor.YELLOW + "Auto-saving on world '" + worldname + "' is disabled!");
					}
				}
			}
		} else {
			showInv();
		}
	}
	
}

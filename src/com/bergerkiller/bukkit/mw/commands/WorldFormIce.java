package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldFormIce extends Command {

	public WorldFormIce() {
		super(Permission.COMMAND_FORMING, "world.formice");
	}

	public void execute() {
		if (args.length != 0) {
			this.genWorldname(1);
			if (this.handleWorld()) {
				WorldConfig wc = WorldConfig.get(worldname);
				if (ParseUtil.isBool(args[0])) {
					wc.formIce = ParseUtil.parseBool(args[0]);
					if (wc.formIce) {
						message(ChatColor.YELLOW + "Ice will now form on world '" + worldname + "'!");
					} else {
						message(ChatColor.YELLOW + "Ice will no longer form on world '" + worldname + "'!");
					}
				} else {
					if (wc.formIce) {
						message(ChatColor.YELLOW + "Ice can form on world '" + worldname + "'!");
					} else {
						message(ChatColor.YELLOW + "Ice can not form on world '" + worldname + "'!");
					}
				}
			}
		} else {
			showInv();
		}
	}
}

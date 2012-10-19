package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldShowRain extends Command {

	public WorldShowRain() {
		super(Permission.COMMAND_SPOUTWEATHER, "world.showrain");
	}

	public void execute() {
		if (args.length != 0) {
			this.genWorldname(1);
			if (this.handleWorld()) {
				WorldConfig wc = WorldConfig.get(worldname);
				if (ParseUtil.isBool(args[0])) {
					wc.showRain = ParseUtil.parseBool(args[0]);
					if (wc.showRain) {
						message(ChatColor.YELLOW + "Rain will be seen by Spoutcraft players on world '" + worldname + "'!");
					} else {
						message(ChatColor.YELLOW + "Rain will not be seen by Spoutcraft players on world '" + worldname + "'!");
					}
				} else {
					if (wc.showRain) {
						message(ChatColor.YELLOW + "Rain is seen by Spoutcraft players on world '" + worldname + "'!");
					} else {
						message(ChatColor.YELLOW + "Rain is not seen by Spoutcraft players on world '" + worldname + "'!");
					}
				}
				this.messageNoSpout();
			}
		} else {
			showInv();
		}
	}
}

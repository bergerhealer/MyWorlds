package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldShowSnow extends Command {

	public WorldShowSnow() {
		super(Permission.COMMAND_SPOUTWEATHER, "world.showsnow");
	}

	public void execute() {
		if (args.length != 0) {
			this.genWorldname(1);
			if (this.handleWorld()) {
				WorldConfig wc = WorldConfig.get(worldname);
				if (ParseUtil.isBool(args[0])) {
					wc.showSnow = ParseUtil.parseBool(args[0]);
					if (wc.showSnow) {
						message(ChatColor.YELLOW + "Snow will be seen by Spoutcraft players on world '" + worldname + "'!");
					} else {
						message(ChatColor.YELLOW + "Snow will not be seen by Spoutcraft players on world '" + worldname + "'!");
					}
				} else {
					if (wc.showSnow) {
						message(ChatColor.YELLOW + "Snow is seen by Spoutcraft players on world '" + worldname + "'!");
					} else {
						message(ChatColor.YELLOW + "Snow is not seen by Spoutcraft players on world '" + worldname + "'!");
					}
				}
				this.messageNoSpout();
			}
		} else {
			showInv();
		}
	}
}

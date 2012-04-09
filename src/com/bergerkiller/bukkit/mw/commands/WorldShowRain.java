package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldShowRain extends Command {

	public WorldShowRain(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.showrain";
	}
	
	public void execute() {
		if (args.length != 0) {
			this.genWorldname(1);
			if (this.handleWorld()) {
				WorldConfig wc = WorldConfig.get(worldname);
				if (StringUtil.isBool(args[0])) {
					wc.showRain = StringUtil.getBool(args[0]);
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

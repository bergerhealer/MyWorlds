package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.utils.EnumUtil;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldGamemode extends Command {

	public WorldGamemode(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.gamemode";
	}
	
	public void execute() {
		this.genWorldname(1);
		if (this.handleWorld()) {
			if (args.length == 0) {
				//display
				GameMode mode = WorldConfig.get(worldname).gameMode;
				String msg = ChatColor.YELLOW + "Current game mode of world '" + worldname + "': ";
				if (mode == null) {
					message(msg + ChatColor.YELLOW + "Not set");
				} else {
					message(msg + ChatColor.YELLOW + mode.name().toLowerCase());
				}
			} else {
				//Parse the gamemode
				GameMode mode = EnumUtil.parseGameMode(args[0], null);
				WorldConfig wc = WorldConfig.get(worldname);
				wc.setGameMode(mode);
				if (mode == null) {
					message(ChatColor.YELLOW + "Game mode of World '" + worldname + "' cleared!");
				} else {
					message(ChatColor.YELLOW + "Game mode of World '" + worldname + "' set to " + mode.name().toLowerCase() + "!");
				}
			}
		}
	}
	
}

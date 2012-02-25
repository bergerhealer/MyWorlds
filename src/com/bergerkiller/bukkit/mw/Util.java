package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Util {

	public static boolean isSolid(Block b, BlockFace direction) {
		int maxwidth = 10;
		while (true) {
			int id = b.getTypeId();
			if (id == 0) return false;
			if (id != 9 && id != 8) return true;
			b = b.getRelative(direction);
			--maxwidth;
			if (maxwidth <= 0) return false;
		}
	}

	public static void notifyConsole(CommandSender sender, String message) {
		if (sender instanceof Player) {
			MyWorlds.plugin.log(Level.INFO, ((Player) sender).getName() + " " + message);
		}
	}
	


}

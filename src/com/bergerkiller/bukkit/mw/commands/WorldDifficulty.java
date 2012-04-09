package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.utils.EnumUtil;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldDifficulty extends Command {
	
	public WorldDifficulty(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.difficulty";
	}
	
	public void execute() {
		this.genWorldname(1);
		if (this.handleWorld()) {
			WorldConfig wc = WorldConfig.get(worldname);
		    if (args.length == 0) {
		    	String diff = wc.difficulty.toString().toLowerCase();
		    	message(ChatColor.YELLOW + "Difficulty of world '" + worldname + "' is set at " + ChatColor.WHITE + diff);
		    } else {
		    	Difficulty diff = EnumUtil.parseDifficulty(args[0], Difficulty.NORMAL);
		    	if (diff != null) {
					wc.difficulty = diff;
					wc.updateDifficulty(wc.getWorld());
					message(ChatColor.YELLOW + "Difficulty of world '" + worldname + "' set to " + ChatColor.WHITE + diff.toString().toLowerCase());
		    	} else {
		    		message(ChatColor.RED + "Difficulty '" + args[0] + "' has not been recognized!");
		    	}
		    }
		}
	}
	
}

package com.bergerkiller.bukkit.mw.commands;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.mw.LoadChunksTask;
import com.bergerkiller.bukkit.mw.MWListener;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldCreate extends Command {
	
	public WorldCreate(CommandSender sender, String[] args) {
		super(sender, args);
		this.node = "world.create";
	}
	
	public void execute() {
		if (args.length != 0) {
			worldname = args[0];
			String gen = null;
			if (worldname.contains(":")) {
				String[] parts = worldname.split(":");
				if (parts.length == 2) {
					worldname = parts[0];
					gen = parts[1];
				} else {
					worldname = parts[0];
					gen = parts[1] + ":" + parts[2];
				}
			}
			if (!WorldManager.worldExists(worldname)) {
				String seed = "";
				for (int i = 1;i < args.length;i++) {
					if (seed != "") seed += " ";
					seed += args[i];
				}
				long seedval = WorldManager.getRandomSeed(seed);
				notifyConsole("Issued a world creation command for world: " + worldname);
		        WorldConfig.remove(worldname);
				if (gen == null) {
					message(ChatColor.YELLOW + "Creating world '" + worldname + "' (this can take a while) ...");
				} else {
					String fixgen = WorldManager.fixGeneratorName(gen);
					if (fixgen == null) {
						message(ChatColor.RED + "Failed to create world because the generator '" + gen + "' is missing!");
					} else {
						WorldManager.setGenerator(worldname, fixgen);
						message(ChatColor.YELLOW + "Creating world '" + worldname + "' using generator '" + fixgen + "' (this can take a while) ...");
					}
				}
		        message(ChatColor.WHITE + "World seed: " + ChatColor.YELLOW + seedval);
		        MWListener.ignoreWorld(worldname);
		        World world = WorldManager.createWorld(worldname, seedval);
				if (world != null) {
					//load chunks
					final int keepdimension = 14;
					final int total = 4 * keepdimension * keepdimension;
					int current = 0;
					int spawnx = world.getSpawnLocation().getBlockX() >> 4;
				    int spawnz = world.getSpawnLocation().getBlockZ() >> 4;
					for (int x = -keepdimension; x < keepdimension; x++) {
						boolean first = true;
						for (int z = -keepdimension; z < keepdimension; z++) {
							int cx = spawnx + x;
							int cz = spawnz + z;
							Task t = null;
							if (first || (current + 2) == total) {
								int per = 100;
								if (first) per = 100 * current / total;
								t = new Task(MyWorlds.plugin, per) {
									public void run() {
										int percent = arg(0, Integer.class);
									    message(ChatColor.YELLOW + "Preparing spawn area (" + percent + "%)...");
									    MyWorlds.plugin.log(Level.INFO, "Preparing spawn area (" + percent + "%)...");
									}
								};
								first = false;
							}
							if (++current == total) {
								t = new Task(MyWorlds.plugin, world) {
									public void run() {
										World world = arg(0, World.class);
										world.setKeepSpawnInMemory(true);
									    message(ChatColor.GREEN + "World '" + world.getName() + "' has been loaded and is ready for use!");
									    MyWorlds.plugin.log(Level.INFO, "World '"+ world.getName() + "' loaded.");
									}
								};
							}
							LoadChunksTask.add(world, cx, cz, t);
						}
					}
				} else {
					message(ChatColor.RED + "World creation failed!");
				}
			} else {
				message(ChatColor.RED + "World already exists!");
			}
		} else {
			showInv();
		}
	}
	
}

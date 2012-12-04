package com.bergerkiller.bukkit.mw.commands;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.LoadChunksTask;
import com.bergerkiller.bukkit.mw.MWListener;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldCreate extends Command {

	public WorldCreate() {
		super(Permission.COMMAND_CREATE, "world.create");
	}

	public void execute() {
		if (args.length != 0) {
			this.worldname = this.removeArg(0);
			String gen = this.getGeneratorName();
			if (!WorldManager.worldExists(worldname)) {
				long seedval = WorldManager.getRandomSeed(StringUtil.combine(" ", this.args));
				logAction("Issued a world creation command for world: " + worldname);
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
		        final World world = WorldManager.createWorld(worldname, seedval);
				if (world != null) {
					//load chunks
					final int keepdimension = 15;
					final int keepedgedim = 2 * keepdimension + 1;
					final int total = keepedgedim * keepedgedim;
					int current = 0;
					int spawnx = world.getSpawnLocation().getBlockX() >> 4;
				    int spawnz = world.getSpawnLocation().getBlockZ() >> 4;
					for (int x = -keepdimension; x <= keepdimension; x++) {
						boolean first = true;
						for (int z = -keepdimension; z <= keepdimension; z++) {
							int cx = spawnx + x;
							int cz = spawnz + z;
							Task t = null;
							if (first || (current + 2) == total) {
								final int percent = first ? (100 * current / total) : 100;
								t = new Task(MyWorlds.plugin) {
									public void run() {
									    message(ChatColor.YELLOW + "Preparing spawn area (" + percent + "%)...");
									    MyWorlds.plugin.log(Level.INFO, "Preparing spawn area (" + percent + "%)...");
									}
								};
								first = false;
							}
							if (++current == total) {
								t = new Task(MyWorlds.plugin) {
									public void run() {
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

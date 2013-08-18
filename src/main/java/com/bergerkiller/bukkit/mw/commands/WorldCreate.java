package com.bergerkiller.bukkit.mw.commands;

import java.util.Locale;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.mw.LoadChunksTask;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.WorldMode;

public class WorldCreate extends Command {

	public WorldCreate() {
		super(Permission.COMMAND_CREATE, "world.create");
	}

	public void execute() {
		if (args.length != 0) {
			this.worldname = this.removeArg(0);
			String gen = this.getGeneratorName();
			this.genForcedWorldMode();
			if (!WorldManager.worldExists(worldname)) {
				long seedval = WorldManager.getRandomSeed(StringUtil.combine(" ", this.args));
				logAction("Issued a world creation command for world: " + worldname);
		        WorldConfig.remove(worldname);
		        WorldConfig wc = WorldConfig.get(worldname, this.forcedWorldMode);
				if (gen == null) {
					message(ChatColor.YELLOW + "Creating world '" + worldname + "' (this can take a while) ...");
					message(ChatColor.WHITE + "World generator: " + ChatColor.YELLOW + "Default (Vanilla)");
				} else {
					String cgenName = WorldManager.fixGeneratorName(gen);
					if (cgenName == null || cgenName.length() <= 1) {
						message(ChatColor.RED + "Failed to create world because the generator '" + gen + "' is missing!");
						return;
					}
					message(ChatColor.YELLOW + "Creating world '" + worldname + "' (this can take a while) ...");
					if (cgenName.indexOf(':') == 0) {
						String args = cgenName.substring(1);
						// Write a level.dat with the options changed
						CommonTagCompound data = WorldManager.createData(this.worldname, seedval);
						data.putValue("generatorName", wc.worldmode.getTypeName());
						data.putValue("generatorVersion", 0);
						data.putValue("generatorOptions", args);
						WorldManager.setData(this.worldname, data);
						message(ChatColor.WHITE + "World options: " + ChatColor.YELLOW + args);
						message(ChatColor.WHITE + "World generator: " + ChatColor.YELLOW + "Default (Vanilla)");
					} else {
						wc.setChunkGeneratorName(cgenName);
						message(ChatColor.WHITE + "World generator: " + ChatColor.YELLOW + cgenName);
					}
				}
		        message(ChatColor.WHITE + "World seed: " + ChatColor.YELLOW + seedval);
		        message(ChatColor.WHITE + "World environment: " + ChatColor.YELLOW + wc.worldmode.getName());
		        MyWorlds.plugin.initDisableSpawn(worldname);
		        final World world = WorldManager.createWorld(worldname, seedval, sender);
				if (world != null) {
					//load chunks
					final int keepdimension = 15;
					final int keepedgedim = 2 * keepdimension + 1;
					final int total = keepedgedim * keepedgedim;
					int current = 0;
					int spawnx = world.getSpawnLocation().getBlockX() >> 4;
				    int spawnz = world.getSpawnLocation().getBlockZ() >> 4;
					for (int x = -keepdimension; x <= keepdimension; x++) {
						for (int z = -keepdimension; z <= keepdimension; z++) {
							// Spam messages every 64 chunks
							boolean first = (current & 63) == 0x0;
							int cx = spawnx + x;
							int cz = spawnz + z;
							Runnable t = null;
							if (first || (current + 2) == total) {
								final int percent = first ? (100 * current / total) : 100;
								t = new Runnable() {
									public void run() {
									    message(ChatColor.YELLOW + "Preparing spawn area (" + percent + "%)...");
									    MyWorlds.plugin.log(Level.INFO, "Preparing spawn area (" + percent + "%)...");
									}
								};
								first = false;
							}
							if (++current == total) {
								t = new Runnable() {
									public void run() {
										// Set to True, any mistakes in loading chunks will be corrected here
										world.setKeepSpawnInMemory(true);
										// Call onLoad (it was ignored while initing to avoid chunk loads when finding spawn)
										WorldConfig.get(world).onWorldLoad(world);
										// Confirmation message
									    message(ChatColor.GREEN + "World '" + world.getName() + "' has been loaded and is ready for use!");
									    MyWorlds.plugin.log(Level.INFO, "World '"+ world.getName() + "' loaded.");
									}
								};
							}
							LoadChunksTask.add(world, cx, cz, t);
						}
					}
				}
			} else {
				message(ChatColor.RED + "World already exists!");
			}
		} else {
			showInv();
			MessageBuilder modes = new MessageBuilder();
			modes.yellow("Available environments: ").setIndent(2).setSeparator(ChatColor.YELLOW, " / ");
			for (WorldMode mode : WorldMode.values()) {
				modes.green(mode.getName());
			}
			modes.send(sender);
		}
	}
	
}

package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class AsyncHandler {
	public static void delete(CommandSender sender, String worldname) {
		args = new Object[2];
		args[0] = worldname;
		args[1] = sender;
		command = "delete";
		run();
	}
	public static void copy(CommandSender sender, String worldname, String newname) {
		args = new Object[3];
		args[0] = worldname;
		args[1] = newname;
		args[2] = sender;
		command = "copy";
		run();
	}
	public static void repair(CommandSender sender, String worldname, long seed) {
		args = new Object[3];
		args[0] = worldname;
		args[1] = seed;
		args[2] = sender;
		command = "repair";
		run();
	}
	
	public static String command;
	public static Object[] args;
	
	public static void run() {
		MyWorlds.plugin.getServer().getScheduler().scheduleAsyncDelayedTask(MyWorlds.plugin, new Runnable() {
			public void run() {
				if (command.equals("delete")) {
					CommandSender sender = (CommandSender) args[1];
					if (WorldManager.deleteWorld(args[0].toString())) {
						MyWorlds.message(sender, ChatColor.GREEN + "World '" + args[0].toString() + "' has been removed!");
					} else {
						MyWorlds.message(sender, ChatColor.RED + "Failed to (completely) remove the world!");
					}
				} else if (command.equals("copy")) {
					String oldworld = args[0].toString();
					String newworld = args[1].toString();
					CommandSender sender = (CommandSender) args[2];
					if (WorldManager.copyWorld(oldworld, newworld)) {
						MyWorlds.message(sender, ChatColor.GREEN + "World '" + oldworld + "' has been copied as '" + newworld + "'!");
					} else {
						MyWorlds.message(sender, ChatColor.RED + "Failed to copy world to '" + newworld + "'!");
					}
				} else if (command.equals("repair")) {
					String worldname = args[0].toString();
					long seed = (Long) args[1];
					CommandSender sender = (CommandSender) args[2];
					
					boolean worked = false;
					if (WorldManager.isBroken(worldname)) {
						worked = WorldManager.generateData(worldname, seed);
						MyWorlds.message(sender, ChatColor.YELLOW + "Level.dat regenerated using seed: " + seed);
					} else {
						worked = true;
					}
					//Fix chunks
					if (worked) {
						int fixedfilecount = 0;
						int totalfixes = 0;
						int totalremoves = 0;
						int totaldelfailures = 0;
						int totalaccessfailures = 0;
						try {
							File regionfolder = new File(WorldManager.getDataFolder(worldname) + File.separator + "region");
							if (regionfolder.exists()) {
								//Generate backup folder
								Calendar cal = Calendar.getInstance();
								SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss");
								File backupfolder = new File(regionfolder + File.separator + "backup_" + sdf.format(cal.getTime()));
								String[] regionfiles = regionfolder.list();
								int i = 1;
								for (String listedFile : regionfiles) {
									MyWorlds.message(sender, ChatColor.YELLOW + "Scanning and repairing file " + i + "/" + regionfiles.length);
									if (listedFile.toLowerCase().endsWith(".mcr")) {
										int fixcount = WorldManager.repairRegion(new File(regionfolder + File.separator + listedFile), backupfolder);
										if (fixcount == -1) {
											totalremoves++;
											fixedfilecount++;
										} else if (fixcount == -2) {
											totalaccessfailures++;
										} else if (fixcount == -3) {
											totaldelfailures++;
										} else if (fixcount > 0) {
											totalfixes += fixcount;
											fixedfilecount++;
										}
									}
									i++;
								}
								MyWorlds.message(sender, ChatColor.YELLOW + "Fixed " + totalfixes + " chunk(s) and removed " + totalremoves + " file(s)!");
								MyWorlds.message(sender, ChatColor.YELLOW.toString() + fixedfilecount + " File(s) are affected!");
								if (totalaccessfailures > 0) {
									MyWorlds.message(sender, ChatColor.YELLOW.toString() + totalaccessfailures + " File(s) were inaccessible (OK-status unknown).");
								}
								if (totaldelfailures > 0) {
									MyWorlds.message(sender, ChatColor.YELLOW.toString() + totaldelfailures + " Unrecoverable file(s) could not be removed.");
								}
							} else {
								MyWorlds.log(Level.INFO, "Region folder not found, no regions edited.");
							}
						} catch (Exception e) {
							//We did nothing...
							e.printStackTrace();
						}
					}
					if (worked) {
						MyWorlds.message(sender, ChatColor.GREEN + "World: '" + worldname + "' has been repaired!");
					} else {
						MyWorlds.message(sender, ChatColor.RED + "Failed to repair world '" + worldname + "'!");
					}
				}
			}
		});
	}

}

package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

public class AsyncHandler {
	public static void delete(final CommandSender sender, final String worldname) {
		new AsyncTask("World deletion thread") {
			public void run() {
				if (WorldManager.deleteWorld(worldname)) {
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "World '" + worldname + "' has been removed!");
				} else {
					CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to (completely) remove the world!");
				}
			}
		}.start();
	}
	public static void copy(final CommandSender sender, final String oldworld, final String newworld) {
		new AsyncTask("World copy thread") {
			public void run() {
				if (WorldManager.copyWorld(oldworld, newworld)) {
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "World '" + oldworld + "' has been copied as '" + newworld + "'!");
				} else {
					CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to copy world to '" + newworld + "'!");
				}
			}
		}.start();
	}
	public static void repair(final CommandSender sender, final String worldname, final long seed) {
		new AsyncTask("World repair thread") {
			public void run() {
				boolean worked = false;
				if (WorldManager.isBroken(worldname)) {
					worked = WorldManager.generateData(worldname, seed);
					CommonUtil.sendMessage(sender, ChatColor.YELLOW + "Level.dat regenerated using seed: " + seed);
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
								CommonUtil.sendMessage(sender, ChatColor.YELLOW + "Scanning and repairing file " + i + "/" + regionfiles.length);
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
							CommonUtil.sendMessage(sender, ChatColor.YELLOW + "Fixed " + totalfixes + " chunk(s) and removed " + totalremoves + " file(s)!");
							CommonUtil.sendMessage(sender, ChatColor.YELLOW.toString() + fixedfilecount + " File(s) are affected!");
							if (totalaccessfailures > 0) {
								CommonUtil.sendMessage(sender, ChatColor.YELLOW.toString() + totalaccessfailures + " File(s) were inaccessible (OK-status unknown).");
							}
							if (totaldelfailures > 0) {
								CommonUtil.sendMessage(sender, ChatColor.YELLOW.toString() + totaldelfailures + " Unrecoverable file(s) could not be removed.");
							}
						} else {
							MyWorlds.plugin.log(Level.INFO, "Region folder not found, no regions edited.");
						}
					} catch (Exception e) {
						//We did nothing...
						e.printStackTrace();
					}
				}
				if (worked) {
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "World: '" + worldname + "' has been repaired!");
				} else {
					CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to repair world '" + worldname + "'!");
				}
			}
		}.start();	
	}


}

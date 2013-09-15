package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

public class AsyncHandler {
	public static void delete(final CommandSender sender, String worldname) {
		final WorldConfig worldConfig = WorldConfig.get(worldname);
		if (worldConfig.isLoaded()) {
			CommonUtil.sendMessage(sender, ChatColor.RED + "Can not delete a loaded world!");
			return;
		}
		WorldConfig.remove(worldConfig.worldname);
		new AsyncTask("World deletion thread") {
			public void run() {
				if (worldConfig.deleteWorld()) {
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "World '" + worldConfig.worldname + "' has been removed!");
				} else {
					CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to (completely) remove the world!");
				}
			}
		}.start();
	}
	public static void copy(final CommandSender sender, final String oldworld, final String newworld) {
		final WorldConfig oldconfig = WorldConfig.get(oldworld);
		final WorldConfig newconfig = WorldConfig.get(newworld);
		new AsyncTask("World copy thread") {
			public void run() {
				if (oldconfig.copyTo(newconfig)) {
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "World '" + oldworld + "' has been copied as '" + newworld + "'!");
				} else {
					CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to copy world to '" + newworld + "'!");
				}
			}
		}.start();
	}
	public static void repair(final CommandSender sender, final String worldname, final long seed) {
		final WorldConfig config = WorldConfig.get(worldname);
		if (config.isLoaded()) {
			CommonUtil.sendMessage(sender, ChatColor.RED + "Can not repair a loaded world!");
			return;
		}
		new AsyncTask("World repair thread") {
			public void run() {
				boolean hasMadeFixes = false;
				if (config.isBroken()) {
					if (config.resetData(seed)) {
						hasMadeFixes = true;
						CommonUtil.sendMessage(sender, ChatColor.YELLOW + "Level.dat regenerated using seed: " + seed);
					} else {
						CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to repair world '" + worldname + "': could not fix level.dat!");
						return;
					}
				}
				//Fix chunks
				int fixedfilecount = 0;
				int totalfixes = 0;
				int totalremoves = 0;
				int totaldelfailures = 0;
				int totalaccessfailures = 0;
				try {
					File regionfolder = config.getRegionFolder();
					if (regionfolder != null) {
						//Generate backup folder
						Calendar cal = Calendar.getInstance();
						SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss");
						File backupfolder = new File(regionfolder + File.separator + "backup_" + sdf.format(cal.getTime()));
						String[] regionfiles = regionfolder.list();
						int i = 1;
						for (String listedFile : regionfiles) {
							if (listedFile.toLowerCase().endsWith(".mca")) {
								CommonUtil.sendMessage(sender, ChatColor.YELLOW + "Scanning and repairing file " + i + "/" + regionfiles.length);
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
						if (totalfixes > 0 || totalremoves > 0 || fixedfilecount > 0) {
							CommonUtil.sendMessage(sender, ChatColor.YELLOW + "Fixed " + totalfixes + " chunk(s) and removed " + totalremoves + " file(s)!");
							CommonUtil.sendMessage(sender, ChatColor.YELLOW.toString() + fixedfilecount + " File(s) are affected!");
							if (fixedfilecount > 0) {
								CommonUtil.sendMessage(sender, ChatColor.YELLOW + "A backup of these files can be found in '" + backupfolder + "'");
							}
							hasMadeFixes = true;
						} else {
							CommonUtil.sendMessage(sender, ChatColor.YELLOW + "No chunk or region file errors have been detected");
						}
						if (totalaccessfailures > 0) {
							CommonUtil.sendMessage(sender, ChatColor.YELLOW.toString() + totalaccessfailures + " File(s) were inaccessible (OK-status unknown).");
						}
						if (totaldelfailures > 0) {
							CommonUtil.sendMessage(sender, ChatColor.YELLOW.toString() + totaldelfailures + " Unrecoverable file(s) could not be removed.");
						}
					} else {
						CommonUtil.sendMessage(sender, ChatColor.RED + "Region folder not found, no regions edited.");
					}
				} catch (Exception e) {
					//We did nothing...
					e.printStackTrace();
				}
				if (hasMadeFixes) {
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "World: '" + worldname + "' has been repaired!");
				} else {
					CommonUtil.sendMessage(sender, ChatColor.GREEN + "World: '" + worldname + "' contained no errors, no fixes have been performed!");
				}
			}
		}.start();	
	}


}

package com.bergerkiller.bukkit.mw;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.utils.MaterialUtil;

public class Util {
	private static final int STATW_ID = Material.STATIONARY_WATER.getId();

	public static boolean isSolid(Block b, BlockFace direction) {
		int maxwidth = 10;
		while (maxwidth-- >= 0) {
			int id = b.getTypeId();
			if (MaterialUtil.isType(id, Material.WATER, Material.STATIONARY_WATER)) {
				b = b.getRelative(direction);
			} else {
				return id != 0;
			}
		}
		return false;
	}

	private static boolean isObsidianPortal(Block main, BlockFace direction) {
		for (int counter = 0; counter < 20; counter++) {
			Material type = main.getType();
			if (type == Material.PORTAL) {
				main = main.getRelative(direction);
			} else if (type == Material.OBSIDIAN) {
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * Checks if a given block is part of a valid water portal, plugin settings are applied
	 * 
	 * @param main portal block
	 * @return True if it is a water Portal, False if not
	 */
	public static boolean isWaterPortal(Block main) {
		if (!MyWorlds.useWaterTeleport || main.getTypeId() != STATW_ID) {
			return false;
		}
		if (main.getRelative(BlockFace.UP).getTypeId() == STATW_ID || main.getRelative(BlockFace.DOWN).getTypeId() == STATW_ID) {
			boolean allow = false;
			if (main.getRelative(BlockFace.NORTH).getType() == Material.AIR || main.getRelative(BlockFace.SOUTH).getType() == Material.AIR) {
				if (Util.isSolid(main, BlockFace.WEST) && Util.isSolid(main, BlockFace.EAST)) {
					allow = true;
				}
			} else if (main.getRelative(BlockFace.EAST).getType() == Material.AIR || main.getRelative(BlockFace.WEST).getType() == Material.AIR) {
				if (Util.isSolid(main, BlockFace.NORTH) && Util.isSolid(main, BlockFace.SOUTH)) {
					allow = true;
				}
			}
			return allow;
		}
		return false;
	}

	/**
	 * Checks if a given block is part of a valid ender portal, plugin settings are applied
	 * 
	 * @param main portal block
	 * @return True if it is an end Portal, False if not
	 */
	public static boolean isEndPortal(Block main) {
		return main.getType() == Material.ENDER_PORTAL;
	}

	/**
	 * Checks if a given block is part of a valid nether portal, plugin settings are applied
	 * 
	 * @param main portal block
	 * @param overrideMainType - True to override the main block type checking
	 * @return True if it is a nether Portal, False if not
	 */
	public static boolean isNetherPortal(Block main) {
		if (!MyWorlds.onlyObsidianPortals) {
			// Simple check
			return main.getType() == Material.PORTAL;
		}
		// Obsidian portal check
		if (main.getType() != Material.PORTAL) {
			return false;
		}
		if (isObsidianPortal(main, BlockFace.UP) && isObsidianPortal(main, BlockFace.DOWN)) {
			if (isObsidianPortal(main, BlockFace.NORTH) && isObsidianPortal(main, BlockFace.SOUTH)) {
				return true;
			}
			if (isObsidianPortal(main, BlockFace.EAST) && isObsidianPortal(main, BlockFace.WEST)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds the spawn offset to a given Location
	 * 
	 * @param location to add to, can be null
	 * @return Location with the spawn offset
	 */
	public static Location spawnOffset(Location location) {
		if (location == null) {
			return null;
		}
		return location.clone().add(0.5, 2, 0.5);
	}

	/**
	 * Gets the Location from a Position
	 * 
	 * @param position to convert
	 * @return the Location, or null on failure
	 */
	public static Location getLocation(Position position) {
		if (position != null) {
			Location loc = position.toLocation();
	    	if (loc.getWorld() != null) {
	    		return loc;
	    	}
		}
		return null;
	}

	/**
	 * Removes all unallowed characters from a portal name.
	 * These are characters that would cause internal loading/saving issues otherwise.
	 * 
	 * @param name to filter
	 * @return filtered name
	 */
	public static String filterPortalName(String name) {
		if (name == null) {
			return null;
		} else {
			return name.replace("\"", "").replace("'", "");
		}
	}

	/**
	 * Gets the amount of bytes of data stored on disk by a specific file or folder
	 * 
	 * @param file to get the size of
	 * @return File/folder size in bytes
	 */
	public static long getFileSize(File file) {
		if (!file.exists()) {
			return 0L;
		} else if (file.isDirectory()) {
			long size = 0;
			for (File subfile : file.listFiles()) {
				size += getFileSize(subfile);
			}
			return size;
		} else {
			return file.length();
		}
	}
}

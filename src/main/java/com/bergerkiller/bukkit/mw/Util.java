package com.bergerkiller.bukkit.mw;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class Util {
	private static final Material STATW_TYPE = Material.STATIONARY_WATER;

	public static boolean isSolid(Block b, BlockFace direction) {
		int maxwidth = 10;
		while (maxwidth-- >= 0) {
			Material type = b.getType();
			if (MaterialUtil.isType(type, Material.WATER, Material.STATIONARY_WATER)) {
				b = b.getRelative(direction);
			} else {
				return type != Material.AIR;
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
	 * Attempts to find the type of Portal that is near a specific Block
	 * 
	 * @param world to look in
	 * @param x - coordinate to look nearby
	 * @param y - coordinate to look nearby
	 * @param z - coordinate to look nearby
	 * @return Portal material, or NULL if no Portal is found
	 */
	public static Material findPortalMaterial(World world, int x, int y, int z) {
		// Check self
		Material mat = findPortalMaterialSingle(world, x, y, z);
		if (mat == null) {
			// Check in a 3x3x3 cube area
			int dx, dy, dz;
			for (dx = -1; dx <= 1; dx++) {
				for (dy = -1; dy <= 1; dy++) {
					for (dz = -1; dz <= 1; dz++) {
						mat = findPortalMaterialSingle(world, x + dx, y + dy, z + dz);
						if (mat != null) {
							return mat;
						}
					}
				}
			}
		}
		return mat;
	}

	private static Material findPortalMaterialSingle(World world, int x, int y, int z) {
		Material type = WorldUtil.getBlockType(world, x, y, z);
		if (type == STATW_TYPE) {
			if (isWaterPortal(world.getBlockAt(x, y, z))) {
				return Material.STATIONARY_WATER;
			}
		} else if (type == Material.PORTAL) {
			if (isNetherPortal(world.getBlockAt(x, y, z))) {
				return Material.PORTAL;
			}
		} else if (type == Material.ENDER_PORTAL) {
			if (isEndPortal(world.getBlockAt(x, y, z))) {
				return Material.ENDER_PORTAL;
			}
		}
		return null;
	}

	/**
	 * Checks if a given block is part of a valid water portal, plugin settings are applied
	 * 
	 * @param main portal block
	 * @return True if it is a water Portal, False if not
	 */
	public static boolean isWaterPortal(Block main) {
		if (!MyWorlds.useWaterTeleport || main.getType() != STATW_TYPE) {
			return false;
		}
		if (main.getRelative(BlockFace.UP).getType() == STATW_TYPE || main.getRelative(BlockFace.DOWN).getType() == STATW_TYPE) {
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

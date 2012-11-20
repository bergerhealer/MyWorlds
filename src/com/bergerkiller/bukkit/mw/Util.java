package com.bergerkiller.bukkit.mw;

import java.util.logging.Level;

import net.minecraft.server.NBTTagDouble;
import net.minecraft.server.NBTTagFloat;
import net.minecraft.server.NBTTagList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;

public class Util {
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

	public static ConfigurationNode cloneNode(ConfigurationNode node) {
		ConfigurationNode cloned = new ConfigurationNode();
		cloned.setHeader(node.getHeader());
		for (String key : node.getKeys()) {
			if (node.isNode(key)) {
				cloned.set(key, cloneNode(node.getNode(key)));
			} else {
				cloned.set(key, node.get(key));
			}
		}
		return cloned;
	}

	public static void notifyConsole(CommandSender sender, String message) {
		if (sender instanceof Player) {
			MyWorlds.plugin.log(Level.INFO, ((Player) sender).getName() + " " + message);
		}
	}

	/**
	 * Obtains a double list from an array of double values
	 * 
	 * @param adouble values
	 * @return List with the values
	 */
	public static NBTTagList doubleArrayToList(double... adouble) {
		NBTTagList nbttaglist = new NBTTagList();
		double[] adouble1 = adouble;
		int i = adouble.length;
		for (int j = 0; j < i; ++j) {
			double d0 = adouble1[j];
			nbttaglist.add(new NBTTagDouble((String) null, d0));
		}
		return nbttaglist;
	}

	/**
	 * Obtains a float list from an array of float values
	 * 
	 * @param afloat values
	 * @return List with the values
	 */
	public static NBTTagList floatArrayToList(float... afloat) {
		NBTTagList nbttaglist = new NBTTagList();
		float[] afloat1 = afloat;
		int i = afloat.length;

		for (int j = 0; j < i; ++j) {
			float f = afloat1[j];

			nbttaglist.add(new NBTTagFloat((String) null, f));
		}

		return nbttaglist;
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
	 * Checks if a given block is of a valid ender portal, plugin settings are applied
	 * 
	 * @param main portal block
	 * @return True if it is an end Portal, False if not
	 */
	public static boolean isEndPortal(Block main, boolean overrideMainType) {
		return overrideMainType || main.getType() == Material.ENDER_PORTAL;
	}

	/**
	 * Checks if a given block is of a valid nether portal, plugin settings are applied
	 * 
	 * @param main portal block
	 * @param overrideMainType - True to override the main block type checking
	 * @return True if it is a nether Portal, False if not
	 */
	public static boolean isNetherPortal(Block main, boolean overrideMainType) {
		if (!MyWorlds.onlyObsidianPortals) {
			// Simple check
			return overrideMainType || main.getType() == Material.PORTAL;
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
	 * Checks whether a given spawn point is the default 0/128/0 spawn point
	 * 
	 * @param loc to check
	 * @return True if it is the default spawn point, False if not
	 */
	public static boolean isDefaultWorldSpawn(Location loc) {
		if (loc == null) {
			return true;
		}
		if (loc.getX() == 0.0 && loc.getZ() == 0.0 && loc.getYaw() == 0.0f && loc.getPitch() == 0.0f) {
			return loc.getY() == 0.0 || loc.getY() == 50.0 || loc.getY() == 64.0 || loc.getY() == 128.0;
		}
		return false;
	}
}

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

public class Util {
	public static boolean isSolid(Block b, BlockFace direction) {
		int maxwidth = 10;
		while (true) {
			int id = b.getTypeId();
			if (id == 0) return false;
			if (id != 9 && id != 8) return true;
			b = b.getRelative(direction);
			--maxwidth;
			if (maxwidth <= 0) return false;
		}
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
	 * Checks if a given portal block is surrounded by obsidian
	 * 
	 * @param main portal block
	 * @return True if it is an Obsidian Portal, False if not
	 */
	public static boolean isObsidianPortal(Block main) {
		if (isObsidianPortal(main, BlockFace.UP) && isObsidianPortal(main, BlockFace.DOWN)) {
			if (isObsidianPortal(main, BlockFace.NORTH) && isObsidianPortal(main, BlockFace.SOUTH)) return true;
			if (isObsidianPortal(main, BlockFace.EAST) && isObsidianPortal(main, BlockFace.WEST)) return true;
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
}

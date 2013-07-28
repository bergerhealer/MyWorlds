package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	/*
	 * The below methods can also be found in StreamUtil in BKCommonLib
	 * In case 1.5.2 compatibility breaks in this plugin, please use the StreamUtil methods instead.
	 */

	/**
	 * Creates a new FileOutputStream to a file.
	 * If the file does not yet exist a new file is created.
	 * The contents of existing files will be overwritten.
	 * 
	 * @param file to open
	 * @return a new FileOutputStream for the (created) file
	 * @throws IOException in case opening the file failed
	 * @throws SecurityException in case the Security Manager (if assigned) denies access
	 */
	public static FileOutputStream createOutputStream(File file) throws IOException, SecurityException {
		return createOutputStream(file, false);
	}

	/**
	 * Creates a new FileOutputStream to a file.
	 * If the file does not yet exist a new file is created.
	 * 
	 * @param file to open
	 * @param append whether to append to existing files or not
	 * @return a new FileOutputStream for the (created) file
	 * @throws IOException in case opening the file failed
	 * @throws SecurityException in case the Security Manager (if assigned) denies access
	 */
	public static FileOutputStream createOutputStream(File file, boolean append) throws IOException, SecurityException {
		File directory = file.getAbsoluteFile().getParentFile();
		if (!directory.exists()) {
			directory.mkdirs();
		}
		if (!file.exists()) {
			file.createNewFile();
		}
		return new FileOutputStream(file, append);
	}

	/**
	 * Tries to copy a file or directory from one place to the other.
	 * If copying fails along the way the error is printed and false is returned.
	 * Note that when copying directories it may result in an incomplete copy.
	 * 
	 * @param sourceLocation
	 * @param targetLocation
	 * @return True if copying succeeded, False if not
	 */
	public static boolean tryCopyFile(File sourceLocation, File targetLocation) {
		try {
			copyFile(sourceLocation, targetLocation);
			return true;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * Copies a file or directory from one place to the other.
	 * If copying fails along the way an IOException is thrown.
	 * Note that when copying directories it may result in an incomplete copy.
	 * 
	 * @param sourceLocation
	 * @param targetLocation
	 * @throws IOException
	 */
	public static void copyFile(File sourceLocation, File targetLocation) throws IOException {
		if (sourceLocation.isDirectory()) {
			if (!targetLocation.exists()) {
				targetLocation.mkdirs();
			}
			for (String subFileName : sourceLocation.list()) {
				copyFile(new File(sourceLocation, subFileName), new File(targetLocation, subFileName));
			}
		} else {
			// Start a new stream
			FileInputStream input = null;
			FileOutputStream output = null;
			FileChannel inputChannel = null;
			FileChannel outputChannel = null;
			try {
				// Initialize file streams
				input = new FileInputStream(sourceLocation);
				inputChannel = input.getChannel();
				output = createOutputStream(targetLocation);
				outputChannel = output.getChannel();
				// Start transferring
				long transfered = 0;
				long bytes = inputChannel.size();
				while (transfered < bytes) {
					transfered += outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
					outputChannel.position(transfered);
				}
			} finally {
				// Close input stream
				if (inputChannel != null) {
					inputChannel.close();
				} else if (input != null) {
					input.close();
				}
				// Close output stream
				if (outputChannel != null) {
					outputChannel.close();
				} else if (output != null) {
					output.close();
				}
			}
		}
	}

	/**
	 * Deletes a file or directory.
	 * This method will attempt to delete all files in a directory
	 * if a directory is specified.
	 * All files it could not delete will be returned.
	 * If the returned list is empty then deletion was successful.
	 * The returned list is unmodifiable.
	 * 
	 * @param file to delete
	 * @return a list of files it could not delete
	 */
	public static List<File> deleteFile(File file) {
		if (file.isDirectory()) {
			List<File> failFiles = new ArrayList<File>();
			deleteFileList(file, failFiles);
			return Collections.unmodifiableList(failFiles);
		} else if (file.delete()) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableList(Arrays.asList(file));
		}
	}

	private static boolean deleteFileList(File file, List<File> failFiles) {
		if (file.isDirectory()) {
			boolean success = true;
			for (File subFile : file.listFiles()) {
				success &= deleteFileList(subFile, failFiles);
			}
			if (success) {
				file.delete();
			}
			return success;
		} else if (file.delete()) {
			return true;
		} else {
			failFiles.add(file);
			return false;
		}
	}
}

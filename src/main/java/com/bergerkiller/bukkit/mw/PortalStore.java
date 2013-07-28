package com.bergerkiller.bukkit.mw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.StringUtil;

public class PortalStore {
	private static HashMap<String, HashMap<String, Position>> portallocations;

	/**
	 * Gets all the portal locations on a world
	 * 
	 * @param worldname to get the portal locations for
	 * @return mapping of portal names vs. positions of these portals
	 */
	public static HashMap<String, Position> getPortalLocations(String worldname) {
		worldname = worldname.toLowerCase();
		HashMap<String, Position> rval = portallocations.get(worldname);
		if (rval == null) {
			rval = new HashMap<String, Position>();
			portallocations.put(worldname, rval);
		}
		return rval;
	}

	/**
	 * Checks whether a specific Portal has a name set, or whether the default name was used.
	 * 
	 * @param portalname to check
	 * @param position of the portal
	 * @return True if the portal can be teleported to, False if not
	 */
	public static boolean canBeTeleportedTo(String portalname, Position position) {
		if (portalname.contains("_")) {
			StringBuilder posName = new StringBuilder(30);
			posName.append(position.getWorldName()).append('_');
			posName.append(position.getBlockX()).append('_');
			posName.append(position.getBlockY()).append('_');
			posName.append(position.getBlockZ());
			if (portalname.equalsIgnoreCase(posName.toString())) {
				return false;
			}
		}
		return true;
	}

	public static Location getPortalLocation(String portalname, String world, boolean spawnlocation) {
		Location loc = getPortalLocation(portalname, world);
		if (spawnlocation) {
			return Util.spawnOffset(loc);
		} else {
			return loc;
		}
	}

	public static Location getPortalLocation(String portalname, String world) {
		if (portalname == null) {
			return null;
		}
		int dotindex = portalname.indexOf(".");
		if (dotindex != -1) {
			world = WorldManager.matchWorld(portalname.substring(0, dotindex));
			portalname = portalname.substring(dotindex + 1);
		}
		// Get a portal from a specified world
		if (world != null) {
			Position pos = getPortalLocations(world).get(portalname);
			if (pos != null) {
				Location loc = Util.getLocation(pos);
				if (loc != null) {
					return loc;
				}
			}
		}
		// Get 'a' portal whose names matches and whose location is available
		for (HashMap<String, Position> positions : portallocations.values()) {
			for (Map.Entry<String, Position> entry : positions.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(portalname)) {
					Location loc = Util.getLocation(entry.getValue());
					if (loc != null) {
						return loc;
					}
				}
			}
		}
		return null;
	}

	public static String[] getPortals(Chunk c) {
		ArrayList<String> rval = new ArrayList<String>();
		for (String name : getPortals()) {
			Location loc = getPortalLocation(name, c.getWorld().getName());
			if (loc != null && loc.getWorld() == c.getWorld()) {
				if (c.getX() == (loc.getBlockX() >> 4)) {
					if (c.getZ() == (loc.getBlockZ() >> 4)) {
						rval.add(name);
					}
				}
			}
		}
		return rval.toArray(new String[0]);
	}

	public static String[] getPortals(World w) {
		ArrayList<String> rval = new ArrayList<String>();
		for (HashMap<String, Position> positions : portallocations.values()) {
			for (Map.Entry<String, Position> entry : positions.entrySet()) {
				// Hide portals that can not be teleported to (does allow teleportation...)
				if (!canBeTeleportedTo(entry.getKey(), entry.getValue())) {
					continue;
				}
				if (entry.getValue().getWorldName().equals(w.getName())) {
					rval.add(entry.getKey());
				}
			}
		}
		return rval.toArray(new String[0]);
	}

	public static String[] getPortals() {
		HashSet<String> names = new HashSet<String>();
		for (HashMap<String, Position> positions : portallocations.values()) {
			for (Entry<String, Position> entry : positions.entrySet()) {
				// Hide portals that can not be teleported to (does allow teleportation...)
				if (!canBeTeleportedTo(entry.getKey(), entry.getValue())) {
					continue;
				}
				names.add(entry.getKey());
			}
		}
		return names.toArray(new String[0]);
	}

	public static void init(File file) {
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) { }
		}
		portallocations = new HashMap<String, HashMap<String, Position>>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try {
				String textline;
				while ((textline = reader.readLine()) != null) {
					String[] args = StringUtil.convertArgs(textline.split(" "));
					if (args.length == 7) {
						String name = args[0];
						try {
							Position pos = new Position(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Float.parseFloat(args[5]), Float.parseFloat(args[6]));
							getPortalLocations(args[1]).put(name, pos);
						} catch (Exception ex) {
							MyWorlds.plugin.log(Level.SEVERE, "Failed to load portal: " + name);
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				reader.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void deinit(File file) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			try {
				for (HashMap<String, Position> positions : portallocations.values()) {
					for (Map.Entry<String, Position> p : positions.entrySet()) {
						Position pos = p.getValue();
						writer.write("\"" + p.getKey() + "\" \"" + pos.getWorldName() + "\" ");
						writer.write(pos.getBlockX() + " " + pos.getBlockY() + " " + pos.getBlockZ() + " " + pos.getYaw() + " " + pos.getPitch());
						writer.newLine();
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				writer.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		portallocations = null;
	}
}

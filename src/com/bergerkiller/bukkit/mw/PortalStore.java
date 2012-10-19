package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

	public static Location getPortalLocation(String portalname, String world, boolean spawnlocation) {
		Location loc = getPortalLocation(portalname, world);
		if (loc != null && spawnlocation) return loc.add(0.5, 2, 0.5);
		return loc;
	}

	public static Location getPortalLocation(String portalname, String world) {
		if (portalname == null) return null;
		int dotindex = portalname.indexOf(".");
		if (dotindex != -1) {
			world = WorldManager.matchWorld(portalname.substring(0, dotindex));
			portalname = portalname.substring(dotindex + 1);
		}
		if (world != null) {
			Position pos = getPortalLocations(world).get(portalname);
			if (pos != null) {
				Location loc = Util.getLocation(pos);
			    if (loc != null) return loc;
			}
		}
    	for (HashMap<String, Position> positions : portallocations.values()) {
			for (Map.Entry<String, Position> entry : positions.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(portalname)) {
					return Util.getLocation(entry.getValue());
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
			names.addAll(positions.keySet());
		}
		return names.toArray(new String[0]);
	}

	public static void init(String filename) {
		portallocations = new HashMap<String, HashMap<String, Position>>();
		for (String textline : SafeReader.readAll(filename, true)) {
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
	}

	public static void deinit(String filename) {
		SafeWriter w = new SafeWriter(filename);
		for (HashMap<String, Position> positions : portallocations.values()) {
			for (Map.Entry<String, Position> p : positions.entrySet()) {
				Position pos = p.getValue();
				w.writeLine("\"" + p.getKey() + "\" \"" + pos.getWorldName() + "\" " + pos.getBlockX() + " " + pos.getBlockY() + " " + pos.getBlockZ() + " " + pos.getYaw() + " " + pos.getPitch());
			}
		}
		w.close();
		portallocations.clear();
		portallocations = null;
	}
}

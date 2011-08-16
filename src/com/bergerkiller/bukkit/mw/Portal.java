package com.bergerkiller.bukkit.mw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

public class Portal {
	private String name;
	private String destination;
	private Location location;
	
	public String getName() {
		return this.name;
	}
	public Location getLocation() {
		return this.location;
	}
	public String getDestinationName() {
		return this.destination;
	}
	public Location getDestination() {
		return getPortalLocation(destination);
	}
	public boolean hasDestination() {
		if (this.destination == null) return false;
		if (this.destination.trim().equals("")) return false;
		return getDestination() != null;
	}
	public boolean teleport(Player p) {
		return teleport(p);
	}
	public boolean teleport(Entity e) {
		Location dest = getDestination();
		if (dest != null && e != null) {
			e.teleport(dest);
			return true;
		}
		return false;
	}
			
	public void add() {
		portallocations.put(name, location);
	}
	public void remove() {
		portallocations.remove(name);
	}		
	public boolean isAdded() {
		return portallocations.containsKey(name);
	}

	public static void remove(String name) {
		portallocations.remove(name);
	}
	public static Portal get(String name) {
		return get(portallocations.get(name));
	}
	public static Portal get(Location signloc) {
		if (signloc == null) return null;
		return get(signloc.getBlock());
	}
	public static Portal get(Block signblock) {
		if (!signblock.getWorld().isChunkLoaded(signblock.getChunk())) {
			signblock.getWorld().loadChunk(signblock.getChunk());
		}
		if (signblock.getState() instanceof Sign) {
			return get(signblock, ((Sign) signblock.getState()).getLines());
		}
		return null;
	}
	public static Portal get(Block signblock, String[] lines) {
		if (signblock.getState() instanceof Sign) {
			if (lines[0].equalsIgnoreCase("[portal]")) {
				String name = lines[1];
				if (name != null && name.trim().equals("") == false) {
					Portal p = new Portal();
					p.name = name;
					p.destination = lines[2];
					p.location = signblock.getLocation();
			    	MaterialData data = signblock.getState().getData();
			    	float yaw = 0;
			    	if (data instanceof Directional) {
			    		switch (((Directional) data).getFacing()) {
			    		case NORTH: yaw = 90; break;		
			    		case NORTH_EAST: yaw = 135; break;		
			    		case EAST: yaw = 180; break;	 
			    		case SOUTH_EAST: yaw = 225; break;	
			    		case SOUTH: yaw = 270; break;	
			    		case SOUTH_WEST: yaw = 315; break;	
			    		case WEST: yaw = 0; break;
			    		case NORTH_WEST: yaw = 45; break;	
			    		}
			    	}
			    	p.location.setYaw(yaw);
					return p;
				}
			}
		}
		return null;
	}

	public static String[] getPortals() {
		return portallocations.keySet().toArray(new String[0]);
	}
	public static String[] getPortals(World w) {
		ArrayList<String> rval = new ArrayList<String>();
		for (String name : portallocations.keySet()) {
			Location loc = portallocations.get(name);
			if (loc != null && loc.getWorld() == w) {
				rval.add(name);
			}
		}
		return rval.toArray(new String[0]);
	}
	public static String[] getPortals(Chunk c) {
		ArrayList<String> rval = new ArrayList<String>();
		for (String name : portallocations.keySet()) {
			Location loc = portallocations.get(name);
			if (loc != null && loc.getWorld() == c.getWorld() && loc.getBlock().getChunk() == c) {
				rval.add(name);
			}
		}
		return rval.toArray(new String[0]);
	}
	public static Portal getPortal(Location loc, double radius) {
		double mindist = Double.MAX_VALUE;
		Portal p = null;
		for (String portalname : portallocations.keySet()) {
			Location ploc = portallocations.get(portalname);
			if (ploc != null && ploc.getWorld() == loc.getWorld()) {
				double distance = ploc.distance(loc);
				if (distance < mindist) {
					Portal newp = Portal.get(ploc);
					if (newp != null) {
						p = newp;
						mindist = distance;
					}
				}
			}
		}
		return p;
	}
	public static Location getPortalLocation(String portalname) {
		if (portalname == null) return null;
		Location loc = portallocations.get(portalname);
		if (loc == null) {
			for (String name : portallocations.keySet()) {
				if (name.equalsIgnoreCase(portalname)) {
					loc = portallocations.get(name);
					break;
				}
			}
		}
		
		if (loc != null) {
			return loc.clone().add(0.5, 2, 0.5);
		} else {
			portalname = WorldManager.matchWorld(portalname);
			World w = WorldManager.getWorld(portalname);
			if (w == null) return null;
			return w.getSpawnLocation();
		}
	}
	
	public static boolean loadPortals(String filename) {
		try {
			File f = new File(filename);
			if (f.exists()) {
				BufferedReader r = new BufferedReader(new FileReader(f));
				String textline = null;
				while((textline = r.readLine()) != null) {
					String[] args = textline.split(" ");
					if (args.length == 7) {
						String name = args[0];
						//load worlds?
						//=============
						World w = WorldManager.getOrCreateWorld(args[1]);
						//=============
						if (w != null) {
							try {
								Location loc = new Location(w, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Float.parseFloat(args[5]), Float.parseFloat(args[6]));
								portallocations.put(name, loc);
							} catch (Exception ex) {
								MyWorlds.log(Level.SEVERE, "[MyWorlds] Failed to load portal: " + name);
							}
						} else {
							MyWorlds.log(Level.WARNING, "[MyWorlds] Failed to load world for portal: " + name);
						}
					}
				}
				r.close();
				return true;
			} else {
				MyWorlds.log(Level.SEVERE, "[MyWorlds] Portal configuration file not found: " + filename);
			}
			MyWorlds.log(Level.INFO, "[MyWorlds] " + portallocations.size() + " portals found in " + Bukkit.getServer().getWorlds().size() + " worlds.");
		} catch (IOException ex) {
			MyWorlds.log(Level.SEVERE, "[MyWorlds] Failed to access portal configuration file: " + filename);
			ex.printStackTrace();
		}
		return false;
	}
	public static boolean savePortals(String filename) {
		try {
			File f = new File(filename);
			File dir = f.getParentFile();
			if ((dir.exists() || dir.mkdirs()) && (f.exists() == false || f.delete())) {
				BufferedWriter w = new BufferedWriter(new FileWriter(f));
				for (String portal : getPortals()) {
					Location loc = portallocations.get(portal);
					w.write(portal + " " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + loc.getYaw() + " " + loc.getPitch());
					w.newLine();
				}
				w.close();
				return true;
			} else {
				MyWorlds.log(Level.SEVERE, "[MyWorlds] Failed to save portal configuration to file: " + filename);
			}
		} catch (IOException ex) {
			MyWorlds.log(Level.SEVERE, "[MyWorlds] Failed to save portal configuration to file: " + filename);
			ex.printStackTrace();
		}
		return false;
	}
	
	private static HashMap<String, Location> portallocations = new HashMap<String, Location>();
}

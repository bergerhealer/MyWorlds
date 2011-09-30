package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;
import org.bukkit.util.config.Configuration;

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
	public String getWorldName() {
		return portalworlds.get(this.name);
	}
	public Location getDestination() {
		Location loc = getPortalLocation(destination, true);
		if (loc == null) {
			String portalname = WorldManager.matchWorld(destination);
			World w = WorldManager.getWorld(portalname);
			if (w != null) {
				loc = w.getSpawnLocation();
			}
		}
		return loc;
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
		portalworlds.put(name, location.getWorld().getName());
	}
	public void remove() {
		remove(this.name);
	}		
	public boolean isAdded() {
		return portallocations.containsKey(name);
	}

	/*
	 * Getters and setters
	 */
	public static void remove(String name) {
		portallocations.remove(name);
		portalworlds.remove(name);
	}
	public static Portal get(String name) {
		return get(getPortalLocation(name));
	}
	public static Portal get(Location signloc) {
		if (signloc == null) return null;
		return get(signloc.getBlock(), false);
	}
	public static Portal get(Location signloc, double radius) {
		Portal p = null;
		for (String portalname : portallocations.keySet()) {
			Location ploc = getPortalLocation(portalname);
			if (ploc != null && ploc.getWorld() == signloc.getWorld()) {
				double distance = ploc.distance(signloc);
				if (distance <= radius) {
					Portal newp = Portal.get(ploc);
					if (newp != null) {
						p = newp;
						radius = distance;
					}
				}
			}
		}
		return p;
	}
	
	public static Portal get(Block signblock, boolean loadchunk) {
		int cx = signblock.getLocation().getBlockX() >> 4;
		int cz = signblock.getLocation().getBlockZ() >> 4;
		if (!signblock.getWorld().isChunkLoaded(cx, cz)) {
			if (loadchunk) {
				signblock.getWorld().loadChunk(cx, cz);
			} else {
				return null;
			}
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
					p.name = name.replace("\"", "").replace("'", "");
					p.destination = lines[2].replace("\"", "").replace("'", "");
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
		for (String name : portalworlds.keySet()) {
			if (getPortalWorld(name).equals(w.getName())) {
				rval.add(name);
			}
		}
		return rval.toArray(new String[0]);
	}
	public static String[] getPortals(Chunk c) {
		ArrayList<String> rval = new ArrayList<String>();
		for (String name : getPortals()) {
			Location loc = getPortalLocation(name);
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
	public static Location getPortalLocation(String portalname) {
		if (portalname == null) return null;
		Location loc = portallocations.get(portalname);
	    if (loc != null) {
			World w = WorldManager.getWorld(getPortalWorld(portalname));
	    	if (w != null) {
	    		loc.setWorld(w);
	    	} else {
	    		return null;
	    	}
	    }
		if (loc == null) {
			for (String name : portallocations.keySet()) {
				if (name.equalsIgnoreCase(portalname)) {
					return getPortalLocation(name);
				}
			}
		} else {
			return loc.clone();
		}
		return null;
	}
	public static Location getPortalLocation(String portalname, boolean spawnlocation) {
		Location loc = getPortalLocation(portalname);
		if (loc != null && spawnlocation) return loc.add(0.5, 2, 0.5);
		return loc;
	}
	
	public static String getPortalWorld(String portalname) {
		return portalworlds.get(portalname);
	}
	
	private static class PortalValidateCommand implements Runnable {
		public PortalValidateCommand(String... portalnames) {
			this.portalnames = portalnames;
		}
		
		private String[] portalnames;
		
		@Override
		public void run() {
			for (String portalname : portalnames) {
				Location loc = getPortalLocation(portalname);
				if (loc != null) {
					if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
						//validate
						if (get(loc) == null) {
			    			MyWorlds.log(Level.WARNING, "Auto-removed portal '" + portalname + "' because it is no longer there!");
			    			remove(portalname);
						}
					}
				}
			}
		}
	}
	
	public static void validate(int delay, World world) {
		validate(delay, getPortals(world));
	}
	public static void validate(int delay, Chunk chunk) {
		validate(delay, getPortals(chunk));
	}
	public static void validate(int delay, String... portalnames) {
		PortalValidateCommand cmd = new PortalValidateCommand(portalnames);
		if (delay < 0) {
			cmd.run();
		} else {
			MyWorlds.plugin.getServer().getScheduler().scheduleSyncDelayedTask(MyWorlds.plugin, cmd);
		}
	}
     
    /*
     * Teleportation and teleport defaults
     */
    private static HashMap<Entity, Long> portaltimes = new HashMap<Entity, Long>();
    private static ArrayList<TeleportCommand> teleportations = new ArrayList<TeleportCommand>();  
    public static void setDefault(String worldname, String destination) {
    	if (destination == null) {
        	defaultlocations.remove(worldname.toLowerCase());
    	} else {
        	defaultlocations.put(worldname.toLowerCase(), destination);
    	}
    }
    public static void handlePortalEnter(Entity e) {
        long currtime = System.currentTimeMillis();
        if (!portaltimes.containsKey(e) || currtime - portaltimes.get(e) >= MyWorlds.teleportInterval) {
        	Portal portal = get(e.getLocation(), 5);  	
        	if (portal == null) {
        		//Default portals
        		String def = defaultlocations.get(e.getWorld().getName().toLowerCase());
        		if (def != null) portal = Portal.get(def);
        		if (portal == null) {
        			//world spawn?
        			World w = WorldManager.getWorld(def);
        			if (w != null) {
        				if (Permission.handleTeleport(e, w.getSpawnLocation())) {
        					MyWorlds.message(e, Localization.getWorldEnter(w));
        				}
        			} else {
        				//Additional destinations??
        			}
        		}
        	}
        	if (portal != null) {
        		delayedTeleport(portal, e);
        	}
    	}
        portaltimes.put(e, currtime);
    }
    private static class TeleportCommand {
    	public Entity e;
    	public Portal portal;
    	public TeleportCommand(Entity e, Portal portal) {
    		this.e = e;
    		this.portal = portal;
    	}
    }
    public static void delayedTeleport(Portal portal, Entity e) {
    	teleportations.add(new TeleportCommand(e, portal));
    	MyWorlds.plugin.getServer().getScheduler().scheduleSyncDelayedTask(MyWorlds.plugin, new Runnable() {
    	    public void run() {
    	    	TeleportCommand telec = teleportations.remove(0);
	    		if (telec.portal.hasDestination()) {
	    			if (Permission.handleTeleport(telec.e, telec.portal)) {
	    				//Success
	    			}
    			} else {
    				MyWorlds.message(telec.e, Localization.get("portal.nodestination"));
    			}
    	    }
    	}, 0L);
    }
	
    /*
     * Loading and saving
     */
	public static void loadDefaultPortals(Configuration config, String worldname) {
		String def = config.getString(worldname + ".defaultPortal", null);
		if (def != null) {
			defaultlocations.put(worldname.toLowerCase(), def);
		}
	}
	public static void saveDefaultPortals(Configuration config) {
		for (String worldname : config.getKeys()) {
			if (!defaultlocations.containsKey(worldname.toLowerCase())) {
				config.removeProperty(worldname + ".defaultPortal");
			}
		}
		for (Map.Entry<String, String> entry : defaultlocations.entrySet()) {
			config.setProperty(entry.getKey() + ".defaultPortal", entry.getValue());
		}
	}
	public static void loadPortals(String filename) {
		for (String textline : SafeReader.readAll(filename, true)) {
			String[] args = MyWorlds.convertArgs(textline.split(" "));
			if (args.length == 7) {
				String name = args[0];
				portalworlds.put(name, args[1]);
				try {
					Location loc = new Location(null, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Float.parseFloat(args[5]), Float.parseFloat(args[6]));
					portallocations.put(name, loc);
				} catch (Exception ex) {
					MyWorlds.log(Level.SEVERE, "Failed to load portal: " + name);
				}
			}
		}
	}
	public static void savePortals(String filename) {
		SafeWriter w = new SafeWriter(filename);
		for (String portal : getPortals()) {
			Location loc = portallocations.get(portal);
			String world = portalworlds.get(portal);
			w.writeLine("\"" + portal + "\" \"" + world + "\" " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + loc.getYaw() + " " + loc.getPitch());
		}
		w.close();
	}
	
	private static HashMap<String, String> defaultlocations = new HashMap<String, String>();
	private static HashMap<String, Location> portallocations = new HashMap<String, Location>();
	private static HashMap<String, String> portalworlds = new HashMap<String, String>();
}

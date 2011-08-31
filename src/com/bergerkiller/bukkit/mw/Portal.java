package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
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
		Location loc = getPortalLocation(destination);
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
		return get(signloc.getBlock(), false);
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
		Portal p = null;
		for (String portalname : portallocations.keySet()) {
			Location ploc = portallocations.get(portalname);
			if (ploc != null && ploc.getWorld() == loc.getWorld()) {
				double distance = ploc.distance(loc);
				if (distance < radius) {
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
		if (loc == null) return null;
		return loc.clone().add(0.5, 2, 0.5);
	}
	
    private static HashMap<Entity, Long> portaltimes = new HashMap<Entity, Long>();
    private static ArrayList<TeleportCommand> teleportations = new ArrayList<TeleportCommand>();  
     
    public static void setDefault(String worldname, String destination) {
    	defaultlocations.put(worldname.toLowerCase(), destination);
    }
    public static void handlePortalEnter(Entity e) {
        long currtime = System.currentTimeMillis();
        if (!portaltimes.containsKey(e) || currtime - portaltimes.get(e) >= MyWorlds.teleportInterval) {
        	Portal portal = getPortal(e.getLocation(), 5);  	
        	if (portal == null) {
        		//Default portals
        		String def = defaultlocations.get(e.getWorld().getName().toLowerCase());
        		if (def != null) portal = Portal.get(def);
        		if (portal == null) {
        			//world spawn?
        			World w = WorldManager.getWorld(def);
        			if (w != null) {
        				e.teleport(w.getSpawnLocation());
        			} else {
        				
        			}
        		}
        	}
        	if (portal != null) {
        		if (!(e instanceof Player) || Permission.has((Player) e, "portal.use")) {
        			delayedTeleport(portal, e);
        		}
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
    	    	boolean worked = telec.portal.teleport(telec.e);
    	    	if (telec.e instanceof Player) {
    	    		if (worked) {
        				((Player) telec.e).sendMessage(ChatColor.GREEN + "You teleported to " + ChatColor.WHITE + telec.portal.getDestinationName() + ChatColor.GREEN + ", have a nice stay!");
        			} else {
        				((Player) telec.e).sendMessage(ChatColor.YELLOW + "This portal has no destination!");
        			}
        		}
    	    }
    	}, 1L);
    }
	
	public static void loadDefaultPortals(String filename) {
		for (String textline : SafeReader.readAll(filename, true)) {
			String[] args = textline.split(" ");
			if (args.length == 2) {
				defaultlocations.put(args[0].toLowerCase(), args[1]);		
			}
		}
	}
	public static void saveDefaultPortals(String filename) {
		SafeWriter writer = new SafeWriter(filename);
		for (String key : defaultlocations.keySet()) {
			writer.writeLine(key.toLowerCase() + " " + defaultlocations.get(key));
		}
		writer.close();
	}
	public static void loadPortals(String filename) {
		for (String textline : SafeReader.readAll(filename, true)) {
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
	}
	public static void savePortals(String filename) {
		SafeWriter w = new SafeWriter(filename);
		for (String portal : getPortals()) {
			Location loc = portallocations.get(portal);
			w.writeLine(portal.replace(" ", "_") + " " + loc.getWorld().getName().replace(" ", "_") + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + loc.getYaw() + " " + loc.getPitch());
		}
		w.close();
	}
	
	private static HashMap<String, String> defaultlocations = new HashMap<String, String>();
	private static HashMap<String, Location> portallocations = new HashMap<String, Location>();
}

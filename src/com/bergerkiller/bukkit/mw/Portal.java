package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;

public class Portal {
	private String name;
	private String destination;
	private String destdisplayname;
	private Location location;
	
	public String getName() {
		return this.name;
	}
	public Location getLocation() {
		return this.location;
	}
	public String getDestinationName() {
		return fixDot(this.destination);
	}
	public String getDestinationDisplayName() {
		if (this.destdisplayname == null || this.destdisplayname.equals("")) {
			this.destdisplayname = fixDot(this.destination);
		}
		return this.destdisplayname;
	}
	public Location getDestination() {
		Location loc = getPortalLocation(destination, location.getWorld().getName(), true);
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
		getPortalLocations(location.getWorld().getName()).put(name, new Position(location));
	}
	public boolean remove() {
		return remove(this.name, this.location.getWorld().getName());
	}		
	public boolean isAdded() {
		return getPortalLocations(this.location.getWorld().getName()).containsKey(name);
	}
    
	/*
	 * Getters and setters
	 */
	public static boolean exists(String world, String portalname) {
		return getPortalLocations(world).containsKey(portalname);
	}
	public static String fixDot(String portalname) {
		if (portalname == null) return null;
		return portalname.substring(portalname.indexOf(".") + 1);
	}
	public static boolean remove(String name, String world) {
		return getPortalLocations(world).remove(name) != null;
	}
	public static Portal get(String name) {
		return get(getPortalLocation(name, null));
	}
	public static Portal get(Location signloc) {
		if (signloc == null) return null;
		return get(signloc.getBlock(), false);
	}
	public static Portal get(Location signloc, double radius) {
		Portal p = null;
		HashMap<String, Position> positions = getPortalLocations(signloc.getWorld().getName());
		for (Map.Entry<String, Position> pos : positions.entrySet()) {
			Location ploc = getPortalLocation(pos.getValue());
			String portalname = pos.getKey();
			if (ploc != null && ploc.getWorld() == signloc.getWorld()) {
				double distance = ploc.distance(signloc);
				if (distance <= radius) {
					Portal newp = Portal.get(ploc);
					if (newp != null) {
						p = newp;
						radius = distance;
					} else if (ploc.getWorld().isChunkLoaded(ploc.getBlockX() >> 4, ploc.getBlockZ() >> 4)) {
						//In loaded chunk and NOT found!
						//Remove it
						positions.remove(portalname);
						MyWorlds.plugin.log(Level.WARNING, "Removed portal '" + portalname + "' because it is no longer there!");
						//End the loop and call the function again
						return get(signloc, radius);
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
					p.destdisplayname = lines[3];
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
		HashSet<String> names = new HashSet<String>();
		for (HashMap<String, Position> positions : portallocations.values()) {
			names.addAll(positions.keySet());
		}
		return names.toArray(new String[0]);
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
	
	private static Location getPortalLocation(Position portalpos) {
		if (portalpos != null) {
			Location loc = portalpos.toLocation();
	    	if (loc.getWorld() != null) {
	    		return loc;
	    	}
		}
		return null;
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
				Location loc = getPortalLocation(pos);
			    if (loc != null) return loc;
			}
		}
    	for (HashMap<String, Position> positions : portallocations.values()) {
			for (Map.Entry<String, Position> entry : positions.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(portalname)) {
					return getPortalLocation(entry.getValue());
				}
			}
    	}
    	return null;
	}
	public static Location getPortalLocation(String portalname, String world, boolean spawnlocation) {
		Location loc = getPortalLocation(portalname, world);
		if (loc != null && spawnlocation) return loc.add(0.5, 2, 0.5);
		return loc;
	}
	
    /*
     * Teleportation and teleport defaults
     */
	public static boolean isPortal(Block main, BlockFace direction) {
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
	public static boolean isPortal(Block main) {
		if (isPortal(main, BlockFace.UP) && isPortal(main, BlockFace.DOWN)) {
			if (isPortal(main, BlockFace.NORTH) && isPortal(main, BlockFace.SOUTH)) return true;
			if (isPortal(main, BlockFace.EAST) && isPortal(main, BlockFace.WEST)) return true;
		}
		return false;
	}
	
	public static void handlePlayerMove(Player player, Location to) {
		Location loc = walkDistanceCheckMap.get(player);
		if (loc != null) {
			if (loc.getWorld() != to.getWorld() || loc.distanceSquared(to) > 1.0) {
				walkDistanceCheckMap.remove(player);
			}
		}
	}
	
	public static void notifyNoMove(Player player) {
		walkDistanceCheckMap.put(player, player.getLocation());
	}
	
	private static WeakHashMap<Player, Location> walkDistanceCheckMap = new WeakHashMap<Player, Location>();
    private static WeakHashMap<Entity, Long> portaltimes = new WeakHashMap<Entity, Long>();
    public static void handlePortalEnter(Entity e) {
    	if (walkDistanceCheckMap.containsKey(e)) {
    		return;
    	}
        long currtime = System.currentTimeMillis();
    	long lastteleport;
    	if (portaltimes.containsKey(e)) {
    		lastteleport = portaltimes.get(e);
    	} else {
    		lastteleport = currtime - MyWorlds.teleportInterval;
    		portaltimes.put(e, lastteleport);
    	}
        if (currtime - lastteleport >= MyWorlds.teleportInterval) {
        	portaltimes.put(e, currtime);
        	Portal portal = get(e.getLocation(), 5);  	
        	if (portal == null) {
        		//Default portals
        		String def = WorldConfig.get(e).defaultPortal;
        		if (def != null) {
        			Location loc = getPortalLocation(def, e.getWorld().getName(), true);
        			if (loc == null) {
        				//world spawn?
            			World w = WorldManager.getWorld(def);
            			if (w != null) {
            				delayedTeleport(null, WorldManager.getSpawnLocation(w), null, e);
            			} else {
            				//Additional destinations??
            			}
        			} else {
        				delayedTeleport(null, loc, def, e);
        			}
        		}
        	} else {
        		delayedTeleport(portal, null, null, e);
        	}
    	}
    }

    public static void delayedTeleport(final Portal portal, final Location dest, final String destname, final Entity e) {
    	new Task(MyWorlds.plugin) {
    		public void run() {
    	    	if (portal == null) {
    	    		if (destname == null) {
    	    			Permission.handleTeleport(e, dest);
    	    		} else {
    	    			Permission.handleTeleport(e, destname, dest);
    	    		}
    	    	} else {
    	    		if (portal.hasDestination()) {
    	    			if (Permission.handleTeleport(e, portal)) {
    	    				//Success
    	    			}
        			} else {
        				CommonUtil.sendMessage(e, Localization.get("portal.nodestination"));
        			}
    	    	}
    		}
    	}.start();
    }
	
    /*
     * Loading and saving
     */
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
	
	private static HashMap<String, HashMap<String, Position>> portallocations;
	private static HashMap<String, Position> getPortalLocations(String worldname) {
		worldname = worldname.toLowerCase();
		HashMap<String, Position> rval = portallocations.get(worldname);
		if (rval == null) {
			rval = new HashMap<String, Position>();
			portallocations.put(worldname, rval);
		}
		return rval;
	}
}

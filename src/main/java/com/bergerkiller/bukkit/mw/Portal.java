package com.bergerkiller.bukkit.mw;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class Portal extends PortalStore {
	private String name;
	private String destination;
	private String destdisplayname;
	private Location location;

	/**
	 * Gets the identifier name of this Portal
	 * 
	 * @return Portal identifier name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gets the Location of this Portal
	 * 
	 * @return Portal location
	 */
	public Location getLocation() {
		return this.location;
	}

	/**
	 * Gets the destination name of this Portal
	 * 
	 * @return Portal destination name
	 */
	public String getDestinationName() {
		if (this.destination == null) {
			return null;
		} else {
			return this.destination.substring(this.destination.indexOf('.') + 1);
		}
	}

	/**
	 * Gets the destination display name of this Portal
	 * 
	 * @return Portal destination display name
	 */
	public String getDestinationDisplayName() {
		return this.destdisplayname;
	}

	/**
	 * Gets the destination location of this Portal
	 * 
	 * @return Portal destination
	 */
	public Location getDestination() {
		final String worldName = this.location == null ? null : this.location.getWorld().getName();
		Location loc = getPortalLocation(this.destination, worldName, true);
		if (loc == null) {
			String portalname = WorldManager.matchWorld(this.destination);
			World w = WorldManager.getWorld(portalname);
			if (w != null) {
				loc = WorldManager.getSpawnLocation(w);
			}
		}
		return loc;
	}

	/**
	 * Checks if this Portal has an available destination
	 * 
	 * @return True if it has a destination, False if not
	 */
	public boolean hasDestination() {
		if (this.destination == null || this.destination.trim().isEmpty()) {
			return false;
		}
		return getDestination() != null;
	}

	/**
	 * Gets a new non-existing Portal object that teleports to this Portal
	 * 
	 * @return new Portal with this Portal as destination
	 */
	public Portal getOtherEnd() {
		Portal p = new Portal();
		p.name = "Unknown";
		p.destdisplayname = p.destination = this.name;
		p.location = null;
		return p;
	}

	/**
	 * Teleports an Entity to the location of this Portal
	 * 
	 * @param entity to teleport
	 * @return True if successful, False if not
	 */
	public boolean teleportSelf(Entity entity) {
		if (entity instanceof Player) {
			MWListenerPost.setLastEntered((Player) entity, this.getOtherEnd());
		}
		boolean rval = EntityUtil.teleport(entity, Util.spawnOffset(this.getLocation()));
		return rval;
	}

	/**
	 * Adds this Portal to the internal mapping
	 */
	public void add() {
		getPortalLocations(location.getWorld().getName()).put(name, new Position(location));
	}

	/**
	 * Removes this Portal from the internal mapping
	 * 
	 * @return True if successful, False if not
	 */
	public boolean remove() {
		return remove(this.name, this.location.getWorld().getName());
	}

	/**
	 * Checks if this Portal is added to the internal mapping
	 * 
	 * @return True if it is added, False if not
	 */
	public boolean isAdded() {
		return getPortalLocations(this.location.getWorld().getName()).containsKey(this.getName());
	}

	@Override
	public String toString() {
		return "Portal {name=" + getName() + ", loc=" + getLocation() + ", dest=" + (hasDestination() ? getDestinationName() : "None") + "}";
	}

	public static boolean exists(String world, String portalname) {
		return getPortalLocations(world).containsKey(portalname);
	}

	public static boolean remove(String name, String world) {
		return getPortalLocations(world).remove(name) != null;
	}

	/**
	 * Gets the nearest portal (sign) near a given point using the SEARCH_RADIUS as radius
	 * 
	 * @param middle point of the sphere to look in
	 * @return Nearest portal, or null if none are found
	 */
	public static Portal getNear(Location middle) {
		return getNear(middle, MyWorlds.maxPortalSignDistance);
	}

	/**
	 * Gets the nearest portal (sign) in a given spherical area
	 * 
	 * @param middle point of the sphere to look in
	 * @param radius of the sphere to look in
	 * @return Nearest portal, or null if none are found
	 */
	public static Portal getNear(Location middle, double radius) {
		Portal p = null;
		HashMap<String, Position> positions = getPortalLocations(middle.getWorld().getName());
		for (Map.Entry<String, Position> pos : positions.entrySet()) {
			Location ploc = Util.getLocation(pos.getValue());
			String portalname = pos.getKey();
			if (ploc != null && ploc.getWorld() == middle.getWorld()) {
				double distance = ploc.distance(middle);
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
						return getNear(middle, radius);
					}
				}
			}
		}
		return p;
	}

	public static Portal get(String name) {
		return get(getPortalLocation(name, null));
	}

	public static Portal get(Location signloc) {
		if (signloc == null) return null;
		return get(signloc.getBlock(), false);
	}

	public static Portal get(Block signblock, boolean loadchunk) {
		if (loadchunk) {
			signblock.getChunk();
		} else if (!WorldUtil.isLoaded(signblock)) {
			return null;
		}
		if (signblock.getState() instanceof Sign) {
			return get(signblock, ((Sign) signblock.getState()).getLines());
		}
		return null;
	}

	public static Portal get(Block signblock, String[] lines) {
		if (signblock.getState() instanceof Sign && lines[0].equalsIgnoreCase("[portal]")) {
			Portal p = new Portal();
			// Read name, if none set, use portal location as name
			p.name = Util.filterPortalName(lines[1]);
			if (LogicUtil.nullOrEmpty(p.name)) {
				p.name = StringUtil.blockToString(signblock);
			}
			// Read destination, if none set, set to null so it's clear there is no destination set
			p.destination = Util.filterPortalName(lines[2]);
			if (p.destination.isEmpty()) {
				p.destination = null;
			}
			// Read destination name, if none set, use destination instead
			if (lines[3].isEmpty()) {
				p.destdisplayname = p.getDestinationName();
			} else {
				p.destdisplayname = lines[3];
			}
			// Set portal locatiol using sign location and orientation
			p.location = signblock.getLocation();
			MaterialData data = signblock.getState().getData();
			float yaw = 0;
			if (data instanceof Directional) {
				yaw = FaceUtil.faceToYaw(((Directional) data).getFacing()) + 90;
			}
			p.location.setYaw(yaw);
			return p;
		}
		return null;
	}

	/**
	 * Handles an entity entering a certain portal block
	 * 
	 * @param e that entered
	 * @param portalMaterial of the block that was used as portal
	 * @param useTravelAgent sets whether to find nearby portals if required
	 * @return A Portal object or a Location object to represent the destination
	 */
	public static Object getPortalEnterDestination(Entity e, Material portalMaterial) {
		Location dest = null;
		Portal portal = getNear(e.getLocation());
		if (portal == null) {
			// Default portals
			String def = null;
			if (portalMaterial == Material.PORTAL) {
				def = WorldConfig.get(e).getNetherPortal();
			} else if (portalMaterial == Material.ENDER_PORTAL) {
				def = WorldConfig.get(e).getEnderPortal();
			}
			if (def != null) {
				portal = get(getPortalLocation(def, e.getWorld().getName()));
				if (portal != null) {
					// Teleport to a specific portal - change perspective
					portal = portal.getOtherEnd();
				} else {
					// Is it a world spawn?
					World w = WorldManager.getWorld(def);
					if (w != null) {
						if (MyWorlds.allowPersonalPortals) {
							// What environment are we coming from?
							Environment oldEnvironment = e.getWorld().getEnvironment();
							Environment newEnvironment = w.getEnvironment();
							if (oldEnvironment == Environment.THE_END) {
								// No special portal type or anything is used
								// Instead, teleport to a personal bed or otherwise world spawn
								if (e instanceof Player) {
									dest = ((Player) e).getBedSpawnLocation();
								}
							} else {
								// Find out what location to teleport to
								// Use source block as the location to search from
								double blockRatio = 1.0;
								if (oldEnvironment != newEnvironment) {
									if (newEnvironment == Environment.NETHER) {
										blockRatio = 0.125;
									} else if (oldEnvironment == Environment.NETHER) {
										blockRatio = 8.0;
									}
								}
								Location start = e.getLocation();
								Location end = new Location(w, blockRatio * start.getX(), start.getY(), blockRatio * start.getZ());
								if (end != null) {
									Block destB = end.getBlock();
									// Figure out the best yaw to use here by checking for air blocks
									float yaw = 0.0f;
									for (BlockFace face : FaceUtil.AXIS) {
										if (destB.getRelative(face).getTypeId() == Material.AIR.getId()) {
											yaw = FaceUtil.faceToYaw(face) + 90f;
											break;
										}
									}
									dest = new Location(destB.getWorld(), destB.getX() + 0.5, destB.getY(), destB.getZ() + 0.5, yaw, 0.0f);
								}
							}
						}
						// No destination found or set, resort back to using world spawn
						if (dest == null) {
							if (e instanceof Player) {
								dest = WorldManager.getPlayerWorldSpawn((Player) e, w);
							} else {
								dest = WorldManager.getSpawnLocation(w);
							}
						}
					}
				}
			}
		}
		// If a portal was found, teleport using it, otherwise use destination Location
		return portal != null ? portal : dest;
	}
}

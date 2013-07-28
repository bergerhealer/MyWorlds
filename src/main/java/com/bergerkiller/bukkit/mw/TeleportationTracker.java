package com.bergerkiller.bukkit.mw;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.collections.EntityMap;
import com.bergerkiller.bukkit.common.utils.EntityUtil;

/**
 * Keeps track of the portals player enter, restricting too much teleporting.
 * This is here to avoid loops in teleports, or getting stuck in a portal.
 */
public class TeleportationTracker {
	// A mapping of player positions to prevent spammed portal teleportation
	private final EntityMap<Player, Location> walkDistanceCheckMap = new EntityMap<Player, Location>();
	// Portal times for a minimal delay
	private final EntityMap<Entity, Long> portaltimes = new EntityMap<Entity, Long>();

	/**
	 * Sets a portal point, so players will no longer attempt to enter it next time
	 * 
	 * @param player to set
	 * @param location of the Portal to set
	 */
	public void setPortalPoint(Player player, Location location) {
		walkDistanceCheckMap.put(player, location);
		portaltimes.put(player, System.currentTimeMillis());
		EntityUtil.setAllowTeleportation(player, false);
		EntityUtil.setPortalCooldown(player, EntityUtil.getPortalCooldownMaximum(player));
	}

	/**
	 * Updates the position of a player, keeping track of players walking outside of a portal radius.
	 * 
	 * @param player to update the position of
	 * @param position of the player
	 */
	public void updatePlayerPosition(Player player, Location position) {
		Location loc = walkDistanceCheckMap.get(player);
		if (loc != null) {
			if (loc.getWorld() != position.getWorld()) {
				// Put in proper world
				walkDistanceCheckMap.put(player, position);
			} else if (loc.distanceSquared(position) > 2.25) {
				// Moved outside radius - remove point
				walkDistanceCheckMap.remove(player);
			}
		}
	}

	/**
	 * Handles the teleport delay and distance checks
	 * 
	 * @param e Entity to pre-teleport
	 * @param portalMaterial of the portal
	 * @return True if teleporting happened, False if not
	 */
	public boolean canTeleport(Entity e) {
    	if (walkDistanceCheckMap.containsKey(e)) {
    		return false;
    	}
    	Long lastTeleport = portaltimes.get(e);
    	if (lastTeleport != null) {
    		final long currtime = System.currentTimeMillis();
    		if (currtime - lastTeleport.longValue() < MyWorlds.teleportInterval) {
    			return false;
    		}
    	}
    	return true;
	}
}

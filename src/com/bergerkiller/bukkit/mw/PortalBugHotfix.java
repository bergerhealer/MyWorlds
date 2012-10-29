package com.bergerkiller.bukkit.mw;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.utils.MaterialUtil;

/**
 * Hotfix to prevent wrongful teleportation
 */
public class PortalBugHotfix implements Listener {
	private static Map<Entity, Location> lastPortalPos = new HashMap<Entity, Location>();
	private static Map<Entity, Vector> lastVelocity = new HashMap<Entity, Vector>();

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		lastPortalPos.remove(event.getPlayer());
		lastVelocity.remove(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled()) {
			return;
		}
		lastVelocity.put(event.getPlayer(), event.getTo().subtract(event.getFrom()).toVector());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPortal(PlayerPortalEvent event) {
		Location pos = lastPortalPos.remove(event.getPlayer());
		if (pos != null) {
			Vector dir = lastVelocity.get(event.getPlayer());
			if (dir == null) {
				dir = new Vector();
			}
			Location from = event.getFrom().clone().add(dir);
			if (from.getWorld() != pos.getWorld()) {
				event.setCancelled(true);
			} else if (from.distance(pos) > 5.0) {
				event.setCancelled(true);
			} else if (!MaterialUtil.isType(from.getBlock(), Material.PORTAL, Material.ENDER_PORTAL)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityPortalEnter(EntityPortalEnterEvent event) {
		if (event.getEntity() instanceof Player) {
			lastPortalPos.put(event.getEntity(), event.getLocation());
		}
	}
}

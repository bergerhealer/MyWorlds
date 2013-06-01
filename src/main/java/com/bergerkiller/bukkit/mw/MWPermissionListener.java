package com.bergerkiller.bukkit.mw;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.bergerkiller.bukkit.common.utils.MaterialUtil;

/**
 * Handles events to manage plugin-defined permissions and messages
 */
public class MWPermissionListener implements Listener {
	public static Portal lastEnteredPortal = null; // Used for permissions and TP messages

	public static boolean handleTeleportPermission(Player player, Location to) {
		// World can be entered?
		if (Permission.canEnter(player, to.getWorld())) {
			// If applicable, Portal can be entered?
			if (lastEnteredPortal != null) {
				String name = lastEnteredPortal.getName();
				if (name != null && !Permission.canEnterPortal(player, name)) {
					Localization.PORTAL_NOACCESS.message(player);
					return false;
				}
			}
		} else {
			Localization.WORLD_NOACCESS.message(player);
			return false;
		}
		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerTeleportPerm(PlayerTeleportEvent event) {
		// Sometimes TO is NULL, this fixes that. After that check the teleport permission is enforced.
		if (event.getTo() == null || event.getTo().getWorld() == null || !handleTeleportPermission(event.getPlayer(), event.getTo())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleportMsg(PlayerTeleportEvent event) {
		if (lastEnteredPortal != null) {
			// Show the portal name
			Localization.PORTAL_ENTER.message(event.getPlayer(), lastEnteredPortal.getDestinationDisplayName());
		} else if (event.getTo().getWorld() != event.getPlayer().getWorld()) {
			// Show world enter message
			Localization.WORLD_ENTER.message(event.getPlayer(), event.getTo().getWorld().getName());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.useInteractedBlock() == Result.DENY || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (MaterialUtil.ISBUCKET.get(event.getItem())) {
			if (!Permission.canBuild(event.getPlayer())) {
				Localization.WORLD_NOBUILD.message(event.getPlayer());
				event.setUseInteractedBlock(Result.DENY);
			}
		} else if (MaterialUtil.ISINTERACTABLE.get(event.getClickedBlock())) {
			if (!Permission.canUse(event.getPlayer())) {
				Localization.WORLD_NOUSE.message(event.getPlayer());
				event.setUseInteractedBlock(Result.DENY);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!Permission.canBuild(event.getPlayer())) {
			Localization.WORLD_NOBREAK.message(event.getPlayer());
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (event.canBuild()) {
			if (!Permission.canBuild(event.getPlayer())) {
				Localization.WORLD_NOBUILD.message(event.getPlayer());
				event.setBuild(false);
			}
		}
	}
}

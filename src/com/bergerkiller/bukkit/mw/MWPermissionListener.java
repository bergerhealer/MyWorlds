package com.bergerkiller.bukkit.mw;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.bergerkiller.bukkit.common.utils.BlockUtil;

/**
 * Handles events to manage plugin-defined permissions and messages
 */
public class MWPermissionListener implements Listener {
	public static Portal lastPortal = null; // Last portal used by a player

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleportMsg(PlayerTeleportEvent event) {
		if (!event.isCancelled()) {
			if (lastPortal != null) {
				// Show portal enter message
				Localization.PORTAL_ENTER.message(event.getPlayer(), lastPortal.getDestinationDisplayName());
			} else if (event.getTo().getWorld() != event.getPlayer().getWorld()) {
				// Show world enter message
				Localization.WORLD_ENTER.message(event.getPlayer(), event.getTo().getWorld().getName());
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!event.isCancelled() && event.getTo() != null && event.getTo().getWorld() != null) {
			if (Permission.canEnter(event.getPlayer(), event.getTo().getWorld())) {

			} else {
				Localization.WORLD_NOACCESS.message(event.getPlayer());
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleportPerm(PlayerTeleportEvent event) {
		if (event.getTo() == null || event.getTo().getWorld() == null) {
			// Special check to prevent a lot of bugs
			event.setCancelled(true);
		}
		if (!event.isCancelled()) {
			// World can be entered?
			if (Permission.canEnter(event.getPlayer(), event.getTo().getWorld())) {
				// If applicable, Portal can be entered?
				if (lastPortal != null && !Permission.canEnter(event.getPlayer(), lastPortal)) {
					Localization.PORTAL_NOACCESS.message(event.getPlayer());
					event.setCancelled(true);
				}
			} else {
				Localization.WORLD_NOACCESS.message(event.getPlayer());
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled() || event.useInteractedBlock() == Result.DENY) {
			return;
		}
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.hasItem() && BlockUtil.isBucket(event.getItem().getTypeId())) {
				if (!Permission.canBuild(event.getPlayer())) {
					Localization.WORLD_NOBUILD.message(event.getPlayer());
					event.setUseInteractedBlock(Result.DENY);
				}
			} else if (BlockUtil.isInteractable(event.getClickedBlock().getTypeId())) {
				if (!Permission.canUse(event.getPlayer())) {
					Localization.WORLD_NOUSE.message(event.getPlayer());
					event.setUseInteractedBlock(Result.DENY);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.isCancelled()) {
			if (!Permission.canBuild(event.getPlayer())) {
				Localization.WORLD_NOBREAK.message(event.getPlayer());
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!event.isCancelled() && event.canBuild()) {
			if (!Permission.canBuild(event.getPlayer())) {
				Localization.WORLD_NOBUILD.message(event.getPlayer());
				event.setBuild(false);
			}
		}
	}
}

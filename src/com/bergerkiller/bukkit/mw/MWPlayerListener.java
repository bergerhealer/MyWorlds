package com.bergerkiller.bukkit.mw;

import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MWPlayerListener extends PlayerListener {
	
	public static boolean isSolid(Block b, BlockFace direction) {
		int maxwidth = 10;
		while (true) {
			int id = b.getTypeId();
			if (id == 0) return false;
			if (id != 9 && id != 8) return true;
			b = b.getRelative(direction);
			--maxwidth;
			if (maxwidth <= 0) return false;
		}
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
		if (event.useInteractedBlock() == Result.DENY) return;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Material type = event.getClickedBlock().getType();
			switch (type) {
			case LEVER : break;
			case WOODEN_DOOR : break;
			case IRON_DOOR : break;
			case TRAP_DOOR : break;
			case CHEST : break;
			case FURNACE : break;
			case BURNING_FURNACE : break;
			case DISPENSER : break;
			case WORKBENCH : break;
			case DIODE_BLOCK_ON : break;
			case DIODE_BLOCK_OFF : break;	
			case BED : break;
			case CAKE : break;
			case NOTE_BLOCK : break;
			case JUKEBOX : break;
			default : return;
			}
			if (!Permission.canUse(event.getPlayer())) {
				event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to use this in this world!");
				event.setUseInteractedBlock(Result.DENY);
			}
		}
	}
	
	@Override
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!event.isCancelled()) {
			if (event.getFrom().getWorld() != event.getTo().getWorld()) {
				WorldConfig.get(event.getTo()).update(event.getPlayer());
			}
		}
	}
	
	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		WorldConfig.get(event.getPlayer()).update(event.getPlayer());
	}
	
	@Override
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (!event.isBedSpawn()) {
			Location loc = WorldManager.getRespawnLocation(event.getPlayer().getWorld());
			if (loc != null) {
				event.setRespawnLocation(loc);
			}
		}
		WorldConfig.get(event.getRespawnLocation()).update(event.getPlayer());
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		WorldConfig.updateReload(event.getPlayer());
	}
	
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!event.isCancelled()) {
			Block b = event.getTo().getBlock();
			if (MyWorlds.useWaterTeleport && b.getTypeId() == 9) {
				if (b.getRelative(BlockFace.UP).getTypeId() == 9 ||
						b.getRelative(BlockFace.DOWN).getTypeId() == 9) {
					boolean allow = false;
					if (b.getRelative(BlockFace.NORTH).getType() == Material.AIR ||
							b.getRelative(BlockFace.SOUTH).getType() == Material.AIR) {
						if (isSolid(b, BlockFace.WEST) && isSolid(b, BlockFace.EAST)) {
							allow = true;
						}
					} else if (b.getRelative(BlockFace.EAST).getType() == Material.AIR ||
							b.getRelative(BlockFace.WEST).getType() == Material.AIR) {
						if (isSolid(b, BlockFace.NORTH) && isSolid(b, BlockFace.SOUTH)) {
							allow = true;
						}
					}
					if (allow)
						Portal.handlePortalEnter(event.getPlayer());
				}
			}
			if (event.getFrom().getWorld() != event.getTo().getWorld()) {
				WorldConfig.updateReload(event.getFrom());
			}
		}
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		if (!Permission.canChat(event.getPlayer())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(Localization.get("world.nochataccess"));
			return;
		}
		Iterator<Player> iterator = event.getRecipients().iterator();
		while (iterator.hasNext()) {
			if (!Permission.canChat(event.getPlayer(), iterator.next())) {
				iterator.remove();
			}
		}
	}

	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		WorldConfig.updateReload(event.getFrom());
	}
	
}

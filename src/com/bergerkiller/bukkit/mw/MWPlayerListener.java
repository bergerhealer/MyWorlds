package com.bergerkiller.bukkit.mw;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
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
			Location loc = WorldManager.getSpawnLocation(event.getPlayer().getWorld().getName());
			if (loc != null) {
				event.setRespawnLocation(loc);
			}
		}
		WorldConfig.get(event.getRespawnLocation()).update(event.getPlayer());
	}
	
	@Override
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!event.isCancelled()) {
			Block b = event.getTo().getBlock();
			if (MyWorlds.useWaterTeleport.get() && b.getTypeId() == 9) {
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
		}
	}
}

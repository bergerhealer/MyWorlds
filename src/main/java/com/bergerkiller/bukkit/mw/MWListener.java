package com.bergerkiller.bukkit.mw;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.bergerkiller.bukkit.common.collections.EntityMap;
import com.bergerkiller.bukkit.common.conversion.Conversion;
import com.bergerkiller.bukkit.common.internal.MobPreSpawnListener;
import com.bergerkiller.bukkit.common.reflection.classes.EntityRef;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class MWListener implements Listener, MobPreSpawnListener {
	// World to disable keepspawnloaded for
	private static HashSet<String> initIgnoreWorlds = new HashSet<String>();
	// A mapping of player positions to prevent spammed portal teleportation
	private static EntityMap<Player, Location> walkDistanceCheckMap = new EntityMap<Player, Location>();
	// A mapping of player positions to store the actually entered portal
	private static EntityMap<Player, Location> playerPortalEnter = new EntityMap<Player, Location>();
	// Portal times for a minimal delay
    private static EntityMap<Entity, Long> portaltimes = new EntityMap<Entity, Long>();
    // Whether weather changes handling is ignored
	public static boolean ignoreWeatherChanges = false;

	public static void ignoreWorld(String worldname) {
		initIgnoreWorlds.add(worldname);
	}

	/**
	 * Handles the teleport delay and distance checks
	 * 
	 * @param e Entity to pre-teleport
	 * @param portalMaterial of the portal
	 * @return True if teleporting happened, False if not
	 */
	public static boolean doPortalTeleport(Entity e, Material portalMaterial) {
    	if (walkDistanceCheckMap.containsKey(e)) {
    		return false;
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
        	return Portal.handlePortalEnter(e, portalMaterial);
        } else {
        	return false;
        }
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldLoad(WorldLoadEvent event) {
		WorldConfig.get(event.getWorld()).timeControl.updateWorld(event.getWorld());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		WorldConfig config = WorldConfig.get(event.getWorld());
		config.timeControl.updateWorld(null);
		WorldManager.closeWorldStreams(event.getWorld());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onWorldInit(WorldInitEvent event) {
		if (initIgnoreWorlds.remove(event.getWorld().getName())) {
			WorldUtil.setKeepSpawnInMemory(event.getWorld(), false);
		} else {
			WorldConfig.get(event.getWorld()).update(event.getWorld());
		}
	}

	public static void setWeather(org.bukkit.World w, boolean storm) {
		ignoreWeatherChanges = true;
		w.setStorm(storm);
		ignoreWeatherChanges = false;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onWeatherChange(WeatherChangeEvent event) {
		if (!ignoreWeatherChanges && WorldConfig.get(event.getWorld()).holdWeather) {
			event.setCancelled(true);
		} else {
			WorldConfig.get(event.getWorld()).updateSpoutWeather(event.getWorld());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		WorldConfig.get(event.getPlayer()).update(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		org.bukkit.World respawnWorld = event.getPlayer().getWorld();
		if (MyWorlds.forceMainWorldSpawn) {
			// Force a respawn on the main world
			respawnWorld = MyWorlds.getMainWorld();
		} else if (event.isBedSpawn() && !WorldConfig.get(event.getPlayer()).forcedRespawn) {
			respawnWorld = null; // Ignore bed spawns that are not overrided
		}
		if (respawnWorld != null) {
			Location loc = WorldManager.getRespawnLocation(respawnWorld);
			if (loc != null) {
				event.setRespawnLocation(loc);
			}
		}
		WorldConfig.get(event.getRespawnLocation()).update(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		WorldConfig.updateReload(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		// Handle player movement for portals
		Location loc = walkDistanceCheckMap.get(event.getPlayer());
		if (loc != null) {
			if (loc.getWorld() != event.getTo().getWorld()) {
				// Put in proper world
				walkDistanceCheckMap.put(event.getPlayer(), event.getTo());
			} else if (loc.distanceSquared(event.getTo()) > 2.25) {
				// Moved outside radius - remove point
				walkDistanceCheckMap.remove(event.getPlayer());
			}
		}
		// Water teleport handling
		Block b = event.getTo().getBlock();
		final int statid = Material.STATIONARY_WATER.getId(); // = 9
		if (MyWorlds.useWaterTeleport && b.getTypeId() == statid) {
			if (b.getRelative(BlockFace.UP).getTypeId() == statid || b.getRelative(BlockFace.DOWN).getTypeId() == statid) {
				boolean allow = false;
				if (b.getRelative(BlockFace.NORTH).getType() == Material.AIR || b.getRelative(BlockFace.SOUTH).getType() == Material.AIR) {
					if (Util.isSolid(b, BlockFace.WEST) && Util.isSolid(b, BlockFace.EAST)) {
						allow = true;
					}
				} else if (b.getRelative(BlockFace.EAST).getType() == Material.AIR || b.getRelative(BlockFace.WEST).getType() == Material.AIR) {
					if (Util.isSolid(b, BlockFace.NORTH) && Util.isSolid(b, BlockFace.SOUTH)) {
						allow = true;
					}
				}
				if (allow) {
					doPortalTeleport(event.getPlayer(), Material.STATIONARY_WATER);
				}
			}
		}
		if (event.getFrom().getWorld() != event.getTo().getWorld()) {
			WorldConfig.updateReload(event.getFrom());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		walkDistanceCheckMap.put(event.getPlayer(), event.getTo());
		portaltimes.put(event.getPlayer(), System.currentTimeMillis());
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		final boolean nether = event.getCause() == TeleportCause.NETHER_PORTAL;
		final boolean end = event.getCause() == TeleportCause.END_PORTAL;
		if (!nether && !end) {
			return; // Ignore alternative types
		}
		// Cancel the internal logic
		event.setCancelled(true);

		// Get from location
		Location loc = playerPortalEnter.remove(event.getPlayer());
		if (loc == null) {
			loc = event.getFrom();
		}
		Block b = loc.getBlock();

		// Handle player teleportation - portal check
		Material mat = Material.AIR;
		if (nether) {
			mat = Material.PORTAL;
			if (!Util.isNetherPortal(b, true)) {
				return; // Invalid
			}
		} else if (end) {
			mat = Material.ENDER_PORTAL;
			if (!Util.isEndPortal(b, true)) {
				return; // Invalid
			}
		}
		// Perform teleportation
		doPortalTeleport(event.getPlayer(), mat);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityPortalEnter(EntityPortalEnterEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			// Survival?
			if (player.getGameMode() == GameMode.CREATIVE || !MyWorlds.alwaysInstantPortal) {
				// Store the to location - the one in the portal enter event is inaccurate
				playerPortalEnter.put((Player) event.getEntity(), event.getLocation());
				return; // Ignore teleportation
			}
		} else if (MyWorlds.onlyPlayerTeleportation) {
			return; // Ignore
		}
		// Handle teleportation
		Block b = event.getLocation().getBlock();
		if (!Util.isNetherPortal(b, false) && !Util.isEndPortal(b, false)) {
			return;
		}
		doPortalTeleport(event.getEntity(), b.getType());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (!Permission.canChat(event.getPlayer())) {
			event.setCancelled(true);
			Localization.WORLD_NOCHATACCESS.message(event.getPlayer());
			return;
		}
		Iterator<Player> iterator = event.getRecipients().iterator();
		while (iterator.hasNext()) {
			if (!Permission.canChat(event.getPlayer(), iterator.next())) {
				iterator.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
		WorldConfig.updateReload(event.getFrom());
		WorldConfig.get(event.getPlayer()).update(event.getPlayer());
		// Execute it again the next tick to ensure changes happened
		CommonUtil.nextTick(new Runnable() {
			public void run() {
				WorldConfig.get(event.getPlayer()).update(event.getPlayer());
			}
		});
		if (MyWorlds.useWorldInventories && !Permission.GENERAL_KEEPINV.has(event.getPlayer())) {
			Object playerHandle = Conversion.toEntityHandle.convert(event.getPlayer());
			org.bukkit.World newWorld = EntityRef.world.get(playerHandle);
			EntityRef.world.set(playerHandle, event.getFrom());
			CommonUtil.savePlayer(event.getPlayer());
			EntityRef.world.set(playerHandle, newWorld);
			MWPlayerDataController.refreshState(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() != SpawnReason.CUSTOM && (!MyWorlds.ignoreEggSpawns || event.getSpawnReason() != SpawnReason.EGG)) {
			if (WorldConfig.get(event.getEntity()).spawnControl.isDenied(event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}

	@Override
	public boolean canSpawn(World world, int x, int y, int z, EntityType entityType) {
		return !WorldConfig.get(world).spawnControl.isDenied(entityType);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockForm(BlockFormEvent event) {
		Material type = event.getNewState().getType();
		if (type == Material.SNOW) {
			if (!WorldConfig.get(event.getBlock()).formSnow) {
				event.setCancelled(true);
			}
		} else if (type == Material.ICE) {
			if (!WorldConfig.get(event.getBlock()).formIce) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.getBlock().getType() == Material.PORTAL) {
			if (!(event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Portal portal = Portal.get(event.getBlock(), false);
		if (portal != null && portal.remove()) {
			event.getPlayer().sendMessage(ChatColor.RED + "You removed portal " + ChatColor.WHITE + portal.getName() + ChatColor.RED + "!");
			MyWorlds.plugin.logAction(event.getPlayer(), "Removed portal '" + portal.getName() + "'!");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		Portal portal = Portal.get(event.getBlock(), event.getLines());
		if (portal != null) {
			if (Permission.PORTAL_CREATE.has(event.getPlayer())) {
				if (Portal.exists(event.getPlayer().getWorld().getName(), portal.getName())) {
					if (!MyWorlds.allowPortalNameOverride || !Permission.PORTAL_OVERRIDE.has(event.getPlayer())) {
						event.getPlayer().sendMessage(ChatColor.RED + "This portal name is already used!");
						event.setCancelled(true);
						return;
					}
				}
				portal.add();
				MyWorlds.plugin.logAction(event.getPlayer(), "Created a new portal: '" + portal.getName() + "'!");
				if (portal.hasDestination()) {
					event.getPlayer().sendMessage(ChatColor.GREEN + "You created a new portal to " + ChatColor.WHITE + portal.getDestinationName() + ChatColor.GREEN + "!");
				} else {
					event.getPlayer().sendMessage(ChatColor.GREEN + "You created a new destination portal!");
				}
			} else {
				event.setCancelled(true);
			}
		}
	}
}

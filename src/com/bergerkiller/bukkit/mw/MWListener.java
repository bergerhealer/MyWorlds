package com.bergerkiller.bukkit.mw;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class MWListener implements Listener {

	
	private static HashSet<String> initIgnoreWorlds = new HashSet<String>();
	public static void ignoreWorld(String worldname) {
		initIgnoreWorlds.add(worldname);
	}
    
	@EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
    	WorldConfig.get(event.getWorld()).timeControl.updateWorld(event.getWorld());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
    	if (!event.isCancelled()) {
    		WorldConfig config = WorldConfig.get(event.getWorld());
    		config.timeControl.updateWorld(null);
        	WorldManager.clearWorldReference(event.getWorld());
        	for (Player player : event.getWorld().getPlayers()) {
        		config.remove(player);
        	}
    	}
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldInit(WorldInitEvent event) {
    	if (initIgnoreWorlds.remove(event.getWorld().getName())) {
    		event.getWorld().setKeepSpawnInMemory(false);
    	} else {
    		WorldConfig.get(event.getWorld()).update(event.getWorld());
    	}
    }
	
	public static void setWeather(World w, boolean storm) {
		ignoreWeatherChanges = true;
		w.setStorm(storm);
		ignoreWeatherChanges = false;
	}
	
	public static boolean ignoreWeatherChanges = false;
	
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onWeatherChange(WeatherChangeEvent event) {
		if (!ignoreWeatherChanges && WorldConfig.get(event.getWorld()).holdWeather) {
			event.setCancelled(true);
		} else {
			WorldConfig.get(event.getWorld()).updateSpoutWeather(event.getWorld());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
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
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (!event.isCancelled()) {
			if (MyWorlds.useAllTeleportPermissions) {
				if (!Permission.canEnter(event.getPlayer(), event.getTo().getWorld())) {
					Localization.message(event.getPlayer(), "world.noaccess");
					event.setCancelled(true);
				}
			}
			if (event.getFrom().getWorld() != event.getTo().getWorld()) {
				WorldConfig.get(event.getFrom()).remove(event.getPlayer());
				WorldConfig.get(event.getTo()).update(event.getPlayer());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		//Is this a new player? Check if the player has a settings file
		if (!WorldManager.getPlayerDataFile(event.getPlayer()).exists()) {
			//fix to use the default world to spawn instead
			event.getPlayer().teleport(WorldManager.getSpawnLocation(event.getPlayer().getWorld()));
		}
		WorldConfig.get(event.getPlayer()).update(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (!event.isBedSpawn()) {
			Location loc = WorldManager.getRespawnLocation(event.getPlayer().getWorld());
			if (loc != null) {
				event.setRespawnLocation(loc);
			}
		}
		if (event.getRespawnLocation().getWorld() != event.getPlayer().getWorld()) {
			WorldConfig.get(event.getPlayer()).remove(event.getPlayer());
			WorldConfig.get(event.getRespawnLocation()).update(event.getPlayer());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		WorldConfig.get(event.getPlayer()).remove(event.getPlayer()); 
		WorldConfig.updateReload(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!event.isCancelled()) {
			Portal.handlePlayerMove(event.getPlayer(), event.getTo());
			Block b = event.getTo().getBlock();
			if (MyWorlds.useWaterTeleport && b.getTypeId() == 9) {
				if (b.getRelative(BlockFace.UP).getTypeId() == 9 ||
						b.getRelative(BlockFace.DOWN).getTypeId() == 9) {
					boolean allow = false;
					if (b.getRelative(BlockFace.NORTH).getType() == Material.AIR ||
							b.getRelative(BlockFace.SOUTH).getType() == Material.AIR) {
						if (Util.isSolid(b, BlockFace.WEST) && Util.isSolid(b, BlockFace.EAST)) {
							allow = true;
						}
					} else if (b.getRelative(BlockFace.EAST).getType() == Material.AIR ||
							b.getRelative(BlockFace.WEST).getType() == Material.AIR) {
						if (Util.isSolid(b, BlockFace.NORTH) && Util.isSolid(b, BlockFace.SOUTH)) {
							allow = true;
						}
					}
					if (allow)
						Portal.handlePortalEnter(event.getPlayer());
				}
			}
			if (event.getFrom().getWorld() != event.getTo().getWorld()) {
				WorldConfig.get(event.getFrom()).remove(event.getPlayer());
				WorldConfig.updateReload(event.getFrom());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
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

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		WorldConfig.updateReload(event.getFrom());
		WorldConfig.get(event.getFrom()).remove(event.getPlayer());
		WorldConfig.get(event.getPlayer()).update(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
		if (MyWorlds.onlyPlayerTeleportation) {
			if (!(event.getEntity() instanceof Player)) {
				return;
			}
		}
    	if (MyWorlds.onlyObsidianPortals) {
    		Block b = event.getLocation().getBlock();
    		if (b.getType() == Material.PORTAL) {
    			if (!Portal.isPortal(b)) return;
    		}
    	}
    	Portal.handlePortalEnter(event.getEntity());
    }
    
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
    	if (!event.isCancelled()) {
        	if (event.getSpawnReason() != SpawnReason.CUSTOM) {
        		if (WorldConfig.get(event.getEntity()).spawnControl.isDenied(event.getEntity())) {
        			event.setCancelled(true);
        		}
        	}
    	}
    }
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockForm(BlockFormEvent event) {
		if (event.isCancelled()) return;
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
	
	@EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPhysics(BlockPhysicsEvent event) {
    	if (!event.isCancelled()) {
        	if (event.getBlock().getType() == Material.PORTAL) {
        		if (!(event.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)) {
        			event.setCancelled(true);
        		}
        	}
    	}
    }
    
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {    
    	if (!event.isCancelled()) {
    		if (Permission.canBuild(event.getPlayer())) {
            	Portal portal = Portal.get(event.getBlock(), false);
            	if (portal != null && portal.remove()) {
            		event.getPlayer().sendMessage(ChatColor.RED + "You removed portal " + ChatColor.WHITE + portal.getName() + ChatColor.RED + "!");
            		Util.notifyConsole(event.getPlayer(), "Removed portal '" + portal.getName() + "'!");
            	}
    		} else {
    			event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to break blocks in this world!");
    			event.setCancelled(true);
    		}
    	}
    }
    
	@EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
    	if (!event.isCancelled() && event.canBuild()) {
    		if (!Permission.canBuild(event.getPlayer())) {
    			event.getPlayer().sendMessage(ChatColor.RED + "You are not allowed to place blocks in this world!");
    			event.setBuild(false);
    		}
    	}
    }
    
	public static float getAngleDifference(float angle1, float angle2) {
		float difference = angle1 - angle2;
        while (difference < -180) difference += 360;
        while (difference > 180) difference -= 360;
        return Math.abs(difference);
	}
            
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
    	if (!event.isCancelled()) {
        	Portal portal = Portal.get(event.getBlock(), event.getLines());
    		if (portal != null) {
    			if (Permission.has(event.getPlayer(), "portal.create")) {
    				if (Portal.exists(event.getPlayer().getWorld().getName(), portal.getName())) {
    					if (!MyWorlds.allowPortalNameOverride || !Permission.has(event.getPlayer(), "portal.override")) {
    						event.getPlayer().sendMessage(ChatColor.RED + "This portal name is already used!");
    						event.setCancelled(true);
    						return;
    					}
    				}
    				portal.add();
    				Util.notifyConsole(event.getPlayer(), "Created a new portal: '" + portal.getName() + "'!");
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
    
}

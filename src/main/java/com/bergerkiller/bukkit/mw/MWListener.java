package com.bergerkiller.bukkit.mw;

import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.events.CreaturePreSpawnEvent;
import com.bergerkiller.bukkit.common.server.MCPCPlusServer;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;

public class MWListener implements Listener {
    // Keeps track of player teleports
    private final TeleportationTracker teleportTracker = new TeleportationTracker();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        WorldConfig.get(event.getWorld()).onWorldUnload(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldInit(WorldInitEvent event) {
        if (MyWorlds.plugin.clearInitDisableSpawn(event.getWorld().getName())) {
            WorldUtil.setKeepSpawnInMemory(event.getWorld(), false);
        } else {
            WorldConfig.get(event.getWorld()).onWorldLoad(event.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        WorldConfig.get(event.getPlayer()).onPlayerEnter(event.getPlayer());
    }

    private static boolean checkRespawnValid(PlayerRespawnEvent event) {
        // Bukkit bug since MC 1.14: respawn event also occurs when players teleport back to the main world from the end
        if (Common.evaluateMCVersion(">=", "1.14")) {
            return event.getPlayer().getHealth() <= 0.0;
        }

        // Only occurred during an actual respawn on earlier versions
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawnMonitor(PlayerRespawnEvent event) {
        if (!checkRespawnValid(event)) {
            // Save inventory before it is reloaded again
            MyWorlds.plugin.getPlayerDataController().onSave(event.getPlayer());
            return;
        }

        WorldConfig.get(event.getPlayer()).onRespawn(event.getPlayer(), event.getRespawnLocation());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!checkRespawnValid(event)) {
            return;
        }

        // Update spawn position based on world configuration
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
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Since there is no intermediary 'new world' to go to, 
        // both methods fire simultaneously after another.
        WorldConfig config = WorldConfig.get(event.getPlayer());
        config.onPlayerLeave(event.getPlayer(), true);
        config.onPlayerLeft(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Handle player movement for portals
        teleportTracker.updatePlayerPosition(event.getPlayer(), event.getTo());

        // Water teleportation handling
        if (MyWorlds.waterPortalEnabled) {
            Block b = event.getTo().getBlock();
            if (Util.isWaterPortal(b)) {
                boolean canTeleport = teleportTracker.canTeleport(event.getPlayer());
                teleportTracker.setPortalPoint(event.getPlayer(), event.getTo());
                if (canTeleport) {
                    // Just like non-delay teleporting, schedule a task
                    handlePortalEnterNextTick(event.getPlayer(), event.getTo());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        teleportTracker.setPortalPoint(event.getPlayer(), event.getTo());
        // This occurs right before we move to a new world.
        // If this is a world change, it is time to save the old information!
        if (event.getTo().getWorld() != event.getPlayer().getWorld()) {
            WorldConfig.get(event.getPlayer()).onPlayerLeave(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerFoodChange(FoodLevelChangeEvent event) {
        event.setCancelled(!WorldConfig.get(event.getEntity()).allowHunger);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
        Block bed = event.getBed();
        if (bed == null) {
            return;
        }
        final WorldConfig config = WorldConfig.get(bed.getWorld());
        if (!config.bedRespawnEnabled) {
            final Player player = event.getPlayer();
            CommonUtil.nextTick(new Runnable() {
                @Override
                public void run() {
                    config.updateBedSpawnPoint(player);
                }
            });
        }
    }

    public void handlePortalEnterNextTick(Player player, Location from) {
        // Handle teleportation the next tick
        final EntityPortalEvent portalEvent = Util.createEntityPortalEvent(player, from);
        CommonUtil.nextTick(new Runnable() {
            public void run() {
                handlePortalEnter(portalEvent, false);
                if (portalEvent.getTo() != null && !portalEvent.isCancelled()) {
                    // Use a travel agent if needed
                    Location to = portalEvent.getTo();
                    if (Util.useTravelAgent(portalEvent)) {
                        to = WorldUtil.findSpawnLocation(to);
                    }
                    // This tends to happen with Bukkit logic...haha
                    if (to == null) {
                        return;
                    }

                    // Note: We could use EntityUtil.teleport here...
                    // But why even bother, we would only add strange differences between teleports
                    portalEvent.getEntity().teleport(to);
                }
            }
        });
    }

    public void handlePortalEnter(EntityPortalEvent event, boolean doTeleportCheck) {
        final Entity entity = event.getEntity();

        // By default, cancel the event until we know it works
        event.setCancelled(true);

        // Initial check for non-player entities
        if (MyWorlds.onlyPlayerTeleportation && !(entity instanceof Player)) {
            return;
        }

        // Get from location
        Location enterLoc = event.getFrom();

        // Handle player teleportation - portal check
        PortalType type = Util.findPortalType(enterLoc.getWorld(), enterLoc.getBlockX(), enterLoc.getBlockY(), enterLoc.getBlockZ());
        if (type == null) {
            event.setCancelled(false); // MyWorlds doesn't handle this event
            return;
        }

        // Player looped teleportation check (since they allow looped teleports)
        if (entity instanceof Player) {
            Player player = (Player) entity;
            boolean canTeleport = !doTeleportCheck || teleportTracker.canTeleport((Player) entity);
            teleportTracker.setPortalPoint(player, enterLoc);
            if (!canTeleport) {
                return;
            }
        }

        // For further processing Portal-specific logic is needed
        if (Portal.handlePortalEnter(event, type) && event.getTo() != null) {
            // Send a preparation message if teleporting to non-generated areas
            if (entity instanceof Player) {
                Location to = event.getTo();
                int chunkX = MathUtil.toChunk(to.getX());
                int chunkZ = MathUtil.toChunk(to.getZ());
                boolean available = true;
                for (int dx = -4; dx <= 4 && available; dx++) {
                    for (int dz = -4; dz <= 4 && available; dz++) {
                        available &= WorldUtil.isChunkAvailable(to.getWorld(), chunkX + dx, chunkZ + dz);
                    }
                }
                if (!available) {
                    Localization.PORTAL_PREPARING.message((Player) entity);
                }
            }
            // Undo the cancellation done earlier
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPortalMonitor(PlayerPortalEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getTo().getWorld() != event.getPlayer().getWorld()) {
            WorldConfig.get(event.getPlayer()).onPlayerLeave(event.getPlayer(), false);
            teleportTracker.setPortalPoint(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (!MyWorlds.endPortalEnabled && !MyWorlds.netherPortalEnabled) {
            return;
        }
        //Note! Event.getTo() can be null! Account for it!

        // Wrap inside an Entity portal event
        EntityPortalEvent entityEvent = Util.createEntityPortalEvent(event.getPlayer(), event.getFrom());
        entityEvent.setTo(event.getTo());
        Util.copyTravelAgent(event, entityEvent);
        handlePortalEnter(entityEvent, true);
        // Now, apply them again
        if (entityEvent.getTo() != null) {
            event.setTo(entityEvent.getTo());
        }
        Util.copyTravelAgent(entityEvent, event);
        event.setCancelled(entityEvent.isCancelled());

        // For ender portals we NEED to teleport away from the end world
        // Not doing so results in the player getting glitched
        // Yes, this is another thing Bukkit needs to fix...
        World fromWorld = event.getFrom().getWorld();
        if (fromWorld.getEnvironment() != Environment.THE_END) {
            return;
        }
        if (!event.isCancelled() && event.getTo() != null && event.getTo().getWorld() != fromWorld) {
            return;
        }

        // Identify the from world as being part of the main worlds
        World mainWorld = WorldUtil.getWorlds().iterator().next();
        final String defaultEnderWorldName;
        if (Common.SERVER instanceof MCPCPlusServer) {
            defaultEnderWorldName = "DIM-1";
        } else {
            defaultEnderWorldName = mainWorld.getName() + "_the_end";
        }
        if (!fromWorld.getName().equalsIgnoreCase(defaultEnderWorldName)) {
            return;
        }

        // Entered a portal on the main the_end world, which would cause credits to show
        final Player player = event.getPlayer();
        final Location safeSpawn = WorldManager.getSafeSpawn(event.getFrom(), false);

        // Set a dummy destination to snap the player out of the glitch
        if (Util.hasTravelAgentField) {
            event.useTravelAgent(false);
        }
        event.setCancelled(false);
        event.setTo(mainWorld.getSpawnLocation());

        // Teleport to the main world and back
        CommonUtil.nextTick(new Runnable() {
            @Override
            public void run() {
                player.teleport(safeSpawn);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (MyWorlds.endPortalEnabled || MyWorlds.netherPortalEnabled) {
            handlePortalEnter(event, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof Player) || (!MyWorlds.endPortalEnabled && !MyWorlds.netherPortalEnabled)) {
            return;
        }
        Player player = (Player) event.getEntity();

        // If player is creative, we can instantly handle the teleport in PLAYER_PORTAL_ENTER
        // If not but the delayed teleportation is preferred, then also let PLAYER_PORTAL_ENTER handle it
        // If not, then we will handle the teleportation in here
        if (PlayerUtil.isInvulnerable(player) || !MyWorlds.alwaysInstantPortal) {
            return;
        }

        // This event fires every tick...better try and avoid spammed tasks
        boolean canTeleport = teleportTracker.canTeleport(player);
        teleportTracker.setPortalPoint(player, event.getLocation());
        if (canTeleport) {
            this.handlePortalEnterNextTick(player, event.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Handle chat permissions
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
        WorldConfig.get(event.getFrom()).onPlayerLeft(event.getPlayer());
        WorldConfig.get(event.getPlayer()).onPlayerEnter(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Any creature spawns we couldn't cancel in PreSpawn we will cancel here
        if (event.getSpawnReason() != SpawnReason.CUSTOM && (!MyWorlds.ignoreEggSpawns || event.getSpawnReason() != SpawnReason.SPAWNER_EGG)) {
            if (WorldConfig.get(event.getEntity()).spawnControl.isDenied(event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreaturePreSpawn(CreaturePreSpawnEvent event) {
        // Cancel creature spawns before entities are spawned
        if (WorldConfig.get(event.getSpawnLocation()).spawnControl.isDenied(event.getEntityType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        // Snow/Ice forming cancelling based on world settings
        BlockData data = BlockData.fromBlockState(event.getNewState());
        if (Util.IS_SNOW.get(data)) {
            if (!WorldConfig.get(event.getBlock()).formSnow) {
                event.setCancelled(true);
            }
        } else if (Util.IS_ICE.get(data)) {
            if (!WorldConfig.get(event.getBlock()).formIce) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        // Allows portals to be placed without physics killing it
        if (MyWorlds.overridePortalPhysics && Util.IS_NETHER_PORTAL.get(event.getBlock())) {
            event.setCancelled(true);
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

    /* Handle placement of MyWorlds portals in a special way */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        PortalItemType type = PortalItemType.fromItem(item);
        if (type != null) {
            event.setCancelled(true);
            final Block b = event.getBlock();
            final BlockData d = type.getPlacedData(event.getPlayer().getEyeLocation().getYaw());

            CommonUtil.nextTick(new Runnable() {
                @Override
                public void run() {
                    WorldUtil.setBlockDataFast(b, d);
                    WorldUtil.queueBlockSend(b);
                }
            });
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
                // Build message
                if (portal.getDestinationName() != null) {
                    Localization.PORTAL_CREATE_TO.message(event.getPlayer(), portal.getDestinationName());
                    if (!portal.hasDestination()) {
                        Localization.PORTAL_CREATE_MISSING.message(event.getPlayer());
                    }
                } else {
                    Localization.PORTAL_CREATE_END.message(event.getPlayer());
                }
            } else {
                event.setCancelled(true);
            }
        }
    }
}

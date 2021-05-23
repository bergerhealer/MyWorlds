package com.bergerkiller.bukkit.mw;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
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
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.common.events.CreaturePreSpawnEvent;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.BlockData;
import com.bergerkiller.bukkit.mw.portal.PortalDestination;
import com.bergerkiller.bukkit.mw.portal.PortalTeleportationHandler;

public class MWListener implements Listener {
    private final MyWorlds plugin;

    public MWListener(MyWorlds plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        WorldConfig.get(event.getWorld()).onWorldUnload(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldInit(WorldInitEvent event) {
        if (MyWorlds.plugin.clearInitDisableSpawn(event.getWorld().getName())) {
            WorldUtil.setKeepSpawnInMemory(event.getWorld(), false);
        }
        WorldConfig.get(event.getWorld()).onWorldLoad(event.getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        WorldConfig.get(event.getPlayer()).onPlayerEnter(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawnMonitor(PlayerRespawnEvent event) {
        if (plugin.getEndRespawnHandler().isDeathRespawn(event)) {
            WorldConfig.get(event.getPlayer()).onRespawn(event.getPlayer(), event.getRespawnLocation());
        } else {
            // Save inventory before it is reloaded again
            // This is important for 1.14 and later to guarantee consistent inventories
            MyWorlds.plugin.getPlayerDataController().onSave(event.getPlayer());
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
        // Water teleportation handling
        if (MyWorlds.waterPortalEnabled) {
            Block b = event.getTo().getBlock();
            if (PortalType.WATER.detect(b)) {
                handlePortalEnter(PortalType.WATER, b, event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        // Decode what kind of portal block this is
        // This might return null for a valid case, because we have restrictions
        Block portalBlock = event.getLocation().getBlock();
        PortalType portalType = PortalType.findPortalType(portalBlock);
        if (portalType == null) {
            return;
        }

        // If entity is a player that isn't bound to a world, then he is being respawned currently
        // For some reason the server spams portal enter events while players sit inside the
        // end portal, viewing the credits. We want none of that!
        if (event.getEntity() instanceof Player) {
            World world = event.getEntity().getWorld();
            UUID uuid = event.getEntity().getUniqueId();
            if (world == null || EntityUtil.getEntity(world, uuid) != event.getEntity()) {
                return;
            }
        }

        // Check for the initial portal enter cooldown for survival players, if active
        // This cooldown is set to non-zero once teleportation would normally commence
        // Creative players are exempt
        // All of this only matters for nether portals
        if (event.getEntity() instanceof Player && !MyWorlds.alwaysInstantPortal && portalType == PortalType.NETHER) {
            Player p = (Player) event.getEntity();
            if (p.getGameMode() != GameMode.CREATIVE && EntityUtil.getPortalCooldown(p) == 0) {
                return;
            }
        }

        // Proceed
        handlePortalEnter(portalType, portalBlock, event.getEntity());
    }

    private void handlePortalEnter(PortalType portalType, Block portalBlock, Entity entity) {
        // Figure out what the destination is of this portal
        PortalDestination.FindResults findResults = PortalDestination.findFromPortal(portalType, portalBlock);
        PortalDestination destination = findResults.getDestination();
        if (!findResults.isAvailable()) {
            return;
        }

        // If entity is a passenger of a vehicle, teleport the vehicle instead
        {
            CommonEntity<?> ce = CommonEntity.get(entity);
            while (ce.getVehicle() != null) {
                ce = CommonEntity.get(ce.getVehicle());
            }
        }

        // If player only and is not a player, disallow
        if (findResults.getDestination().isPlayersOnly() && !(entity instanceof Player)) {
            return;
        }

        // If entity has passenger, and we do not allow it, cancel
        if (!findResults.getDestination().canTeleportMounts() && CommonEntity.get(entity).hasPassenger()) {
            //TODO: Show a message or not?
            return;
        }

        // If portal, check the player can actually enter this portal
        if (findResults.isFromPortal() &&
            entity instanceof Player &&
            !Permission.canEnterPortal((Player) entity, findResults.getPortalName()))
        {
            // Debounce
            if (plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
                Localization.PORTAL_NOACCESS.message((Player) entity);
            }
            return;
        }

        // Setup handler, which performs the teleportations for us
        PortalTeleportationHandler teleportationHandler = destination.getMode().createTeleportationHandler();
        teleportationHandler.setup(plugin, portalType, portalBlock, destination, entity);

        // If the destination leads to a portal sign, let the portal sign deal with it
        {
            Location portalLocation = Portal.getPortalLocation(destination.getName(), portalBlock.getWorld().getName());
            if (portalLocation != null) {
                // Debounce
                if (!plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
                    return;
                }

                // Refers to an actual portal sign on this world or another world
                // For players, setup the display message of the portal destination
                if (entity instanceof Player) {
                    String display = destination.getDisplayName();
                    if (display.isEmpty()) {
                        display = destination.getName();
                    }
                    MWListenerPost.setLastEntered((Player) entity, display);
                }

                // Fix position to be the middle of the block, 1 above
                portalLocation = portalLocation.clone();
                portalLocation.setX((double) portalLocation.getBlockX() + 0.5);
                portalLocation.setY(portalLocation.getY() + 1.0);
                portalLocation.setZ((double) portalLocation.getBlockZ() + 0.5);

                // Handle perms up-front
                if (!Portal.canTeleportEntityTo(entity, portalLocation)) {
                    return;
                }

                // Teleport the player the next tick
                // TODO: Correct entity velocity based on orientation change
                teleportationHandler.scheduleTeleportation(portalLocation);
                return;
            }
        }

        // Destination must be a world name, find out what world
        // If no world can be found, fail, and show a message to players
        World world = WorldManager.getWorld(WorldManager.matchWorld(destination.getName()));
        if (world == null) {
            // Debounce
            if (plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
                if (entity instanceof Player && portalType != PortalType.WATER) {
                    Localization.PORTAL_NODESTINATION.message((Player) entity);
                }
            }
            return;
        }

        // If the entity is a player that has a previously known position on the world,
        // teleport to that position. Do this if configured.
        Location lastPosition;
        if (destination.isTeleportToLastPosition() &&
            entity instanceof Player &&
            (lastPosition = MWPlayerDataController.readLastLocation((Player) entity, world)) != null)
        {
            if (plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
                teleportationHandler.scheduleTeleportation(lastPosition);
            }
            return;
        }

        // Rest is handled by the portal mode
        teleportationHandler.handleWorld(world);
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

    // Spawn reason is incorrect on these versions of Minecraft
    private static final boolean TRADER_SPAWNS_BORKED = Common.evaluateMCVersion(">=", "1.14") && Common.evaluateMCVersion("<=", "1.14.4");

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Any creature spawns we couldn't cancel in PreSpawn we will cancel here
        boolean isCustomSpawn = (event.getSpawnReason() == SpawnReason.CUSTOM);

        // Spawn reason is incorrect on Minecraft 1.14 - 1.14.4 and use Spawn Reason CUSTOM anyway
        if (TRADER_SPAWNS_BORKED && isCustomSpawn) {
            String name = event.getEntityType().name();
            if ("WANDERING_TRADER".equals(name) || "TRADER_LLAMA".equals(name)) {
                isCustomSpawn = false;
            }
        }

        if (!isCustomSpawn && (!MyWorlds.ignoreEggSpawns || event.getSpawnReason() != SpawnReason.SPAWNER_EGG)) {
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
        if (MyWorlds.overridePortalPhysics && MaterialUtil.ISNETHERPORTAL.get(event.getBlock())) {
            // Since we cancelled the event, the portal block is no longer registered
            // Explicitly register the block as a nether portal to fix POI errors.
            WorldUtil.markNetherPortal(event.getBlock());
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
                    if (MaterialUtil.ISNETHERPORTAL.get(b)) {
                        WorldUtil.markNetherPortal(b);
                    }
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

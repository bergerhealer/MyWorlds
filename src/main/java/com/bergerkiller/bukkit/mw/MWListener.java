package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.block.SignSide;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.mw.portal.PortalDestinationDebouncer;
import com.bergerkiller.bukkit.mw.portal.PortalMode;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.SpawnChangeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
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
import com.bergerkiller.bukkit.mw.utils.BlockPhysicsEventDataAccessor;
import com.bergerkiller.mountiplex.reflection.declarations.ClassResolver;
import com.bergerkiller.mountiplex.reflection.declarations.MethodDeclaration;
import com.bergerkiller.mountiplex.reflection.util.FastMethod;

public class MWListener implements Listener {
    private static final Material END_PORTAL_FRAME_TYPE = MaterialUtil.getFirst("END_PORTAL_FRAME", "LEGACY_ENDER_PORTAL_FRAME");
    private final MyWorlds plugin;
    private final Map<Player, List<Consumer<Player>>> pendingPlayerJoinTasks = new HashMap<>();
    private final Set<Player> playersInWater = new HashSet<>();
    private final PortalDestinationDebouncer destinationDebouncer;

    public MWListener(MyWorlds plugin) {
        this.plugin = plugin;
        this.destinationDebouncer = new PortalDestinationDebouncer(plugin);
    }

    /**
     * Schedules a task to be run for when a player joins the server properly, with a tick timeout.
     * Runs immediately if player is already online/logged in.
     *
     * @param player Player
     * @param tickTimeout Tick timeout
     * @param runnable Task to run
     */
    public void scheduleForPlayerJoin(final Player player, int tickTimeout, final Consumer<Player> runnable) {
        if (player.isValid()) {
            runnable.accept(player);
            return;
        }

        synchronized (pendingPlayerJoinTasks) {
            pendingPlayerJoinTasks.computeIfAbsent(player, p -> new ArrayList<>()).add(runnable);
        }
        new Task(plugin) {
            @Override
            public void run() {
                synchronized (pendingPlayerJoinTasks) {
                    List<Consumer<Player>> tasks = pendingPlayerJoinTasks.remove(player);
                    if (tasks != null && tasks.remove(runnable) && !tasks.isEmpty()) {
                        pendingPlayerJoinTasks.put(player, tasks);
                    }
                }
            }
        }.start(tickTimeout);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        WorldConfig.get(event.getWorld()).onWorldUnload(event.getWorld(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldInit(WorldInitEvent event) {
        if (MyWorlds.plugin.clearInitDisableSpawn(event.getWorld().getName())) {
            WorldUtil.setKeepSpawnInMemory(event.getWorld(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        WorldConfig.get(event.getWorld()).onWorldLoad(event.getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        WorldConfig wc = WorldConfig.get(event.getPlayer());
        wc.onPlayerEnter(event.getPlayer(), true);

        // Run actions for player join scheduled earlier
        synchronized (pendingPlayerJoinTasks) {
            List<Consumer<Player>> pendingTasks = pendingPlayerJoinTasks.remove(event.getPlayer());
            if (pendingTasks != null) {
                for (Consumer<Player> task : pendingTasks) {
                    task.accept(event.getPlayer());
                }
            }
        }

        // If limit is reached, teleport the player to the main world one tick delayed
        // Skip if the player is already on a main world (wut?)
        if (!wc.checkPlayerLimit(event.getPlayer()) && wc != WorldConfig.getMain()) {
            CommonUtil.nextTick(() -> {
                if (!event.getPlayer().isValid() || wc.checkPlayerLimit(event.getPlayer())) {
                    return;
                }

                Localization.WORLD_FULL.message(event.getPlayer(), wc.worldname);
                event.getPlayer().teleport(WorldConfig.getMain().getSpawnLocation());
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawnMonitor(PlayerRespawnEvent event) {
        if (plugin.getEndRespawnHandler().isEndPortalRespawn(event)) {
            // Save inventory before it is reloaded again
            // This is important for 1.14 and later to guarantee consistent inventories
        } else {
            WorldConfig.get(event.getPlayer()).onRespawn(event.getPlayer(), event.getRespawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Since there is no intermediary 'new world' to go to, 
        // both methods fire simultaneously after another.
        WorldConfig config = WorldConfig.get(event.getPlayer());
        config.onPlayerLeave(event.getPlayer(), true);
        config.onPlayerLeft(event.getPlayer());
        // Cleanup
        playersInWater.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Water teleportation handling
        if (MyWorlds.waterPortalEnabled) {
            Block b = event.getTo().getBlock();
            boolean handleEnter = false;
            if (MyWorlds.waterPortalStrict) {
                // Must be inside a portal
                handleEnter = PortalType.WATER.detect(b);
            } else {
                // Also allow transition air -> water to trigger them
                boolean inWater = Util.IS_WATER_OR_WATERLOGGED.get(b);
                if (!inWater) {
                    playersInWater.remove(event.getPlayer());
                } else if (playersInWater.add(event.getPlayer()) || PortalType.WATER.detect(b)) {
                    handleEnter = true;
                }
            }
            if (handleEnter) {
                handlePortalEnter(PortalType.WATER, b, event.getPlayer(),
                        EntityUtil.getPortalCooldown(event.getPlayer()));
            }
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
        if (!config.getBedRespawnMode().persistInProfile()) {
            // If supported by bukkit, disable setting the spawn point
            // We check for an invalid spawn point next tick anyways
            try {
                event.setSpawnLocation(false);
            } catch (Throwable t) {
                /* Does not exist */
            }

            final Player player = event.getPlayer();
            CommonUtil.nextTick(new Runnable() {
                @Override
                public void run() {
                    WorldManager.removeInvalidBedSpawnPoint(player);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockChangedByEntity(EntityChangeBlockEvent event) {
        if (event.getTo() == END_PORTAL_FRAME_TYPE) {
            WorldConfig wc = WorldConfig.get(event.getBlock().getWorld());
            if (!wc.getDefaultEndPortalDestination().isActivationEnabled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortalCreated(PortalCreateEvent event) {
        if (event.getReason() == PortalCreateEvent.CreateReason.FIRE) {
            WorldConfig wc = WorldConfig.get(event.getWorld());
            if (!wc.getDefaultNetherPortalDestination().isActivationEnabled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        // If both end and nether portals are disabled, don't do anything
        if (!MyWorlds.endPortalEnabled && !MyWorlds.netherPortalEnabled) {
            return;
        }

        Block portalBlock = event.getLocation().getBlock();

        // Don't do it for players, causes horrible problems!
        if (event.getEntity() instanceof Player) {
            plugin.getPortalEnterEventDebouncer().triggerAndRunOnceATick(portalBlock, event.getEntity());
        } else {
            plugin.getPortalEnterEventDebouncer().trigger(portalBlock, event.getEntity());
        }
    }

    public void onPortalEnter(Block portalBlock, Entity entity, int portalCooldown) {
        // Decode what kind of portal block this is
        // This might return null for a valid case, because we have restrictions
        PortalType portalType = PortalType.findPortalType(portalBlock);
        if (portalType == null) {
            return;
        }

        // Check not filtered
        if (plugin.isPortalFiltered(portalType, portalBlock)) {
            return;
        }

        // If entity is a player that isn't bound to a world, then he is being respawned currently
        // For some reason the server spams portal enter events while players sit inside the
        // end portal, viewing the credits. We want none of that!
        if (entity instanceof Player) {
            World world = entity.getWorld();
            UUID uuid = entity.getUniqueId();
            if (world == null || EntityUtil.getEntity(world, uuid) != entity) {
                return;
            }
        }

        // Check for the initial portal enter cooldown for survival players, if active
        // This cooldown is set to non-zero once teleportation would normally commence
        // Creative players are exempt
        // All of this only matters for nether portals
        if (portalType == PortalType.NETHER) {
            if (entity instanceof Player) {
                if (!MyWorlds.alwaysInstantPortal && ((Player) entity).getGameMode() != GameMode.CREATIVE) {
                    if (Common.hasCapability("Common:EntityUtil:PortalWaitDelay")) {
                        if (!handlePortalEnterDelay(entity)) {
                            return;
                        }
                    } else {
                        if (!handlePortalEnterFallback(entity)) {
                            return;
                        }
                    }
                }
            } else if (portalCooldown > 0) {
                return;
            }
        }

        // Proceed
        handlePortalEnter(portalType, portalBlock, entity, portalCooldown);
    }

    private static boolean handlePortalEnterDelay(Entity entity) {
        CommonEntity<?> commonEntity = CommonEntity.get(entity);
        return (commonEntity.getPortalTime() >= commonEntity.getPortalWaitTime());
    }

    private static boolean handlePortalEnterFallback(Entity entity) {
        // By pure coincidence we can sort of use the cooldown to check when the delay has expired
        // This is because the server sets the cool down ticks after the delay expires
        // It's less good though...
        return EntityUtil.getPortalCooldown(entity) != 0;
    }

    private void handlePortalEnter(PortalType portalType, Block portalBlock, Entity entity, int portalCooldown) {
        // Figure out what the destination is of this portal
        PortalDestination.FindResults findResults = PortalDestination.findFromPortal(portalType, portalBlock);
        PortalDestination destination = findResults.getDestination();
        if (!findResults.isAvailable()) {
            return;
        }

        // If we already processed this destination for this entity this tick, do not process it again
        // This prevents running 2 or 4 teleports to the same destination for a player entering a portal
        // because the player position has become outdated.
        if (!destinationDebouncer.tryDestination(entity, destination)) {
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

        // If destination mode is set to VANILLA, we do nothing ourselves here
        if (destination.getMode() == PortalMode.VANILLA) {
            return;
        }

        // If the destination leads to a portal sign, let the portal sign deal with it
        // Do not run this logic if the portal mode is non-default and already matches a world name
        // This way if people name a portal "world", the mode nether-link default portal remains functional
        Location portalLocation = null;
        if (!destination.isPortalLookupIgnored()) {
            portalLocation = PortalStore.getPortalLocation(destination.getName(), portalBlock.getWorld().getName(), true, entity);
        }

        // Setup handler, which performs the teleportations for us
        PortalTeleportationHandler teleportationHandler = destination.getMode().createTeleportationHandler();
        teleportationHandler.setup(plugin, portalType, portalBlock, destination,
                findResults.getPortal(),
                (portalLocation != null) ? Optional.of(destination.getName()) : Optional.empty(),
                entity,
                portalCooldown);

        // If the destination leads to a portal sign, let the portal sign deal with it
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

            // Handle perms up-front
            if (!Portal.canTeleportEntityTo(entity, portalLocation)) {
                return;
            }

            // Teleport the entity the next tick
            teleportationHandler.scheduleTeleportation(portalLocation);
            return;
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        WorldConfig.get(event.getFrom()).onPlayerLeft(event.getPlayer());
        WorldConfig.get(event.getPlayer()).onPlayerEnter(event.getPlayer(), false);
    }

    // Spawn reason is incorrect on these versions of Minecraft
    private static final boolean TRADER_SPAWNS_BORKED = Common.evaluateMCVersion(">=", "1.14") && Common.evaluateMCVersion("<=", "1.14.4");

    // Since BKCommonLib 1.18.2-v2: reason attribute
    private static final FastMethod<CreatureSpawnEvent.SpawnReason> getPreSpawnReason = new FastMethod<>();
    static {
        try {
            getPreSpawnReason.init(CreaturePreSpawnEvent.class.getDeclaredMethod("getSpawnReason"));
        } catch (NoSuchMethodException | SecurityException e) {
            ClassResolver tmp = new ClassResolver();
            tmp.setDeclaredClass(CreaturePreSpawnEvent.class);
            getPreSpawnReason.init(new MethodDeclaration(tmp,
                    "public " + CreatureSpawnEvent.SpawnReason.class.getName() + " getSpawnReason() {\n" +
                    "    return " + CreatureSpawnEvent.SpawnReason.class.getName() + ".NATURAL;\n" +
                    "}"));
        }
    }

    private boolean isSpawnFiltered(CreatureSpawnEvent.SpawnReason reason, EntityType entityType) {
        // Check reason is something we check for or not
        switch (reason) {
        case NATURAL:
        case SPAWNER:
            return true;
        case SPAWNER_EGG:
            return !MyWorlds.ignoreEggSpawns;
        case BREEDING:
        case SLIME_SPLIT:
        case OCELOT_BABY:
        case INFECTION:
            return !MyWorlds.ignoreBreedingSpawns;
        case DEFAULT:
            return false;
        case CUSTOM:
            if (TRADER_SPAWNS_BORKED) {
                String name = entityType.name();
                if ("WANDERING_TRADER".equals(name) || "TRADER_LLAMA".equals(name)) {
                    return true;
                }
            }
            return false;
        default:
            // Ignore command-spawned mobs (COMMAND is not a valid enum on all mc versions)
            if (reason.name().equals("COMMAND")) {
                return false;
            }

            // Entity leaves player shoulder (ignore that, assume it was already spawned)
            if (reason.name().equals("SHOULDER_ENTITY")) {
                return false;
            }

            // Allay duplication logic
            if (reason.name().equals("DUPLICATION")) {
                return !MyWorlds.ignoreBreedingSpawns;
            }

            // Mushroom cow -> Sheared cow
            if (reason.name().equals("SHEARED")) {
                return false;
            }

            // Tadpole -> Frog growth stage
            if (reason.name().equals("METAMORPHOSIS")) {
                return false;
            }

            // Entity frozen in snow
            if (reason.name().equals("FROZEN")) {
                return false;
            }

            // For all cases we don't know about, better safe than sorry. Block it.
            return true;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreaturePreSpawn(CreaturePreSpawnEvent event) {
        // Only handle certain types of spawns
        if (!isSpawnFiltered(getPreSpawnReason.invoke(event), event.getEntityType())) {
            return;
        }

        // Cancel creature spawns before entities are spawned
        if (WorldConfig.get(event.getSpawnLocation()).spawnControl.isDenied(event.getEntityType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only handle certain types of spawns
        if (!isSpawnFiltered(event.getSpawnReason(), event.getEntityType())) {
            return;
        }

        // Cancel creature spawns after entities are created, before they spawn
        if (WorldConfig.get(event.getEntity()).spawnControl.isDenied(event.getEntity())) {
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
        if (MyWorlds.overridePortalPhysics && MaterialUtil.ISNETHERPORTAL.get(BlockPhysicsEventDataAccessor.INSTANCE.get(event))) {
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
            Localization.PORTAL_REMOVED.message(event.getPlayer(), portal.getName());
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpawnChange(SpawnChangeEvent event) {
        WorldConfig.get(event.getWorld()).resetSpawnLocationIfNotAt(event.getWorld().getSpawnLocation());
    }

    public boolean handleSignEdit(Player player, Block signBlock, SignSide signSide, String[] signLines) {
        // See if the player entered a portal on the sign
        Portal portal = Portal.get(signBlock, signLines);
        if (portal == null) {
            return true;
        }

        // If back-side is supported, verify the other side of the sign isn't also a portal
        // This isn't supported and so we send a message in that case
        if (SignSide.BACK.isSupported()) {
            Sign sign = BlockUtil.getSign(signBlock);
            if (sign != null && Portal.get(sign, signSide.opposite()) != null) {
                player.sendMessage(ChatColor.RED + "The other side of this sign already contains a portal!");
                return false;
            }
        }

        if (Permission.PORTAL_CREATE.has(player)) {
            if (Portal.exists(player.getWorld().getName(), portal.getName())) {
                if (!MyWorlds.allowPortalNameOverride || !Permission.PORTAL_OVERRIDE.has(player)) {
                    player.sendMessage(ChatColor.RED + "This portal name is already used!");
                    return false;
                }
            }
            portal.add();
            MyWorlds.plugin.logAction(player, "Created a new portal: '" + portal.getName() + "'!");
            // Build message
            if (portal.getDestinationName() != null) {
                Localization.PORTAL_CREATE_TO.message(player, portal.getDestinationName());
                if (!portal.hasDestination()) {
                    Localization.PORTAL_CREATE_MISSING.message(player);
                }
                if (portal.isRejoin()) {
                    Localization.PORTAL_CREATE_REJOIN.message(player);
                }
            } else {
                Localization.PORTAL_CREATE_END.message(player);
            }

            return true;
        } else {
            Localization.PORTAL_BUILD_NOPERM.message(player);
            return false;
        }
    }
}

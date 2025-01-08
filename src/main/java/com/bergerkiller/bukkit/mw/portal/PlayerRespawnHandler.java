package com.bergerkiller.bukkit.mw.portal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.mountiplex.MountiplexUtil;
import com.bergerkiller.mountiplex.reflection.util.FastConstructor;
import com.bergerkiller.mountiplex.reflection.util.FastMethod;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.internal.CommonBootstrap;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.WorldConfig;

/**
 * When players jump into an end portal in the end, the player
 * is removed from the world. After showing the end credits,
 * or if skipped, the player is respawned onto another world.<br>
 * <br>
 * This handlers makes sure to capture this respawn event,
 * and put the player to the right configured destination
 * instead. It also handles rewriting the respawn event
 * position for normal deaths, if one is configured for the world.
 */
public class PlayerRespawnHandler {
    private final MyWorlds plugin;
    private final Map<UUID, RespawnDestination> _respawns = new HashMap<>();
    private boolean isHandlingEntityPortalEvent = false;

    // Whether a respawn event is fired because the player entered an END portal, instead of dying/plugin cause
    private static final boolean END_RESPAWN_USING_PORTAL_EVENT = !CommonBootstrap.evaluateMCVersion(">=", "1.14");
    private static final Predicate<PlayerRespawnEvent> RESPAWN_IS_END_PORTAL = createRespawnIsEndPortalPredicate();
    private static Predicate<PlayerRespawnEvent> createRespawnIsEndPortalPredicate() {
        if (CommonBootstrap.evaluateMCVersion(">=", "1.20")) {
            // Bukkit now has a respawn cause option we can use to check this
            try {
                final FastMethod<Enum<?>> getRespawnReasonMethod = new FastMethod<>(PlayerRespawnEvent.class.getMethod("getRespawnReason"));
                return event -> {
                    Enum<?> reason = (Enum<?>) getRespawnReasonMethod.invoke(event);
                    return reason.name().equals("END_PORTAL");
                };
            } catch (Throwable t) {
                return event -> { throw MountiplexUtil.uncheckedRethrow(t); };
            }
        } else if (CommonBootstrap.evaluateMCVersion(">=", "1.14")) {
            // Must check using player health. If nonzero then it's (probably?) related to the end portal
            return event -> event.getPlayer().getHealth() > 0.0;
        } else {
            // Here end respawns are handled using the portal event, instead. So always false.
            return LogicUtil.alwaysFalsePredicate();
        }
    }

    // useTravelAgent(boolean) was removed at some point, but we must make sure to disable it on versions where it is active
    private static final Method useTravelAgentMethod;
    static {
        Method m = null;
        try {
            m = PlayerRespawnEvent.class.getDeclaredMethod("useTravelAgent", boolean.class);
        } catch (Throwable t) {}
        useTravelAgentMethod = m;
    }

    // isAnchorSpawn() doesn't exist on all versions of Bukkit/minecraft
    private static final Method isAnchorSpawnMethod;
    static {
        Method m = null;
        try {
            m = PlayerRespawnEvent.class.getDeclaredMethod("isAnchorSpawn");
        } catch (Throwable t) {}
        isAnchorSpawnMethod = m;
    }

    // When a spawn point was set using /spawnpoint
    private static final Method getRespawnLocationMethod;
    static {
        Method m = null;
        try {
            m = Player.class.getMethod("getRespawnLocation");
        } catch (Throwable t) {}
        getRespawnLocationMethod = m;
    }

    // Different constructors for the EntityPortalEvent
    private static final EntityPortalEventConstructor entityPortalEventConstructor = createEntityPortalEventConstructor();
    private static EntityPortalEventConstructor createEntityPortalEventConstructor() {
        try {
            for (Constructor<?> c : EntityPortalEvent.class.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();

                // Legacy
                //   public EntityPortalEvent(final Entity entity, final Location from, final Location to, final TravelAgent pta)
                if (params.length == 4 &&
                        params[0] == Entity.class &&
                        params[1] == Location.class &&
                        params[2] == Location.class &&
                        params[3].getSimpleName().equals("TravelAgent")
                ) {
                    final FastConstructor<EntityPortalEvent> fc = new FastConstructor<>(c);
                    return (entity, from, to) -> fc.newInstance(entity, from, to, null);
                }

                // Modern
                //   public EntityPortalEvent(@NotNull Entity entity, @NotNull Location from, @Nullable Location to)
                if (params.length == 3 &&
                        params[0] == Entity.class &&
                        params[1] == Location.class &&
                        params[2] == Location.class
                ) {
                    final FastConstructor<EntityPortalEvent> fc = new FastConstructor<>(c);
                    return (entity, from, to) -> fc.newInstance(entity, from, to);
                }
            }
            throw new IllegalStateException("No suitable constructor");
        } catch (Throwable t) {
            MyWorlds.plugin.getLogger().log(Level.WARNING,
                    "Failed to find EntityPortalEvent constructor", t);
            return null;
        }
    }

    // Different constructors for the PlayerPortalEvent
    private static final PlayerPortalEventConstructor playerPortalEventConstructor = createPlayerPortalEventConstructor();
    private static PlayerPortalEventConstructor createPlayerPortalEventConstructor() {
        try {
            for (Constructor<?> c : PlayerPortalEvent.class.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();

                // Legacy
                //   public PlayerPortalEvent(final Player entity, final Location from, final Location to, final TravelAgent pta)
                if (params.length == 4 &&
                        params[0] == Player.class &&
                        params[1] == Location.class &&
                        params[2] == Location.class &&
                        params[3].getSimpleName().equals("TravelAgent")
                ) {
                    final FastConstructor<PlayerPortalEvent> fc = new FastConstructor<>(c);
                    return (player, from, to) -> fc.newInstance(player, from, to, null);
                }

                // Modern
                //   public PlayerPortalEvent(@NotNull Player entity, @NotNull Location from, @Nullable Location to)
                if (params.length == 3 &&
                        params[0] == Player.class &&
                        params[1] == Location.class &&
                        params[2] == Location.class
                ) {
                    final FastConstructor<PlayerPortalEvent> fc = new FastConstructor<>(c);
                    return (player, from, to) -> fc.newInstance(player, from, to);
                }
            }
            throw new IllegalStateException("No suitable constructor");
        } catch (Throwable t) {
            MyWorlds.plugin.getLogger().log(Level.WARNING,
                    "Failed to find PlayerPortalEvent constructor", t);
            return null;
        }
    }

    public PlayerRespawnHandler(MyWorlds plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets whether a respawn event is the result of a player respawning in the end.
     * Since Minecraft 1.14 a respawn event also fires when the player
     * teleports away from the end. This checks for that situation.
     * 
     * @param event
     * @return True if this respawn event was due to using an end portal
     */
    public boolean isEndPortalRespawn(PlayerRespawnEvent event) {
        return RESPAWN_IS_END_PORTAL.test(event);
    }

    /**
     * Gets whether a respawn event involves respawning at a player
     * bed or world anchor.
     *
     * @param event
     * @return True if respawning at a bed or world anchor
     */
    public boolean isBedOrAnchorRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn()) {
            return true;
        }
        if (isAnchorSpawnMethod != null) {
            try {
                Boolean isAnchorSpawn = (Boolean) isAnchorSpawnMethod.invoke(event);
                if (isAnchorSpawn.booleanValue()) {
                    return true;
                }
            } catch (Throwable t) {
                this.plugin.getLogger().log(Level.SEVERE, "Unhandled error handling respawn event", t);
            }
        }
        if (getRespawnLocationMethod != null) {
            try {
                Location loc = (Location) getRespawnLocationMethod.invoke(event.getPlayer());
                if (loc != null) {
                    return true;
                }
            } catch (Throwable t) {
                this.plugin.getLogger().log(Level.SEVERE, "Unhandled error handling respawn event", t);
            }
        }
        return false;
    }

    /**
     * Handles when an Entity is being teleported by a Portal, by MyWorlds.
     * Returns null if the teleportation is cancelled. Can also return
     * a new (different) to Location if changed by another plugin.
     *
     * @param entity Entity that's teleporting by portal
     * @param from Previous position of the Entity
     * @param to Where MyWorlds wants to teleport the Player
     * @return Final to Location, or null if cancelled
     */
    public Location handlePortalEnter(Entity entity, Location from, Location to) {
        if (entityPortalEventConstructor == null) {
            return to;
        }

        if (entity instanceof Player) {
            PlayerTeleportEvent event = playerPortalEventConstructor.create((Player) entity, from, to);
            event.setCancelled(false);
            try {
                isHandlingEntityPortalEvent = true;
                if (CommonUtil.callEvent(event).isCancelled()) {
                    return null;
                } else {
                    return event.getTo();
                }
            } finally {
                isHandlingEntityPortalEvent = false;
            }
        } else {
            EntityPortalEvent event = entityPortalEventConstructor.create(entity, from, to);
            event.setCancelled(false);
            try {
                isHandlingEntityPortalEvent = true;
                if (CommonUtil.callEvent(event).isCancelled()) {
                    return null;
                } else {
                    return event.getTo();
                }
            } finally {
                isHandlingEntityPortalEvent = false;
            }
        }
    }

    public void enable() {

        this.plugin.register(new Listener() {
            @EventHandler(priority = EventPriority.NORMAL)
            public void onPlayerRespawn(PlayerRespawnEvent event) {
                if (isEndPortalRespawn(event)) {
                    // If we have a pending respawn, then the respawn event already has a pre-picked respawn location
                    // Take it and don't handle anything more
                    RespawnDestination newRespawn = _respawns.remove(event.getPlayer().getUniqueId());
                    if (newRespawn != null) {
                        if (newRespawn.isCancelled()) {
                            event.setRespawnLocation(event.getPlayer().getLocation());
                        } else {
                            event.setRespawnLocation(newRespawn.position);
                            event.getPlayer().setVelocity(newRespawn.velocity);
                        }
                    }
                } else {
                    // Update spawn position based on world configuration
                    if (MyWorlds.forceMainWorldSpawn) {
                        // Force a respawn on the main world
                        event.setRespawnLocation(WorldConfig.get(MyWorlds.getMainWorld()).getSpawnLocation());
                    } else if (!isBedOrAnchorRespawn(event) || !WorldConfig.get(event.getPlayer()).getBedRespawnMode().useWhenRespawning()) {
                        // Respawn at what is set in the world configuration of this World
                        World fromWorld = event.getPlayer().getWorld();
                        Location newRespawn = WorldConfig.get(fromWorld).respawnPoint
                                .get(event.getPlayer(), fromWorld);
                        if (newRespawn != null) {
                            event.setRespawnLocation(newRespawn);
                        }
                    }
                }
            }

            @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
            public void onPlayerPortal(PlayerPortalEvent event) {
                // Ignore the event we're firing ourselves
                if (isHandlingEntityPortalEvent) {
                    isHandlingEntityPortalEvent = false;
                    return;
                }

                if (END_RESPAWN_USING_PORTAL_EVENT) {
                    RespawnDestination newRespawn = _respawns.remove(event.getPlayer().getUniqueId());
                    if (newRespawn != null) {
                        event.setTo(newRespawn.position);
                        event.getPlayer().setVelocity(newRespawn.velocity);
                        if (useTravelAgentMethod != null) {
                            try {
                                useTravelAgentMethod.invoke(event, Boolean.FALSE);
                            } catch (Throwable t) {}
                        }
                        return;
                    }
                }

                // Cancel all portal events we handle ourselves, not handled up here
                if (checkPortalHandled(event.getPlayer(), event.getFrom())) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
            public void onEntityPortal(EntityPortalEvent event) {
                // Ignore the event we're firing ourselves
                if (isHandlingEntityPortalEvent) {
                    isHandlingEntityPortalEvent = false;
                    return;
                }

                // Cancel events that we handle ourselves, for all non-player entities
                if (checkPortalHandled(event.getEntity(), event.getFrom())) {
                    event.setCancelled(true);
                }
            }

            private boolean checkPortalHandled(Entity entity, Location from) {
                // If portals aren't handled, ignore all of this
                if (from == null || (!MyWorlds.endPortalEnabled && !MyWorlds.netherPortalEnabled)) {
                    return false;
                }

                // If portal is nearby that we handle, we handle it for sure
                Block portalBlock = from.getBlock();
                PortalType portalType = PortalType.findPortalType(portalBlock);
                if (portalType != null) {
                    // If default portal mode is VANILLA, check whether there is a portal sign nearby at all
                    // If not, we do NOT handle it, and allow default vanilla behavior
                    if (WorldConfig.get(portalBlock).getDefaultDestination(portalType).getMode() == PortalMode.VANILLA) {
                        if (!PortalDestination.findFromPortal(portalType, portalBlock).isFromPortal()) {
                            return false;
                        }
                    }

                    return true;
                }

                // There is a bukkit bug that it sends another portal enter event one tick
                // delayed after teleporting. Detect that and reject that event as well.
                Location loc = plugin.getPortalTeleportationCooldown().getPortal(entity);
                if (loc != null && loc.equals(from)) {
                    return true;
                }

                return false;
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerQuit(PlayerQuitEvent event) {
                _respawns.remove(event.getPlayer().getUniqueId());
            }
        });
    }

    public void disable() {
    }

    /**
     * Sets the next respawn location and velocity of the player. When the player
     * respawns, the player is teleported to this location, moving at the velocity
     * as specified.
     * 
     * @param player Player that is about to respawn
     * @param position Position the player should respawn at
     * @param velocity Motion of the player right after respawning
     */
    public void setNextRespawn(Player player, Location position, Vector velocity) {
        _respawns.put(player.getUniqueId(), new RespawnDestination(position, velocity));
    }

    /**
     * Cancels the next respawn. This teleports the player exactly where the player already is.
     *
     * @param player Player to cancel the respawn for
     */
    public void cancelNextRespawn(Player player) {
        _respawns.put(player.getUniqueId(), new RespawnDestination(null, null));
    }

    private static final class RespawnDestination {
        public final Location position;
        public final Vector velocity;

        public RespawnDestination(Location position, Vector velocity) {
            this.position = position; // Can be null!
            this.velocity = velocity; // Can be null!
        }

        public boolean isCancelled() {
            return this.position == null;
        }
    }

    @FunctionalInterface
    private interface EntityPortalEventConstructor {
        EntityPortalEvent create(Entity entity, Location from, Location to);
    }

    @FunctionalInterface
    private interface PlayerPortalEventConstructor {
        PlayerPortalEvent create(Player player, Location from, Location to);
    }
}

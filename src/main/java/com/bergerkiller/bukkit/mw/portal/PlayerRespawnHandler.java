package com.bergerkiller.bukkit.mw.portal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
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
    private static final boolean END_RESPAWN_USING_PORTAL_EVENT = CommonBootstrap.evaluateMCVersion("<=", "1.13.2");
    private final MyWorlds plugin;
    private final Map<UUID, RespawnDestination> _respawns = new HashMap<>();

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

    public PlayerRespawnHandler(MyWorlds plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets whether a respawn event is the result of a player death or not.
     * Since Minecraft 1.14 a respawn event also fires when the player
     * teleports away from the end. This checks for that situation.
     * 
     * @param event
     * @return True if this respawn event was due to player death
     */
    public boolean isDeathRespawn(PlayerRespawnEvent event) {
        return END_RESPAWN_USING_PORTAL_EVENT || event.getPlayer().getHealth() <= 0.0;
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
        return false;
    }

    public void enable() {

        this.plugin.register(new Listener() {
            @EventHandler(priority = EventPriority.NORMAL)
            public void onPlayerRespawn(PlayerRespawnEvent event) {
                if (isDeathRespawn(event)) {
                    // Update spawn position based on world configuration
                    if (MyWorlds.forceMainWorldSpawn) {
                        // Force a respawn on the main world
                        event.setRespawnLocation(WorldConfig.get(MyWorlds.getMainWorld()).getSpawnLocation());
                    } else if (!isBedOrAnchorRespawn(event) || !WorldConfig.get(event.getPlayer()).bedRespawnEnabled) {
                        // Respawn at what is set in the world configuration of this World
                        World fromWorld = event.getPlayer().getWorld();
                        event.setRespawnLocation(WorldConfig.get(fromWorld).respawnPoint
                                .get(event.getPlayer(), fromWorld));
                    }
                } else {
                    // If we have a pending respawn, then the respawn event already has a pre-picked respawn location
                    // Take it and don't handle anything more
                    RespawnDestination newRespawn = _respawns.remove(event.getPlayer().getUniqueId());
                    if (newRespawn != null) {
                        event.setRespawnLocation(newRespawn.position);
                        event.getPlayer().setVelocity(newRespawn.velocity);
                    }
                }
            }

            @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
            public void onPlayerPortal(PlayerPortalEvent event) {
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

                // If portals aren't handled, ignore all of this
                if (event.getFrom() == null || (!MyWorlds.endPortalEnabled && !MyWorlds.netherPortalEnabled)) {
                    return;
                }

                // Cancel all portal events we handle ourselves, not handled up here
                if (PortalType.findPortalType(event.getFrom().getBlock()) != null) {
                    event.setCancelled(true);
                }
            }

            @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
            public void onEntityPortal(EntityPortalEvent event) {
                // If portals aren't handled, ignore all of this
                if (event.getFrom() == null || (!MyWorlds.endPortalEnabled && !MyWorlds.netherPortalEnabled)) {
                    return;
                }

                // Cancel events that we handle ourselves, for all non-player entities
                if (PortalType.findPortalType(event.getFrom().getBlock()) != null) {
                    event.setCancelled(true);
                }
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
     * @param player
     * @param location
     * @param velocity
     */
    public void setNextRespawn(Player player, Location position, Vector velocity) {
        _respawns.put(player.getUniqueId(), new RespawnDestination(position, velocity));
    }

    private static final class RespawnDestination {
        public final Location position;
        public final Vector velocity;

        public RespawnDestination(Location position, Vector velocity) {
            this.position = position;
            this.velocity = velocity;
        }
    }
}

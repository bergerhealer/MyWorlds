package com.bergerkiller.bukkit.mw.portal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
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
import com.bergerkiller.bukkit.mw.WorldManager;

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

    public void enable() {

        this.plugin.register(new Listener() {
            @EventHandler(priority = EventPriority.NORMAL)
            public void onPlayerRespawn(PlayerRespawnEvent event) {
                if (isDeathRespawn(event)) {
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
                        event.useTravelAgent(false);
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

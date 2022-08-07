package com.bergerkiller.bukkit.mw.portal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;

/**
 * Tracks cooldowns for portal entering and teleportation.
 * Prevents rapid teleportation loops when teleporting between
 * portals
 */
public class PortalTeleportationCooldown {
    private static final double WALK_DIST_SQ = 2.25;
    /**
     * After this number of ticks elapse without an attempted teleport, 'forget' about
     * the cooldown we stored in the past. Must be high enough to prevent instantly
     * taking the portal again during the changing-world delay. As entries get reset
     * when players walk away from the portal, this timeout doesn't need to be very
     * small.
     */
    private static final int PORTAL_COOLDOWN_TIMEOUT = 300;

    private final MyWorlds plugin;
    private final Map<UUID, TeleportedPosition> _positions = new HashMap<>();
    private Task refreshTask = null;

    public PortalTeleportationCooldown(MyWorlds plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        refreshTask = new Task(plugin) {
            @Override
            public void run() {
                if (_positions.isEmpty()) {
                    return;
                }
                int currentTicks = CommonUtil.getServerTicks();
                Iterator<Map.Entry<UUID, TeleportedPosition>> iter = _positions.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<UUID, TeleportedPosition> entry = iter.next();

                    // If no position is stored, get rid of it entirely
                    if (entry.getValue().position == null) {
                        iter.remove();
                        continue;
                    }

                    // Find the entity with this UUID. First try the world we know,
                    // if not found, assume either a world change happened or
                    // that the entity despawned. Otherwise, ask the entry.
                    Entity entity = EntityUtil.getEntity(entry.getValue().position.getWorld(), entry.getKey());
                    if (entity == null || entry.getValue().tryExpire(entity, currentTicks)) {
                        iter.remove();
                        continue;
                    }

                    // Reset every tick
                    entry.getValue().justJoined = false;
                }
            }
        }.start(1, 1);
        plugin.register(new Listener() {
            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerJoin(PlayerJoinEvent event) {
                // For the current tick, ignore the player teleport event
                getTeleportedPosition(event.getPlayer()).justJoined = true;
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerRespawn(PlayerRespawnEvent event) {
                if (event.getRespawnLocation() != null) {
                    setPortal(event.getPlayer(), event.getRespawnLocation());
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onPlayerTeleport(PlayerTeleportEvent event) {
                TeleportedPosition tp = getTeleportedPosition(event.getPlayer());
                if (!tp.justJoined) {
                    setPortal(event.getPlayer(), event.getTo());
                }
            }

            @EventHandler(priority = EventPriority.MONITOR)
            public void onPlayerQuit(PlayerQuitEvent event) {
                _positions.remove(event.getPlayer().getUniqueId());
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onCreatureSpawn(CreatureSpawnEvent event) {
                if (event.getSpawnReason() == SpawnReason.NETHER_PORTAL) {
                    setPortal(event.getEntity(), event.getLocation());
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onEntityPortal(EntityPortalEvent event) {
                if (event.getTo() != null) {
                    setPortal(event.getEntity(), event.getTo());
                }
            }

            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onEntityTeleport(EntityTeleportEvent event) {
                setPortal(event.getEntity(), event.getTo());
            }
        });
    }

    public void disable() {
        Task.stop(refreshTask);
        refreshTask = null;
        _positions.clear();
    }

    /**
     * Gets the last-known portal position set using {@link #setPortal(Entity, Location)}
     * 
     * @param entity
     * @return Location, null if none is set
     */
    public Location getPortal(Entity entity) {
        TeleportedPosition position = _positions.get(entity.getUniqueId());
        return (position == null) ? null : position.position;
    }

    /**
     * Sets the predicted next position of the Entity at the destination portal.
     * Prevents the entity teleporting back on arrival.
     * 
     * @param entity
     * @param position
     */
    public void setPortal(Entity entity, Location position) {
        getTeleportedPosition(entity).set(position);
    }

    /**
     * Checks whether an Entity is allowed to enter a portal right now
     * to initiate another teleport. If the entity recently entered
     * a portal and has not moved away from it, then entering the
     * portal is cancelled.<br>
     * <br>
     * If the portal could not be entered, then the cooldown is extended.
     * 
     * @param entity
     * @param entityPosition expected position of the Entity
     * @return True if the entity is allowed to enter a (new) portal
     */
    public boolean tryEnterPortal(Entity entity) {
        return tryEnterPortal(entity, entity.getLocation());
    }

    /**
     * Checks whether an Entity is allowed to enter a portal right now
     * to initiate another teleport. If the entity recently entered
     * a portal and has not moved away from it, then entering the
     * portal is cancelled.<br>
     * <br>
     * If the portal could not be entered, then the cooldown is extended.
     * 
     * @param entity
     * @param entityPosition expected position of the Entity
     * @return True if the entity is allowed to enter a (new) portal
     */
    public boolean tryEnterPortal(Entity entity, Location entityPosition) {
        TeleportedPosition tp = getTeleportedPosition(entity);
        boolean allowed = tp.canEnter(entity, entityPosition);
        tp.set(entityPosition);
        return allowed;
    }

    private TeleportedPosition getTeleportedPosition(Entity entity) {
        return _positions.computeIfAbsent(entity.getUniqueId(), TeleportedPosition::new);
    }

    private static class TeleportedPosition {
        public Location position;
        public int cooldown;
        public boolean justJoined;

        public TeleportedPosition(UUID entityUUID) {
            this.position = null;
            this.cooldown = Integer.MIN_VALUE;
            this.justJoined = false;
        }

        public void set(Location position) {
            this.position = (position == null) ? null : position.clone();
            this.cooldown = CommonUtil.getServerTicks();
        }

        public boolean tryExpire(Entity entity, int currentTicks) {
            if (this.position.distanceSquared(entity.getLocation()) > WALK_DIST_SQ) {
                return true; // expired, entity walked away from the portal
            } else if (currentTicks > (this.cooldown + PORTAL_COOLDOWN_TIMEOUT)) {
                return true; // expired, cooldown timer has expired
            } else {
                return false; // still active
            }
        }

        public boolean canEnter(Entity entity, Location entityPosition) {
            // Check cooldown timer. If expired, permit entering the portal
            if (CommonUtil.getServerTicks() > (this.cooldown + PORTAL_COOLDOWN_TIMEOUT)) {
                return true;
            }

            // Different world
            if (this.position.getWorld() != entityPosition.getWorld()) {
                return true;
            }

            // Far enough away from last time
            if (this.position.distanceSquared(entityPosition) > WALK_DIST_SQ) {
                return true;
            }

            return false;
        }
    }
}

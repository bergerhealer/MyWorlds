package com.bergerkiller.bukkit.mw.portal;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.entity.CommonEntity;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.events.MyWorldsTeleportPortalEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.math.Quaternion;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.generated.net.minecraft.server.level.EntityPlayerHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.EntityHandle;

import java.util.Optional;

/**
 * Interface for main teleportation handling functions
 */
public abstract class PortalTeleportationHandler {
    private static final boolean IS_END_PORTAL_CANCELLABLE = Common.evaluateMCVersion(">=", "1.21");

    protected MyWorlds plugin;
    protected PortalType portalType;
    protected Block portalBlock;
    protected PortalDestination destination;
    protected Optional<Portal> fromPortal;
    protected Optional<String> toPortalName;
    protected Entity entity;
    protected int portalCooldown;

    public void setup(MyWorlds plugin,
                      PortalType portalType,
                      Block portalBlock,
                      PortalDestination destination,
                      Optional<Portal> fromPortal,
                      Optional<String> toPortalName,
                      Entity entity,
                      int portalCooldown)
    {
        this.plugin = plugin;
        this.portalType = portalType;
        this.portalBlock = portalBlock;
        this.destination = destination;
        this.fromPortal = fromPortal;
        this.toPortalName = toPortalName;
        this.entity = entity;
        this.portalCooldown = portalCooldown;
    }

    /**
     * Performs teleportation for an Entity, updating the last-entered portal position automatically.
     * If the teleportation fails, then the original (entered) portal position is restored.
     * The velocity for the entity on the other end is automatically computed,
     * based on the change in orientation of the player.
     *
     * @param position
     */
    public void scheduleTeleportation(Location position) {
        Vector velocity;
        if (entity instanceof Minecart) {
            // Minecarts dont really have a clear front and back, so they can drive into
            // a portal in any orientation. Assume whatever velocity the minecart has
            // is meant to be moving forwards, out of the portal's position.
            velocity = position.getDirection().multiply(entity.getVelocity().length());
        } else {
            Vector forwardDirection;
            if (entity instanceof LivingEntity) {
                forwardDirection = ((LivingEntity) entity).getEyeLocation().getDirection();
            } else {
                forwardDirection = entity.getLocation().getDirection();
            }

            Quaternion old_orientation = Quaternion.fromLookDirection(forwardDirection, new Vector(0.0, 1.0, 0.0));
            Quaternion new_orientation = Quaternion.fromLookDirection(position.getDirection(), new Vector(0.0, 1.0, 0.0));
            Quaternion diff = Quaternion.diff(old_orientation, new_orientation);
            velocity = entity.getVelocity();
            diff.transformPoint(velocity);
        }

        // Position fix for minecarts
        if (entity instanceof Minecart) {
            position = position.clone();
            position.setYaw(position.getYaw() - 90.0f);
        }

        scheduleTeleportationWithVelocity(position, velocity);
    }

    /**
     * Performs teleportation for an Entity, updating the last-entered portal position automatically.
     * If the teleportation fails, then the original (entered) portal position is restored.
     * 
     * @param destPosition Original position MyWorlds wants to teleport the player to
     * @param destVelocity Initial velocity of the Entity. Isn't used if position is changed by plugins.
     */
    public void scheduleTeleportationWithVelocity(Location destPosition, Vector destVelocity) {
        disablePortalsForCooldown();

        // Fire an event that allows for updating the position, but can also cancel it
        Location positionAdjusted = plugin.getEndRespawnHandler().handlePortalEnter(entity, entity.getLocation(), destPosition.clone());
        if (positionAdjusted == null) {
            return;
        }

        // If position was changed by a plugin, keep Entity velocity
        final Vector velocity = destPosition.equals(positionAdjusted) ? destVelocity : entity.getVelocity();

        // Event stuff
        {
            MyWorldsTeleportPortalEvent event = new MyWorldsTeleportPortalEvent(
                    entity,
                    portalType,
                    positionAdjusted,
                    destination,
                    fromPortal,
                    toPortalName);
            if (CommonUtil.callEvent(event).isCancelled()) {
                return;
            }
            positionAdjusted = event.getTo();
        }

        final Location position = positionAdjusted;

        if (entity instanceof Player) {
            Player player = (Player) entity;

            // Cannot cancel the default respawn behavior of the server
            // All we can do is handle the respawn event and teleport there
            // However, what we can do is avoid showing the end credits if not desired
            if (this.isFromTheMainEnd()) {
                if (!this.destination.isShowCredits()) {
                    final EntityPlayerHandle player_handle = EntityPlayerHandle.fromBukkit(player);
                    if (!player_handle.hasSeenCredits()) {
                        player_handle.setHasSeenCredits(true);
                        CommonUtil.nextTick(() -> player_handle.setHasSeenCredits(true));
                    }                    
                }
                plugin.getEndRespawnHandler().setNextRespawn(player, position, velocity);

                if (!IS_END_PORTAL_CANCELLABLE) {
                    return;
                }
            }

            // Forcibly show the credits, which creates a respawn event similar to the default behavior
            if (this.destination.isShowCredits()) {
                PlayerUtil.showEndCredits(player);
                plugin.getEndRespawnHandler().setNextRespawn(player, position, velocity);
                return;
            }
        }

        // Default next-tick teleportation
        CommonUtil.nextTick(() -> {
            performTeleportation(position, velocity);
        });
    }

    /**
     * Performs teleportation for an Entity, updating the last-entered portal position automatically.
     * If the teleportation fails, then the original (entered) portal position is restored.
     * 
     * @param position
     * @param velocity
     */
    private boolean performTeleportation(Location position, Vector velocity) {
        disablePortalsForCooldown();

        Location original_position = plugin.getPortalTeleportationCooldown().getPortal(entity);
        plugin.getPortalTeleportationCooldown().setPortal(entity, position);
        if (CommonEntity.get(entity).teleport(position, portalType.getTeleportCause())) {
        //if (entity.teleport(position, portalType.getTeleportCause())) {
            // Verify that the Entity position matches the same BLOCK coordinates
            // The server sometimes teleports entities at floor(coords) rather than the actual ones
            // We must correct for this
            // NOT needed for players, whose entity doesn't despawn and respawn.
            Location trueLoc;
            if (!(entity instanceof Player) &&
                !(trueLoc = entity.getLocation()).equals(position) &&
                position.getWorld() == trueLoc.getWorld() &&
                position.getBlockX() == trueLoc.getBlockX() &&
                position.getBlockY() == trueLoc.getBlockY() &&
                position.getBlockZ() == trueLoc.getBlockZ())
            {
                EntityHandle handle = EntityHandle.fromBukkit(entity);
                handle.setPosition(position.getX(), position.getY(), position.getZ());
            }

            // This as well
            disablePortalsForCooldown();

            // Give momentum
            entity.setVelocity(velocity);
            return true;
        } else {
            if (original_position != null) {
                plugin.getPortalTeleportationCooldown().setPortal(entity, original_position);
            }
            return false;
        }
    }

    private void disablePortalsForCooldown() {
        // Before teleporting, set a cooldown. Clear any queued up portal enter events
        // to prevent cooldown being reset to 0 again.
        EntityUtil.setPortalCooldown(entity, EntityUtil.getPortalCooldownMaximum(entity));
        plugin.getPortalEnterEventDebouncer().clear(entity);
    }

    /**
     * Gets whether the main end portal was entered by the entity. For players,
     * this normally causes end credits to show, and an automatic respawn to occur.
     * 
     * @return end world portal was entered
     */
    public boolean isFromTheMainEnd() {
        return portalType == PortalType.END && WorldUtil.isMainEndWorld(portalBlock.getWorld());
    }

    /**
     * Checks whether new portals can be created by this entity with the current
     * configuration
     * 
     * @return True if portals can be created
     */
    public boolean checkCanCreatePortals() {
        if (this.entity instanceof Player) {
            return Permission.GENERAL_LINKNETHER.has((Player) entity);
        } else {
            return this.destination.canNonPlayersCreatePortals();
        }
    }

    /**
     * Handles teleportation to a world
     * 
     * @param world The world to teleport to
     */
    public abstract void handleWorld(World world);
}

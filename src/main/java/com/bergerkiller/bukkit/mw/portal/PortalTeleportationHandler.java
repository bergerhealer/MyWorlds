package com.bergerkiller.bukkit.mw.portal;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.math.Quaternion;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.PlayerUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.generated.net.minecraft.server.EntityPlayerHandle;

/**
 * Interface for main teleportation handling functions
 */
public abstract class PortalTeleportationHandler {
    protected MyWorlds plugin;
    protected PortalType portalType;
    protected Block portalBlock;
    protected PortalDestination destination;
    protected Entity entity;

    public void setup(MyWorlds plugin,
                      PortalType portalType,
                      Block portalBlock,
                      PortalDestination destination,
                      Entity entity)
    {
        this.plugin = plugin;
        this.portalType = portalType;
        this.portalBlock = portalBlock;
        this.destination = destination;
        this.entity = entity;
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
        Vector forwardDirection;
        if (entity instanceof LivingEntity) {
            forwardDirection = ((LivingEntity) entity).getEyeLocation().getDirection();
        } else {
            forwardDirection = entity.getLocation().getDirection();
        }
        Quaternion old_orientation = Quaternion.fromLookDirection(forwardDirection, new Vector(0.0, 1.0, 0.0));
        Quaternion new_orientation = Quaternion.fromLookDirection(position.getDirection(), new Vector(0.0, 1.0, 0.0));
        Quaternion diff = Quaternion.diff(old_orientation, new_orientation);
        Vector velocity = entity.getVelocity();
        diff.transformPoint(velocity);
        scheduleTeleportation(position, velocity);
    }

    /**
     * Performs teleportation for an Entity, updating the last-entered portal position automatically.
     * If the teleportation fails, then the original (entered) portal position is restored.
     * 
     * @param position
     * @param velocity
     */
    public void scheduleTeleportation(Location position, Vector velocity) {
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
                return;
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
    public boolean performTeleportation(Location position, Vector velocity) {
        Location original_position = plugin.getPortalTeleportationCooldown().getPortal(entity);
        plugin.getPortalTeleportationCooldown().setPortal(entity, position);
        //if (CommonEntity.get(entity).teleport(position, portalType.getTeleportCause())) {
        if (entity.teleport(position, portalType.getTeleportCause())) {
            entity.setVelocity(velocity);
            return true;
        } else {
            if (original_position != null) {
                plugin.getPortalTeleportationCooldown().setPortal(entity, original_position);
            }
            return false;
        }
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

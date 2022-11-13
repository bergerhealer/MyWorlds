package com.bergerkiller.bukkit.mw.portal.handlers;

import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.math.Matrix4x4;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.portal.NetherPortalOrientation;
import com.bergerkiller.bukkit.mw.portal.NetherPortalSearcher;
import com.bergerkiller.bukkit.mw.portal.PortalTeleportationHandler;
import com.bergerkiller.bukkit.mw.portal.NetherPortalSearcher.SearchStatus;
import com.bergerkiller.generated.net.minecraft.world.level.dimension.DimensionManagerHandle;

/**
 * Handler for teleporting between worlds through nether portals,
 * searching for existing portals on the other end, or creating
 * new ones if needed.
 */
public class PortalTeleportationHandlerNetherLink extends PortalTeleportationHandler {
    private static final Method getCoordinateScaleMethod;

    static {
        Method m = null;
        if (Common.evaluateMCVersion(">=", "1.16") && DimensionManagerHandle.T.isAvailable()) {
            try {
                m = DimensionManagerHandle.T.getType().getMethod("getCoordinateScale");
                if (m.getReturnType() != double.class) {
                    m = null;
                }
            } catch (NoSuchMethodException ex) {}
        }
        getCoordinateScaleMethod = m;
    }

    @Override
    public void handleWorld(World world) {
        // Figure out the desired Block location on the other world
        // This uses *8 rules for nether world vs other worlds
        double factor = getCoordinateScale(portalBlock.getWorld()) / getCoordinateScale(world);

        // Default radius to look for portals, server configuration
        double baseSearch = 128.0;
        if (Common.hasCapability("Common:WorldUtil:getDefaultNetherPortalSearchRadius")) {
            baseSearch = getNetherPortalSearchRadius(world);
        }
        baseSearch /= 8.0; // We included a *8 in the coordinate scaling factor up above, MC doesn't

        // Turn factor into a search radius
        int searchRadius = (int) (Math.max(1.0, factor) * baseSearch);
        Block searchStartBlock = world.getBlockAt(MathUtil.floor(portalBlock.getX() * factor),
                                                  portalBlock.getY(),
                                                  MathUtil.floor(portalBlock.getZ() * factor));

        // Calculate the create options for the current entity teleportation
        NetherPortalSearcher.CreateOptions createOptions = null;
        if (checkCanCreatePortals()) {
            createOptions = NetherPortalSearcher.CreateOptions.create()
                    .initiator(entity)
                    .orientation(portalType.getOrientation(portalBlock, entity));
        }

        // Start searching
        NetherPortalSearcher.SearchResult result = plugin.getNetherPortalSearcher().search(searchStartBlock, searchRadius, createOptions);

        // For entities that can be kept in stasis while processing happens in the background,
        // put the entity in there. Or release, when no longer busy.
        // If this parity breaks, a timeout will free the entity eventually.
        if (plugin.getEntityStasisHandler().canBeKeptInStasis(entity)) {
            if (result.getStatus().isBusy()) {
                plugin.getEntityStasisHandler().putInStasis(entity);
            } else {
                plugin.getEntityStasisHandler().freeFromStasis(entity);
            }
        }

        // Stop when busy / debounce
        if (result.getStatus().isBusy() || !plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
            EntityUtil.setPortalCooldown(entity, portalCooldown); // Reset cooldown so the portal is entered again next tick
            if (this.portalType == PortalType.NETHER && Common.hasCapability("Common:EntityUtil:PortalWaitDelay")) {
                extendPortalWaitTime(entity);
            }
            return;
        }

        if (result.getStatus() == SearchStatus.NOT_FOUND) {
            // Failed to find an existing portal, we can't create a new one
            if (entity instanceof Player) {
                Localization.PORTAL_LINK_UNAVAILABLE.message((Player) entity);
            }
        } else if (result.getStatus() == SearchStatus.NOT_CREATED) {
            // Failed to create a new portal on the other end
            if (entity instanceof Player) {
                Localization.PORTAL_LINK_FAILED.message((Player) entity);
            }
        } else if (result.getStatus() == SearchStatus.FOUND) {
            // Retrieve portal transformations for the current (old) and destination (new) portal
            NetherPortalOrientation destPortalOrientation = NetherPortalOrientation.compute(result.getResult().getBlock());
            Matrix4x4 oldTransform = portalType.getTransform(portalBlock, entity);
            Matrix4x4 newTransform = destPortalOrientation.getTransform();

            // Retrieve location by combining the entity feet position with the (possible) player eye location
            Matrix4x4 transform;
            {
                Location loc = entity.getLocation();
                if (entity instanceof LivingEntity) {
                    Location eyeLoc = ((LivingEntity) entity).getEyeLocation();
                    loc.setYaw(eyeLoc.getYaw());
                    loc.setPitch(eyeLoc.getPitch());
                }
                transform = Matrix4x4.fromLocation(loc);
            }

            // Change transformation to be relative to the portal entered
            transform = Matrix4x4.diff(oldTransform, transform);

            // Remove distance from portal as a component from the transform
            // This positions the player inside the portal on arrival, rather than slightly in front
            {
                Matrix4x4 correctiveTransform = new Matrix4x4();
                correctiveTransform.translate(0.0, 0.0, -transform.toVector().getZ());
                transform.storeMultiply(correctiveTransform, transform);
            }

            // Apply new portal transformation to old portal relative transformation
            transform.storeMultiply(newTransform, transform);

            // Compute the final Location information from the transform
            Location locToTeleportTo = transform.toLocation(result.getResult().getWorld());

            // Check that this location sits inside an existing portal frame on the destination
            // If not, teleport the entity to a place where it's safe in the middle of a portal
            destPortalOrientation.adjustPosition(entity, locToTeleportTo);

            // Retrieve the velocity of the entity upon entering the portal
            // Transform this velocity the same way we transformed the position
            Vector velocityAfterTeleport = entity.getVelocity();
            Matrix4x4.diffRotation(oldTransform, newTransform).transformPoint(velocityAfterTeleport);

            // Perform the teleportation woo
            scheduleTeleportationWithVelocity(locToTeleportTo, velocityAfterTeleport);
        }
    }

    private static void extendPortalWaitTime(Entity entity) {
        EntityUtil.setPortalTime(entity, EntityUtil.getPortalWaitTime(entity) - 1);
    }

    // Just to avoid nonsense, it is in its own method, as it does not always exist
    // Can be removed when BKCommonLib 1.18.1-v2 or newer is a hard dep
    private static int getNetherPortalSearchRadius(World world) {
        return WorldUtil.getNetherPortalSearchRadius(world);
    }

    /**
     * Gets the coordinate scale used for a World. Datapacks can alter this for a world.
     * On versions before datapacks, the nether environment is assumed to have a scale
     * of 8.
     *
     * @param world
     * @return coordinate scale on the world
     */
    public static double getCoordinateScale(World world) {
        //WorldUtil.getDimensionType(world).getDimensionManagerHandle()
        if (getCoordinateScaleMethod != null) {
            Object dim = WorldUtil.getDimensionType(world).getDimensionManagerHandle();
            try {
                return (double) getCoordinateScaleMethod.invoke(dim);
            } catch (Throwable t) {
                MyWorlds.plugin.getLogger().log(Level.SEVERE,
                        "Failed to read coordinate scale, switching to fallback", t);
            }
        }
        return world.getEnvironment() == Environment.NETHER ? 8.0 : 1.0;
    }
}

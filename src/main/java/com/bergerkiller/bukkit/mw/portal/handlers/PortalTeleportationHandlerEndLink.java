package com.bergerkiller.bukkit.mw.portal.handlers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.portal.PortalTeleportationHandler;

/**
 * Teleports a player to another world at fixed coordinates, where
 * an obsidian platform is prepared for the entity.
 */
public class PortalTeleportationHandlerEndLink extends PortalTeleportationHandler {
    @Override
    public void handleWorld(World world) {
        // Debounce
        if (!plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
            return;
        }

        // Find or create the end platform
        Block endPlatform = WorldUtil.findEndPlatform(world);
        if (endPlatform == null) {
            endPlatform = WorldUtil.createEndPlatform(world, entity);
            if (endPlatform == null) {
                if (entity instanceof Player) {
                    Localization.PORTAL_LINK_FAILED.message((Player) entity);
                }
                return;
            }
        }

        // Teleport player on top of the platform
        Location destinationLoc = endPlatform.getLocation().add(0.5, 1.0, 0.5);
        scheduleTeleportation(destinationLoc);
    }
}

package com.bergerkiller.bukkit.mw.portal.handlers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.portal.PortalTeleportationHandler;

/**
 * Default handling for teleporting to a World
 */
public class PortalTeleportationHandlerRejoin extends PortalTeleportationHandler {
    @Override
    public void handleWorld(World world) {
        if (plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
            Location destinationLoc;
            if (entity instanceof Player) {
                destinationLoc = WorldManager.getPlayerRejoinPosition((Player) entity, world);
            } else {
                destinationLoc = WorldManager.getSpawnLocation(world);
            }

            // Perform the teleportation woooo
            scheduleTeleportation(destinationLoc);
        }
    }
}

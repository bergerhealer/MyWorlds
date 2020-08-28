package com.bergerkiller.bukkit.mw.portal.handlers;

import org.bukkit.Location;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.portal.PortalTeleportationHandler;

/**
 * Default handling for teleporting to a World
 */
public class PortalTeleportationHandlerDefault extends PortalTeleportationHandler {
    @Override
    public void handleWorld(World world) {
        if (plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
            // Perform the teleportation yay
            Location destinationLoc = WorldManager.getSpawnLocation(world);
            scheduleTeleportation(destinationLoc);
        }
    }
}

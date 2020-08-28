package com.bergerkiller.bukkit.mw.portal.handlers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.portal.PortalTeleportationHandler;

/**
 * Shows the end credits for the player, then teleports the player to
 * his last-known bed spawn or world spawn on the destination world.
 */
public class PortalTeleportationHandlerRespawn extends PortalTeleportationHandler {
    @Override
    public void handleWorld(World world) {
        if (plugin.getPortalTeleportationCooldown().tryEnterPortal(entity)) {
            if (entity instanceof Player) {
                Player player = (Player) entity;

                // Do the zzz
                Location bedSpawn = WorldManager.getPlayerRespawnPosition(player, world);
                if (bedSpawn != null) {
                    scheduleTeleportation(bedSpawn);
                    return;
                }
            }

            // Perform the teleportation yay
            Location destinationLoc = WorldManager.getSpawnLocation(world);
            scheduleTeleportation(destinationLoc);
        }
    }
}

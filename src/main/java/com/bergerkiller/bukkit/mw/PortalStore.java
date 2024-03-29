package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import com.bergerkiller.bukkit.mw.portal.PortalSignList;

public class PortalStore {

    /**
     * Checks whether a specific Portal has a name set, or whether the default name was used.
     * 
     * @param portalname to check
     * @param position of the portal
     * @return True if the portal can be teleported to, False if not
     */
    public static boolean canBeTeleportedTo(String portalname, Position position) {
        if (portalname.contains("_")) {
            StringBuilder posName = new StringBuilder(30);
            posName.append(position.getWorldName()).append('_');
            posName.append(position.getBlockX()).append('_');
            posName.append(position.getBlockY()).append('_');
            posName.append(position.getBlockZ());
            if (portalname.equalsIgnoreCase(posName.toString())) {
                return false;
            }
        }
        return true;
    }

    public static Location getPortalLocation(String portalname, String world, boolean spawnlocation) {
        return getPortalLocation(portalname, world, spawnlocation, null);
    }

    public static Location getPortalLocation(String portalname, String world, boolean spawnlocation, Entity entityBeingTeleported) {
        Location loc = getPortalLocation(portalname, world);
        if (spawnlocation) {
            return Util.spawnOffset(loc, entityBeingTeleported);
        } else {
            return loc;
        }
    }

    public static Location getPortalLocation(String portalname, String world) {
        if (portalname == null) {
            return null;
        }
        int dotindex = portalname.indexOf(".");
        if (dotindex != -1) {
            world = WorldManager.matchWorld(portalname.substring(0, dotindex));
            portalname = portalname.substring(dotindex + 1);
        }
        // Get a portal from a specified world
        if (world != null) {
            PortalSignList.PortalEntry portal = MyWorlds.plugin.getPortalSignList().findPortalOnWorld(portalname, world);
            if (portal != null) {
                Location loc = Util.getLocation(portal.position);
                if (loc != null) {
                    return loc;
                }
            }
        }
        // Get 'a' portal whose names matches and whose location is available
        for (PortalSignList.PortalEntry portal : MyWorlds.plugin.getPortalSignList().findPortalsRelaxed(portalname)) {
            Location loc = Util.getLocation(portal.position);
            if (loc != null) {
                return loc;
            }
        }
        return null;
    }

    public static String[] getPortals(Chunk c) {
        ArrayList<String> rval = new ArrayList<String>();
        for (String name : getPortals()) {
            Location loc = getPortalLocation(name, c.getWorld().getName());
            if (loc != null && loc.getWorld() == c.getWorld()) {
                if (c.getX() == (loc.getBlockX() >> 4)) {
                    if (c.getZ() == (loc.getBlockZ() >> 4)) {
                        rval.add(name);
                    }
                }
            }
        }
        return rval.toArray(new String[0]);
    }

    public static String[] getPortals(World w) {
        return MyWorlds.plugin.getPortalSignList().listPortalsOnWorld(w.getName()).stream()
                .filter(p -> canBeTeleportedTo(p.portalName, p.position))
                .map(p -> p.portalName)
                .toArray(String[]::new);
    }

    public static String[] getPortals() {
        return MyWorlds.plugin.getPortalSignList().listAllPortals().stream()
                .filter(p -> canBeTeleportedTo(p.portalName, p.position))
                .map(p -> p.portalName)
                .distinct()
                .toArray(String[]::new);
    }
}

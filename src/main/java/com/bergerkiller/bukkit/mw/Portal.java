package com.bergerkiller.bukkit.mw;

import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.EntityUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class Portal extends PortalStore {
    private String name;
    private String destination;
    private String destdisplayname;
    private Location location;

    /**
     * Gets the identifier name of this Portal
     * 
     * @return Portal identifier name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the Location of this Portal
     * 
     * @return Portal location
     */
    public Location getLocation() {
        return this.location;
    }

    /**
     * Gets the destination name of this Portal
     * 
     * @return Portal destination name
     */
    public String getDestinationName() {
        if (this.destination == null) {
            return null;
        } else {
            return this.destination.substring(this.destination.indexOf('.') + 1);
        }
    }

    /**
     * Gets the destination display name of this Portal
     * 
     * @return Portal destination display name
     */
    public String getDestinationDisplayName() {
        return this.destdisplayname;
    }

    /**
     * Gets the destination location of this Portal
     * 
     * @param player for which to get the destination. Null to ignore.
     * @return Portal destination
     */
    public Location getDestination() {
        return getDestination(null);
    }

    /**
     * Gets the destination location of this Portal
     * 
     * @param player for which to get the destination. Null to ignore.
     * @return Portal destination
     */
    public Location getDestination(Player player) {
        final String worldName = this.location == null ? null : this.location.getWorld().getName();
        Location loc = getPortalLocation(this.destination, worldName, true, player);
        if (loc == null) {
            String portalname = WorldManager.matchWorld(this.destination);
            World w = WorldManager.getWorld(portalname);
            if (w != null) {
                if (MyWorlds.portalToLastPosition && player != null) {
                    loc = WorldManager.getPlayerWorldSpawn(player, w, true).location;
                } else {
                    loc = WorldManager.getSpawnLocation(w);
                }
            }
        }
        return loc;
    }

    /**
     * Checks if this Portal has an available destination
     * 
     * @return True if it has a destination, False if not
     */
    public boolean hasDestination() {
        if (this.destination == null || this.destination.trim().isEmpty()) {
            return false;
        }
        return getDestination(null) != null;
    }

    /**
     * Gets a new non-existing Portal object that teleports to this Portal
     * 
     * @return new Portal with this Portal as destination
     */
    public Portal getOtherEnd() {
        Portal p = new Portal();
        p.name = "Unknown";
        p.destdisplayname = p.destination = this.name;
        p.location = null;
        return p;
    }

    /**
     * Teleports an Entity to the location of this Portal
     * 
     * @param entity to teleport
     * @return True if successful, False if not
     */
    public boolean teleportSelf(Entity entity) {
        if (entity instanceof Player) {
            MWListenerPost.setLastEntered((Player) entity, this.getOtherEnd().getDestinationDisplayName());
        }
        boolean rval = EntityUtil.teleport(entity, Util.spawnOffset(this.getLocation(), entity));
        return rval;
    }

    /**
     * Adds this Portal to the internal mapping
     */
    public void add() {
        MyWorlds.plugin.getPortalSignList().storePortal(name, new Position(location));
    }

    /**
     * Removes this Portal from the internal mapping
     * 
     * @return True if successful, False if not
     */
    public boolean remove() {
        return remove(this.name, this.location.getWorld().getName());
    }

    /**
     * Checks if this Portal is added to the internal mapping
     * 
     * @return True if it is added, False if not
     */
    public boolean isAdded() {
        return exists(this.location.getWorld().getName(), this.getName());
    }

    @Override
    public String toString() {
        return "Portal {name=" + getName() + ", loc=" + getLocation() + ", dest=" + (hasDestination() ? getDestinationName() : "None") + "}";
    }

    public static boolean exists(String world, String portalname) {
        return MyWorlds.plugin.getPortalSignList().findPortalOnWorld(portalname, world) != null;
    }

    public static boolean remove(String name, String world) {
        return MyWorlds.plugin.getPortalSignList().removePortal(name, world);
    }

    /**
     * Gets the nearest portal (sign) near a given point using the SEARCH_RADIUS as radius
     * 
     * @param middle point of the sphere to look in
     * @return Nearest portal, or null if none are found
     */
    public static Portal getNear(Location middle) {
        return getNear(middle, MyWorlds.maxPortalSignDistance);
    }

    /**
     * Gets the nearest portal (sign) in a given spherical area
     * 
     * @param middle point of the sphere to look in
     * @param radius of the sphere to look in
     * @return Nearest portal, or null if none are found
     */
    public static Portal getNear(Location middle, double radius) {
        Portal p = null;
        String worldname = middle.getWorld().getName();
        for (Map.Entry<String, Position> pos : MyWorlds.plugin.getPortalSignList().listPortalsOnWorld(worldname)) {
            Location ploc = Util.getLocation(pos.getValue());
            String portalname = pos.getKey();
            if (ploc != null && ploc.getWorld() == middle.getWorld()) {
                double distance = ploc.distance(middle);
                if (distance <= radius) {
                    Portal newp = Portal.get(ploc);
                    if (newp != null) {
                        p = newp;
                        radius = distance;
                    } else if (ploc.getWorld().isChunkLoaded(ploc.getBlockX() >> 4, ploc.getBlockZ() >> 4)) {
                        //In loaded chunk and NOT found!
                        //Remove it
                        MyWorlds.plugin.getPortalSignList().removePortal(portalname, worldname);
                        MyWorlds.plugin.log(Level.WARNING, "Removed portal '" + portalname + "' because it is no longer there!");
                        //End the loop and call the function again
                        return getNear(middle, radius);
                    }
                }
            }
        }
        return p;
    }

    public static Portal get(String name) {
        return get(getPortalLocation(name, null));
    }

    public static Portal get(Location signloc) {
        if (signloc == null) return null;
        return get(signloc.getBlock(), true);
    }

    public static Portal get(Block signblock, boolean loadchunk) {
        if (loadchunk) {
            signblock.getChunk();
        } else if (!WorldUtil.isLoaded(signblock)) {
            return null;
        }
        if (signblock.getState() instanceof Sign) {
            return get(signblock, ((Sign) signblock.getState()).getLines());
        }
        return null;
    }

    public static Portal get(Block signblock, String[] lines) {
        if (signblock.getState() instanceof Sign && lines[0].equalsIgnoreCase("[portal]")) {
            Portal p = new Portal();
            // Read name, if none set, use portal location as name
            p.name = Util.filterPortalName(lines[1]);
            if (LogicUtil.nullOrEmpty(p.name)) {
                p.name = StringUtil.blockToString(signblock);
            }
            // Read destination, if none set, set to null so it's clear there is no destination set
            p.destination = Util.filterPortalName(lines[2]);
            if (p.destination.isEmpty()) {
                p.destination = null;
            }
            // Read destination name, if none set, use destination instead
            if (lines[3].isEmpty()) {
                p.destdisplayname = p.getDestinationName();
            } else {
                p.destdisplayname = lines[3];
            }
            // Set portal locatiol using sign location and orientation
            p.location = signblock.getLocation();
            p.location.setX(p.location.getBlockX() + 0.5);
            p.location.setZ(p.location.getBlockZ() + 0.5);

            float yaw = 0;
            MaterialData data = BlockUtil.getData(signblock);
            if (data instanceof Directional) {
                yaw = FaceUtil.faceToYaw(((Directional) data).getFacing()) + 90;
            }
            p.location.setYaw(yaw);
            return p;
        }
        return null;
    }

    /**
     * Gets whether teleportation to another location is at all possible for an entity to do.
     * Checks world configuration and permissions, if the entity is a player.
     * 
     * @param e Entity to teleport
     * @param dest Destination Location to teleport to
     * @return True if teleportation is (possibly) allowed
     */
    public static boolean canTeleportEntityTo(Entity e, Location dest) {
        // Check permissions for players
        if (e instanceof Player) {
            return MWListenerPost.handleTeleportPermission((Player) e, dest);
        }
        // Non-player entity was denied from teleporting there because of spawn control
        return !WorldConfig.get(dest).spawnControl.isDenied(e);
    }
}

package com.bergerkiller.bukkit.mw;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import com.bergerkiller.bukkit.common.utils.CommonUtil;
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
        Location loc = getPortalLocation(this.destination, worldName, true);
        if (loc == null) {
            String portalname = WorldManager.matchWorld(this.destination);
            World w = WorldManager.getWorld(portalname);
            if (w != null) {
                if (MyWorlds.portalToLastPosition && player != null) {
                    loc = WorldManager.getPlayerWorldSpawn(player, w);
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
            MWListenerPost.setLastEntered((Player) entity, this.getOtherEnd());
        }
        boolean rval = EntityUtil.teleport(entity, Util.spawnOffset(this.getLocation()));
        return rval;
    }

    /**
     * Adds this Portal to the internal mapping
     */
    public void add() {
        getPortalLocations(location.getWorld().getName()).put(name, new Position(location));
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
        return getPortalLocations(this.location.getWorld().getName()).containsKey(this.getName());
    }

    @Override
    public String toString() {
        return "Portal {name=" + getName() + ", loc=" + getLocation() + ", dest=" + (hasDestination() ? getDestinationName() : "None") + "}";
    }

    public static boolean exists(String world, String portalname) {
        return getPortalLocations(world).containsKey(portalname);
    }

    public static boolean remove(String name, String world) {
        return getPortalLocations(world).remove(name) != null;
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
        HashMap<String, Position> positions = getPortalLocations(middle.getWorld().getName());
        for (Map.Entry<String, Position> pos : positions.entrySet()) {
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
                        positions.remove(portalname);
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
        return get(signloc.getBlock(), false);
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
            MaterialData data = signblock.getState().getData();
            float yaw = 0;
            if (data instanceof Directional) {
                yaw = FaceUtil.faceToYaw(((Directional) data).getFacing()) + 90;
            }
            p.location.setYaw(yaw);
            return p;
        }
        return null;
    }

    /**
     * Handles an entity entering a certain portal block.
     * If this method returns False, the caller should set the event to cancelled.
     * The destination Location and travel agent are updated by this method if True
     * is returned. If the Entity is a Player, messages are sent when teleportation
     * failed.
     * 
     * @param event of the entering
     * @param portalType of the portal
     * @return True if a new destination was set, False if not
     */
    public static boolean handlePortalEnter(EntityPortalEvent event, PortalType portalType) {
        // First, see whether a portal or default world will be used
        Entity entity = event.getEntity();
        Portal enteredPortal = getNear(entity.getLocation());
        World enteredWorld = null;
        if (enteredPortal == null) {
            // Default portals
            String def = null;
            if (portalType == PortalType.NETHER) {
                def = WorldConfig.get(entity).getNetherPortal();
            } else if (portalType == PortalType.END) {
                def = WorldConfig.get(entity).getEnderPortal();
            }
            if (def != null) {
                enteredPortal = get(getPortalLocation(def, entity.getWorld().getName()));
                if (enteredPortal != null) {
                    // Teleport to a specific portal - change perspective
                    enteredPortal = enteredPortal.getOtherEnd();
                } else {
                    // Resort back to a potential default world
                    enteredWorld = WorldManager.getWorld(def);
                }
            }
        }
        // Proceed to further handle this request
        Location destinationLoc = null;
        boolean useTravelAgent = false;

        // Further handle portal entering
        if (enteredPortal != null) {
            Player p = CommonUtil.tryCast(entity, Player.class);
            destinationLoc = enteredPortal.getDestination(p);
            if (destinationLoc == null) {
                String name = enteredPortal.getDestinationName();
                if (name != null && entity instanceof Player) {
                    // Show message indicating the destination is unavailable
                    Localization.PORTAL_NOTFOUND.message((Player) entity, name);
                    // Return here to avoid an additional 'no destination' message later on
                    return false;
                }
            } else if (entity instanceof Player) {
                // For later on: set up the right portal for permissions and messages
                destinationLoc = destinationLoc.clone().add(0.0, 1.0, 0.0); // Fix
                MWListenerPost.setLastEntered((Player) entity, enteredPortal);
            }
        }

        // Check for teleporting to the last-known position
        if (enteredWorld != null &&
            MyWorlds.allowPersonalPortals &&
            MyWorlds.portalToLastPositionPersonal &&
            entity instanceof Player &&
            WorldManager.hasLastKnownPosition((Player) entity, enteredWorld))
        {
            destinationLoc = WorldManager.getPlayerWorldSpawn((Player) entity, enteredWorld);
            enteredWorld = null;
        }

        // Further handle teleportation to worlds
        if (enteredWorld != null) {
            if (MyWorlds.allowPersonalPortals) {
                // Personal portals - which means a portal may have to be created on the other end
                // To find out where to place this portal, compare the from and to environments
                Environment oldEnvironment = entity.getWorld().getEnvironment();
                Environment newEnvironment = enteredWorld.getEnvironment();
                if (newEnvironment == Environment.THE_END) {
                    // Always use this location of the world as destination
                    // Anything else will cause internal logic to break
                    // SERIOUSLY ANYTHING ELSE WILL COMPLETELY BREAK THIS!!!
                    destinationLoc = new Location(enteredWorld, 100, 50, 0);
                    useTravelAgent = true;
                } else if (oldEnvironment == Environment.THE_END) {
                    // No special portal type or anything is used
                    // Instead, teleport to a personal bed or otherwise world spawn
                    if (entity instanceof Player) {
                        destinationLoc = ((Player) entity).getBedSpawnLocation();
                    }
                } else {
                    // Find out what location to teleport to
                    // Use source block as the location to search from
                    double blockRatio = 1.0;
                    if (oldEnvironment != newEnvironment) {
                        if (newEnvironment == Environment.NETHER) {
                            blockRatio = 0.125;
                        } else if (oldEnvironment == Environment.NETHER) {
                            blockRatio = 8.0;
                        }
                    }
                    Location start = entity.getLocation();
                    Location end = new Location(enteredWorld, blockRatio * start.getX(), start.getY(), blockRatio * start.getZ());
                    Block destB = end.getBlock();
                    // Figure out the best yaw to use here by checking for air blocks
                    float yaw = 0.0f;
                    for (BlockFace face : FaceUtil.AXIS) {
                        if (Util.IS_AIR.get(destB.getRelative(face))) {
                            yaw = FaceUtil.faceToYaw(face) + 90f;
                            break;
                        }
                    }
                    destinationLoc = new Location(destB.getWorld(), destB.getX() + 0.5, destB.getY(), destB.getZ() + 0.5, yaw, 0.0f);
                    useTravelAgent = true;
                }
            }
            // No destination found or set, resort back to using world spawn
            if (destinationLoc == null) {
                if (entity instanceof Player) {
                    destinationLoc = WorldManager.getPlayerWorldSpawn((Player) entity, enteredWorld);
                } else {
                    destinationLoc = WorldManager.getSpawnLocation(enteredWorld);
                }
            }
        }

        if (destinationLoc == null) {
            // No destination available
            // Send a missing destination message for non-water portals
            if (entity instanceof Player && portalType != PortalType.WATER) {
                Localization.PORTAL_NODESTINATION.message((Player) entity);
            }
        } else if (canTeleportTo(entity, destinationLoc)) {
            // Successful teleport
            event.useTravelAgent(useTravelAgent);
            event.setTo(destinationLoc);
            return true;
        }
        return false;
    }

    private static boolean canTeleportTo(Entity e, Location dest) {
        // Check permissions for players
        if (e instanceof Player) {
            return MWListenerPost.handleTeleportPermission((Player) e, dest);
        }
        // Non-player entity was denied from teleporting there because of spawn control
        return !WorldConfig.get(dest).spawnControl.isDenied(e);
    }
}

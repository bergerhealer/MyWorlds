package com.bergerkiller.bukkit.mw.portal;

import java.util.function.Supplier;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.WorldManager;

import com.bergerkiller.bukkit.mw.portal.handlers.*;

/**
 * Behavior of portals on a world. Applies extra rules to the configured
 * world or portal name.
 */
public enum PortalMode {
    /**
     * Teleports to the world spawn/portal position by this name,
     * and does nothing more
     */
    DEFAULT("Teleport to", PortalTeleportationHandlerDefault::new),
    /**
     * Teleports the player to the bed in any world that shares inventory state
     * with the destination world. If the player last 'slept' in the nether
     * and is asked to respawn, the player is teleported to the nether even
     * if the destination was overworld.<br>
     * <br>
     * If the player has no bed spawn, the player is teleported to the world spawn
     * point on the destination world. If a portal name is specified, the player
     * is teleported to the portal.
     */
    RESPAWN("Respawn at the player bed, or world spawn of", () -> new PortalTeleportationHandlerRespawn(false)),
    /**
     * Teleports the player to a bed spawn, or world spawn, on the
     * configured world. If the player bed is on a different world it is
     * not used. If a portal name is specified, the player
     * is teleported to the portal.
     */
    RESPAWN_ON("Respawn at the player bed (if on the destination world), or world spawn of",
            () -> new PortalTeleportationHandlerRespawn(true)),
    /**
     * Checks the world data what the last world was in the world group
     * of the destination world, and teleports the player to that world
     * at the last known position the player had.
     */
    REJOIN("Rejoin at last known position of", PortalTeleportationHandlerRejoin::new),
    /**
     * Transforms the current position of the player to a new position
     * on another world, then finds or builds a nether portal there.
     * The player is then teleported to that portal.
     */
    NETHER_LINK("Nether-link to", PortalTeleportationHandlerNetherLink::new),
    /**
     * Prepares an obsidian platform at fixed coordinates of the destination
     * world and teleports players to it
     */
    END_PLATFORM("End platform on", PortalTeleportationHandlerEndLink::new),
    /**
     * Lets Vanilla Minecraft deal with all logic, unless a [portal] sign is put
     * nearby. Will allow the normal portal events to complete.
     */
    VANILLA("Vanilla Portal Behavior", PortalTeleportationHandlerDefault::new);

    private final String _infoPrefix;
    private final Supplier<PortalTeleportationHandler> _handlerSupplier;

    private PortalMode(String infoPrefix, Supplier<PortalTeleportationHandler> handlerSupplier) {
        _infoPrefix = infoPrefix;
        _handlerSupplier = handlerSupplier;
    }

    /**
     * Creates a new teleportation handler for this mode.
     * All custom behavior is implemented here.
     * 
     * @return new teleportation handler
     */
    public PortalTeleportationHandler createTeleportationHandler() {
        return this._handlerSupplier.get();
    }

    /**
     * Gets an appropriate info string for the given destination, and this mode
     * 
     * @param destination
     * @param displayName Alias display name for destination, empty string does nothing
     * @return info string
     */
    public String getInfoString(String destination, String displayName) {
        String aliasStr = displayName.isEmpty() ? "" : (ChatColor.YELLOW + " (" + displayName + ")");

        if (Portal.getPortalLocation(destination, null) != null) {
            return _infoPrefix + " portal " + ChatColor.WHITE + "'" + destination + "'" + aliasStr;
        }

        String worldName = WorldManager.matchWorld(destination);
        if (worldName != null) {
            return _infoPrefix + " world " + ChatColor.WHITE + "'" + worldName + "'" + aliasStr;
        }

        return _infoPrefix + ChatColor.RED + " [Invalid] " + destination + aliasStr;
    }

    public static PortalMode fromString(String name) {
        if (name == null) {
            return DEFAULT;
        } else if (name.equalsIgnoreCase("end_link")) {
            return END_PLATFORM;
        } else {
            return ParseUtil.parseEnum(name, DEFAULT);
        }
    }
}

package com.bergerkiller.bukkit.mw.portal;

import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.WorldConfig;

import net.md_5.bungee.api.ChatColor;

/**
 * Stores the rules for a single portal destination.
 * This is used for default portal rules of a world,
 * and is parsed from signs put nearby portals.
 */
public class PortalDestination {
    private PortalMode _mode = PortalMode.DEFAULT;
    private String _name = "";
    private String _display = "";
    private boolean _playersOnly = false;
    private boolean _teleportMounts = true;
    private boolean _lastPosition = false;
    private boolean _showCredits = false;
    private boolean _nonPlayersCreatePortals = true;

    public void setMode(PortalMode mode) {
        this._mode = mode;
    }

    public PortalMode getMode() {
        return this._mode;
    }

    public void setName(String name) {
        this._name = (name == null) ? "" : name;
    }

    public String getName() {
        return this._name;
    }

    public void setDisplayName(String displayName) {
        this._display = (displayName == null) ? "" : displayName;
    }

    public String getDisplayName() {
        return this._display;
    }

    public void setPlayersOnly(boolean playersOnly) {
        this._playersOnly = playersOnly;
    }

    public boolean isPlayersOnly() {
        return this._playersOnly;
    }

    public void setCanTeleportMounts(boolean teleportMounts) {
        this._teleportMounts = teleportMounts;
    }

    public boolean canTeleportMounts() {
        return this._teleportMounts;
    }

    public void setTeleportToLastPosition(boolean lastPosition) {
        this._lastPosition = lastPosition;
    }

    public boolean isTeleportToLastPosition() {
        return this._lastPosition;
    }

    public void setShowCredits(boolean showCredits) {
        this._showCredits = showCredits;
    }

    public boolean isShowCredits() {
        return this._showCredits;
    }

    public void setCanNonPlayersCreatePortals(boolean canThey) {
        this._nonPlayersCreatePortals = canThey;
    }

    public boolean canNonPlayersCreatePortals() {
        return this._nonPlayersCreatePortals;
    }

    public String getInfoString() {
        if (this._name.isEmpty()) {
            return ChatColor.RED + "Disabled";
        }
        String s = ChatColor.YELLOW + this._mode.getInfoString(this._name, this._display);
        if (this.isPlayersOnly()) {
            s += ChatColor.RED + " [Players only]";
        }
        if (this.canNonPlayersCreatePortals()) {
            s += ChatColor.DARK_GREEN + " [Non-players create portals]";
        }
        if (this.isTeleportToLastPosition()) {
            s += ChatColor.BLUE + " [Last Position]";
        }
        if (this.canTeleportMounts()) {
            s += ChatColor.BLUE + " [Teleport Mounts]";
        }
        if (this.isShowCredits()) {
            s += ChatColor.BLUE + " [Show Credits]";
        }
        return s;
    }

    @Override
    public PortalDestination clone() {
        PortalDestination clone = new PortalDestination();
        clone.setMode(this.getMode());
        clone.setName(this.getName());
        clone.setDisplayName(this.getDisplayName());
        clone.setCanNonPlayersCreatePortals(this.canNonPlayersCreatePortals());
        clone.setCanTeleportMounts(this.canTeleportMounts());
        clone.setShowCredits(this.isShowCredits());
        clone.setPlayersOnly(this.isPlayersOnly());
        clone.setTeleportToLastPosition(this.isTeleportToLastPosition());
        return clone;
    }

    @Override
    public String toString() {
        return ChatColor.stripColor(this.getInfoString());
    }

    /**
     * Parses portal destination information from a configuration section
     * 
     * @param config
     * @param key
     * @return portal destination, null if none is stored
     */
    public static PortalDestination fromConfig(ConfigurationNode config, String key) {
        Object atKey = config.get(key);
        if (atKey instanceof String) {
            // Default behavior with a name
            PortalDestination result = new PortalDestination();
            result.setMode(PortalMode.DEFAULT);
            result.setName((String) atKey);
            result.setDisplayName(null);
            result.setPlayersOnly(false);
            result.setCanTeleportMounts(true);
            result.setTeleportToLastPosition(false);
            result.setShowCredits(false);
            result.setCanNonPlayersCreatePortals(true);
            return result;
        } else if (atKey instanceof ConfigurationNode) {
            // From yaml configuration, specifying various things
            ConfigurationNode node = (ConfigurationNode) atKey;
            PortalDestination result = new PortalDestination();
            result.setMode(PortalMode.fromString(node.get("mode", "default")));
            result.setName(node.get("name", String.class, null));
            result.setDisplayName(node.get("display", String.class, null));
            result.setPlayersOnly(node.get("playersOnly", false));
            result.setCanTeleportMounts(node.get("teleportMounts", true));
            result.setTeleportToLastPosition(node.get("teleportToLastPosition", false));
            result.setShowCredits(node.get("showCredits", false));
            result.setCanNonPlayersCreatePortals(node.get("nonPlayersCreatePortals", true));
            return result;
        } else {
            // None stored
            return null;
        }
    }

    /**
     * Saves a portal destination to configuration
     * 
     * @param destination
     * @param config
     * @param key
     */
    public static void toConfig(PortalDestination destination, ConfigurationNode config, String key) {
        if (destination == null || destination.getName().isEmpty()) {
            config.remove(key);
        } else {
            ConfigurationNode node = config.getNode(key);
            node.set("mode", destination.getMode());
            node.set("name", destination.getName());
            node.set("display", destination.getDisplayName());
            node.set("playersOnly", destination.isPlayersOnly());
            node.set("teleportMounts", destination.canTeleportMounts());
            node.set("teleportToLastPosition", destination.isTeleportToLastPosition());
            node.set("showCredits", destination.isShowCredits());
            node.set("nonPlayersCreatePortals", destination.canNonPlayersCreatePortals());
        }
    }

    /**
     * Finds the desired portal destination when entering a physical
     * portal. If a sign is nearby configuring these rules, then
     * the destination configured on the sign is returned.
     * Otherwise, the default rules for the world are used.
     * Permissions are not handled here!
     * 
     * @param portalType Type of portal portalBlock is
     * @param portalBlock Block of the portal touched by the player
     * @return Desired portal destination, null if none is handled by MyWorlds
     */
    public static PortalDestination.FindResults findFromPortal(PortalType portalType, Block portalBlock) {
        // Check for nearby signs altering default behavior
        Portal portalNearby = Portal.getNear(portalBlock.getLocation());
        if (portalNearby != null) {
            //TODO: allow PortalMode to be used in the Portal itself
            PortalDestination portalDestination = new PortalDestination();
            portalDestination.setMode(PortalMode.DEFAULT);
            portalDestination.setName(portalNearby.getDestinationName());
            if (portalNearby.getDestinationDisplayName().equals(portalDestination.getName())) {
                portalDestination.setDisplayName("");
            } else {
                portalDestination.setDisplayName(portalNearby.getDestinationDisplayName());
            }
            portalDestination.setPlayersOnly(true);
            portalDestination.setCanTeleportMounts(true);
            portalDestination.setTeleportToLastPosition(MyWorlds.portalToLastPosition);
            return new FindResults(portalDestination, portalNearby.getName());
        }

        // Default behavior
        return new FindResults(WorldConfig.get(portalBlock).getDefaultDestination(portalType), null);
    }

    public static class FindResults {
        private final PortalDestination destination;
        private final String portalName;

        public FindResults(PortalDestination destination, String portalName) {
            this.destination = destination;
            this.portalName = portalName;
        }

        /**
         * Gets whether results are available
         * 
         * @return True if available
         */
        public boolean isAvailable() {
            return this.destination != null;
        }

        /**
         * Gets the portal destination that was found.
         * Null if none was found.
         * 
         * @return destination
         */
        public PortalDestination getDestination() {
            return this.destination;
        }

        /**
         * Gets whether a nearby portal sign was used to find
         * this destination
         * 
         * @return True if a portal was used
         */
        public boolean isFromPortal() {
            return this.portalName != null;
        }

        /**
         * If this destination was found because a nearby portal
         * sign was found, then this returns the name of that portal.
         * 
         * @return portal name
         */
        public String getPortalName() {
            return this.portalName;
        }
    }
}

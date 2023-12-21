package com.bergerkiller.bukkit.mw.portal;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.WorldConfig;

import java.util.Optional;

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
    private boolean _enableActivation = true;
    private boolean _autoDetectDestination = false;

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

    /**
     * Gets whether looking up portals by {@link #getName()} is ignored. This is the
     * case when {@link #getMode()} is set to non-default, and a world exists matching
     * the name.
     *
     * @return True if portal lookup should be ignored
     */
    public boolean isPortalLookupIgnored() {
        if (getMode() == PortalMode.DEFAULT) {
            return false;
        }

        WorldConfig wc = WorldConfig.getIfExists(getName());
        if (wc == null || !wc.isLoaded()) {
            return false;
        }

        // World exists and is loaded.
        return true;
    }

    /**
     * Gets whether MyWorlds should try and find a matching world to create a nether
     * or end link automatically. If a destination is set manually, then auto-detection
     * is turned off.
     *
     * @return True if auto-detection can occur right now
     */
    public boolean canAutoDetect() {
        return _autoDetectDestination && (_name == null || _name.isEmpty());
    }

    /**
     * Controls {@link #canAutoDetect()}
     *
     * @return True if auto-detection is enabled
     */
    public boolean isAutoDetectEnabled() {
        return _autoDetectDestination;
    }

    public void setAutoDetectEnabled(boolean enabled) {
        _autoDetectDestination = enabled;
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

    public void setActivationEnabled(boolean activate) {
        this._enableActivation = activate;
    }

    /**
     * Gets whether the portal can light automatically by placing fire in the obsidian
     * frame (nether portal) or eyes of ender in the end gateways (end portal)
     *
     * @return True if activation of the portal is enabled
     */
    public boolean isActivationEnabled() {
        return this._enableActivation;
    }

    public String getInfoString() {
        if (this._mode == PortalMode.VANILLA) {
            return ChatColor.YELLOW + "Vanilla Portal Behavior";
        }
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
        if (!this.isActivationEnabled()) {
            s += ChatColor.RED + " [Cannot be lit]";
        }
        return s;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof PortalDestination) {
            PortalDestination other = (PortalDestination) o;
            return _mode == other._mode &&
                    _name.equals(other._name) &&
                    _display.equals(other._display) &&
                    _playersOnly == other._playersOnly &&
                    _teleportMounts == other._teleportMounts &&
                    _lastPosition == other._lastPosition &&
                    _showCredits == other._showCredits &&
                    _nonPlayersCreatePortals == other._nonPlayersCreatePortals &&
                    _enableActivation == other._enableActivation &&
                    _autoDetectDestination == other._autoDetectDestination;
        } else {
            return false;
        }
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
        clone.setActivationEnabled(this.isActivationEnabled());
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
            result.setActivationEnabled(true);
            result.setAutoDetectEnabled(false);
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
            result.setActivationEnabled(node.get("activationEnabled", true));
            result.setAutoDetectEnabled(node.get("autoDetect", false));
            return result;
        } else {
            // None stored. Default unset configuration with autodetect on
            PortalDestination result = new PortalDestination();
            result.setMode(PortalMode.DEFAULT);
            result.setName("");
            result.setDisplayName(null);
            result.setPlayersOnly(false);
            result.setCanTeleportMounts(true);
            result.setTeleportToLastPosition(false);
            result.setShowCredits(false);
            result.setCanNonPlayersCreatePortals(true);
            result.setActivationEnabled(true);
            result.setAutoDetectEnabled(true);
            return result;
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
        if (destination == null) {
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
            node.set("activationEnabled", destination.isActivationEnabled());
            node.set("autoDetect", destination.isAutoDetectEnabled());
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
            portalDestination.setMode(portalNearby.isRejoin() ? PortalMode.REJOIN : PortalMode.DEFAULT);
            portalDestination.setName(portalNearby.getDestinationName());
            if (portalNearby.getDestinationDisplayName().equals(portalDestination.getName())) {
                portalDestination.setDisplayName("");
            } else {
                portalDestination.setDisplayName(portalNearby.getDestinationDisplayName());
            }

            // Default options for portals without name / not in list
            portalDestination.setPlayersOnly(!MyWorlds.portalSignsTeleportMobs);
            portalDestination.setCanTeleportMounts(true);

            // Load options from portals.txt if set
            if (!LogicUtil.nullOrEmpty(portalNearby.getName())) {
                Location loc = portalNearby.getLocation();
                if (loc != null) {
                    PortalSignList.PortalEntry e = MyWorlds.plugin.getPortalSignList().findPortalOnWorld(
                            portalNearby.getName(), loc.getWorld().getName());
                    if (e != null) {
                        // Load options
                        portalDestination.setPlayersOnly(e.getOption("playersonly", portalDestination.isPlayersOnly()));
                        portalDestination.setCanTeleportMounts(e.getOption("teleportmounts", portalDestination.canTeleportMounts()));
                    }
                }
            }

            if (!portalNearby.isRejoin() && MyWorlds.portalToLastPosition) {
                // Find destination location. If that world has rememberLastPos to true, use it.
                Location dest = portalNearby.getDestination();
                if (dest != null && WorldConfig.get(dest.getWorld()).rememberLastPlayerPosition) {
                    portalDestination.setTeleportToLastPosition(true);
                }
            }
            return new FindResults(portalDestination, Optional.of(portalNearby));
        }

        // Default behavior
        return new FindResults(WorldConfig.get(portalBlock).getDefaultDestination(portalType), Optional.empty());
    }

    public static class FindResults {
        private final PortalDestination destination;
        private final Optional<Portal> portal;

        public FindResults(PortalDestination destination, Optional<Portal> portal) {
            this.destination = destination;
            this.portal = portal;
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
         * Gets the portal sign details, if a portal sign was near the portal
         *
         * @return Portal information
         */
        public Optional<Portal> getPortal() {
            return portal;
        }

        /**
         * Gets whether a nearby portal sign was used to find
         * this destination
         * 
         * @return True if a portal was used
         */
        public boolean isFromPortal() {
            return portal.isPresent();
        }

        /**
         * If this destination was found because a nearby portal
         * sign was found, then this returns the name of that portal.
         * 
         * @return portal name, null if no portal sign was near
         */
        public String getPortalName() {
            return portal.map(Portal::getName).orElse(null);
        }
    }
}

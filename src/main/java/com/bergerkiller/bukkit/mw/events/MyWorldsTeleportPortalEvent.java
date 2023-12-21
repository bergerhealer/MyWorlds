package com.bergerkiller.bukkit.mw.events;

import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.portal.PortalDestination;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;

/**
 * Event fired when a MyWorlds-managed portal teleports a player
 */
public class MyWorldsTeleportPortalEvent extends MyWorldsTeleportEvent {
    private final PortalDestination toPortalDestination;
    private final PortalType portalType;
    private final Optional<Portal> fromPortal;

    public MyWorldsTeleportPortalEvent(Entity entity, PortalType portalType, Location to, PortalDestination toPortalDestination, Optional<Portal> fromPortal, Optional<String> toPortalName) {
        super(entity, Cause.PORTAL, to, toPortalName);
        this.toPortalDestination = toPortalDestination;
        this.portalType = portalType;
        this.fromPortal = fromPortal;
    }

    /**
     * Gets the type of portal that was activated that resulted in this teleportation
     *
     * @return Portal Type
     */
    public PortalType getPortalType() {
        return portalType;
    }

    /**
     * Gets the details about the destination of the portal. This includes teleportation
     * options and rules set for this type of destination, such as whether to rejoin
     * players. This information is always available.
     *
     * @return Portal destination rules.
     */
    public PortalDestination getToPortalDestination() {
        return toPortalDestination;
    }

    /**
     * If the player entered a Portal managed by a MyWorlds portal sign, this is the Portal
     * information of that portal.
     *
     * @return Entered portal information. Empty if not available.
     */
    public Optional<Portal> getFromPortal() {
        return fromPortal;
    }
}

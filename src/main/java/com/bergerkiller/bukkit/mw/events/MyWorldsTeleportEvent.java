package com.bergerkiller.bukkit.mw.events;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

import java.util.Optional;

/**
 * Event fired by MyWorlds before an entity is teleported in some way by MyWorlds.
 * Can be one of the instances based on {@link Cause}
 */
public class MyWorldsTeleportEvent extends EntityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Cause cause;
    private Location to;
    private final Optional<String> toPortalName;
    private boolean cancelled;

    public MyWorldsTeleportEvent(Entity entity, Cause cause, Location to, Optional<String> toPortalName) {
        super(entity);
        this.cause = cause;
        this.to = to;
        this.toPortalName = toPortalName;
    }

    /**
     * Gets the cause of the teleportation.
     *
     * @return Cause
     * @see Cause
     */
    public Cause getCause() {
        return cause;
    }

    /**
     * Gets the name of the portal to which is teleported. Does not change when {@link #setTo(Location)}
     * is called. Empty when teleporting to a world (spawn or last location)
     *
     * @return Destination portal name
     */
    public Optional<String> getToPortalName() {
        return toPortalName;
    }

    /**
     * Gets the location to which the player will be teleported
     *
     * @return To location
     */
    public Location getTo() {
        return to;
    }

    /**
     * Sets the location to which the player will be teleported.
     * Changes the destination.
     *
     * @param to New to location
     */
    public void setTo(Location to) {
        this.to = to;
    }

    /**
     * Gets whether this event is for a Player. If so, {@link #getEntity()} can be cast to Player
     *
     * @return True if this is a teleportation of a Player
     */
    public boolean isPlayer() {
        return getEntity() instanceof Player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled || to == null;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Cause of the teleportation. Directly controls what instance this event
     * class is.
     */
    public enum Cause {
        /** Event is a {@link MyWorldsTeleportCommandEvent} */
        COMMAND,
        /** Event is a {@link MyWorldsTeleportPortalEvent} */
        PORTAL
    }

    /**
     * Creates a teleport event specific to the type of trigger that caused it.
     * The returned event is automatically called and cancel logic of it handled.
     */
    @FunctionalInterface
    public interface Factory {
        MyWorldsTeleportEvent create(Entity entity, Location to);
    }
}

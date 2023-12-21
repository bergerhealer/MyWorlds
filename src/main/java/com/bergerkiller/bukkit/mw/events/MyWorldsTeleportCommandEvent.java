package com.bergerkiller.bukkit.mw.events;

import com.bergerkiller.bukkit.mw.Portal;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;

/**
 * Event fired when a MyWorlds command causes a player teleportation
 */
public class MyWorldsTeleportCommandEvent extends MyWorldsTeleportEvent {
    private final CommandType commandType;

    public MyWorldsTeleportCommandEvent(Entity entity, CommandType commandType, Location to) {
        super(entity, Cause.COMMAND, to, Optional.empty());
        this.commandType = commandType;
    }

    public MyWorldsTeleportCommandEvent(Entity entity, CommandType commandType, Location to, String toPortalName) {
        super(entity, Cause.COMMAND, to, Optional.ofNullable(toPortalName));
        this.commandType = commandType;
    }

    /**
     * Gets the type of command that was executed
     *
     * @return Command Type
     */
    public CommandType getCommandType() {
        return commandType;
    }

    /**
     * Type of MyWorlds command that was executed
     */
    public enum CommandType {
        /** Use of /tpp to a portal name */
        TELEPORT_PORTAL,
        /** Use of /tpp to a world name */
        TELEPORT_WORLD,
        /** Use of /mw rejoin */
        REJOIN,
        /** Use of /mw lastposition tp (or clicking on an item) */
        LAST_POSITION,
        /** Use of /mw spawn */
        SPAWN,
        /** Use of /mw evacuate to teleport all players out of the world */
        EVACUATE
    }
}

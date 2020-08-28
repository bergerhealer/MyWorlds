package com.bergerkiller.bukkit.mw.commands;

import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.WorldMode;
import com.bergerkiller.bukkit.mw.portal.PortalDestination;
import com.bergerkiller.bukkit.mw.portal.PortalMode;

public class WorldSetDefaultPortal extends Command {
    private final PortalType type;

    public WorldSetDefaultPortal(PortalType type) {
        super(Permission.COMMAND_SETPORTAL, type.getCommandNode());
        this.type = type;
    }

    @Override
    public void execute() {
        // Parse all possible arguments, with a name= prefix for special ones
        // If no prefix specified, only parses the name
        PortalMode new_mode = null;
        String new_name = null;
        String new_display = null;
        Boolean new_playersOnly = null;
        Boolean new_teleportToLastPosition = null;
        Boolean new_teleportMounts = null;
        Boolean new_showCredits = null;
        boolean parsedAnArg = false;
        while (args.length > 0) {
            String arg = args[0];
            int checkNameIdx = arg.indexOf('=');
            String name_key = (checkNameIdx == -1) ? "" : arg.substring(0, checkNameIdx).toLowerCase(Locale.ENGLISH);
            String name_val = (checkNameIdx == -1) ? "" : arg.substring(checkNameIdx+1);

            if (name_key.equals("mode")) {
                new_mode = ParseUtil.parseEnum(name_val, PortalMode.DEFAULT);
                this.removeArg(0);
                parsedAnArg = true;
            } else if (name_key.equals("name")) {
                new_name = name_val;
                this.removeArg(0);
                parsedAnArg = true;
            } else if (name_key.equals("display")) {
                new_display = name_val;
                this.removeArg(0);
                parsedAnArg = true;
            } else if (name_key.equals("playeronly") ||
                       name_key.equals("playersonly") ||
                       name_key.equals("onlyplayer") ||
                       name_key.equals("onlyplayers"))
            {
                new_playersOnly = ParseUtil.parseBool(name_val, Boolean.FALSE);
                this.removeArg(0);
                parsedAnArg = true;
            } else if (name_key.equals("lastposition")) {
                new_teleportToLastPosition = ParseUtil.parseBool(name_val, Boolean.FALSE);
                this.removeArg(0);
                parsedAnArg = true;
            } else if (name_key.equals("mounts") || name_key.equals("teleportmounts")) {
                new_teleportMounts = ParseUtil.parseBool(name_val, Boolean.FALSE);
                this.removeArg(0);
                parsedAnArg = true;
            } else if (name_key.equals("showcredits")) {
                new_showCredits = ParseUtil.parseBool(name_val, Boolean.FALSE);
                this.removeArg(0);
                parsedAnArg = true;
            } else if (!parsedAnArg) {
                new_name = arg;
                this.removeArg(0);
                parsedAnArg = true;
            } else {
                break;
            }
        }

        this.genWorldname(0);
        if (this.handleWorld()) {
            WorldConfig config = WorldConfig.get(worldname);
            PortalDestination oldDest = config.getDefaultDestination(this.type);

            // Figure out the most appropriate alternatives to missing arguments
            // When no name is specified, use the one currently set if available, otherwise 'Disabled'
            if (new_name == null) {
                new_name = (oldDest == null) ? "" : oldDest.getName();
            }
            // When no display name specified, keep previous if name does not change, otherwise 'none'
            if (new_display == null) {
                if (oldDest != null && oldDest.getName().equals(new_name)) {
                    new_display = oldDest.getDisplayName();
                } else {
                    new_display = "";
                }
            }
            // When no mode is specified, compute the most appropriate one based on portal type / world type
            if (new_mode == null) {
                if (oldDest != null && new_name.equals(oldDest.getName())) {
                    new_mode = oldDest.getMode();
                } else if (Portal.getPortalLocation(new_name, null) != null) {
                    new_mode = PortalMode.DEFAULT;
                } else {
                    World world = WorldManager.getWorld(WorldManager.matchWorld(new_name));
                    if (world == null) {
                        new_mode = PortalMode.DEFAULT; // Dunno what this is, assume portal
                    } else {
                        WorldConfig link = WorldConfig.get(world);
                        if (this.type == PortalType.NETHER &&
                                ((config.worldmode == WorldMode.NORMAL && link.worldmode == WorldMode.NETHER) ||
                                 (config.worldmode == WorldMode.NETHER && link.worldmode == WorldMode.NORMAL)))
                        {
                            // nether -> world and world -> nether uses nether link by default
                            new_mode = PortalMode.NETHER_LINK;
                        }
                        else if (this.type == PortalType.END &&
                               (config.worldmode == WorldMode.NORMAL && link.worldmode == WorldMode.THE_END))
                        {
                            // normal -> end uses the end platform
                            new_mode = PortalMode.END_LINK;
                        }
                        else if (this.type == PortalType.END &&
                                (config.worldmode == WorldMode.THE_END && link.worldmode == WorldMode.NORMAL))
                        {
                            // end -> normal shows credits, then teleports to (bed)spawn
                            new_mode = PortalMode.RESPAWN;
                        }
                        else
                        {
                            // Dunno. Default teleport
                            new_mode = PortalMode.DEFAULT;
                        }
                    }
                }
            }
            // When not specified, use old if possible
            if (new_showCredits == null) {
                if (oldDest != null) {
                    new_showCredits = oldDest.isShowCredits();
                } else {
                    new_showCredits = (this.type == PortalType.END && config.worldmode == WorldMode.THE_END);
                }
            }
            if (new_playersOnly == null) {
                if (oldDest != null) {
                    new_playersOnly = oldDest.isPlayersOnly();
                } else {
                    new_playersOnly = (this.type == PortalType.END && config.worldmode == WorldMode.THE_END);
                }
            }
            if (new_teleportToLastPosition == null) {
                if (oldDest != null) {
                    new_teleportToLastPosition = oldDest.isTeleportToLastPosition();
                } else {
                    new_teleportToLastPosition = Boolean.FALSE;
                }
            }
            if (new_teleportMounts == null) {
                if (oldDest != null) {
                    new_teleportMounts = oldDest.canTeleportMounts();
                } else {
                    new_teleportMounts = Boolean.FALSE;
                }
            }

            // Verify the destination name is valid
            if (!new_name.isEmpty() && Portal.getPortalLocation(new_name, null) == null && WorldManager.matchWorld(new_name) == null) {
                message(ChatColor.RED + "Destination '"  + new_name + "' is not a valid world or portal!");
                return;
            }

            // Setup new destination info and update
            PortalDestination newDest = new PortalDestination();
            newDest.setMode(new_mode);
            newDest.setName(new_name);
            newDest.setDisplayName(new_display);
            newDest.setPlayersOnly(new_playersOnly);
            newDest.setTeleportToLastPosition(new_teleportToLastPosition);
            newDest.setCanTeleportMounts(new_teleportMounts);
            newDest.setShowCredits(new_showCredits);
            config.setDefaultDestination(this.type, newDest);

            message(ChatColor.GREEN + "Default " + this.type.name().toLowerCase(Locale.ENGLISH) +
                    " portal behavior of world " + ChatColor.WHITE + "'"  + worldname + "'" +
                    ChatColor.GREEN + " set to: " + newDest.getInfoString());
        }
    }
}

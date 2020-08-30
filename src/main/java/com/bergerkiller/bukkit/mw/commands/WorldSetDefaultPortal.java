package com.bergerkiller.bukkit.mw.commands;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.PortalStore;
import com.bergerkiller.bukkit.mw.PortalType;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.WorldMode;
import com.bergerkiller.bukkit.mw.portal.PortalDestination;
import com.bergerkiller.bukkit.mw.portal.PortalMode;
import com.bergerkiller.mountiplex.MountiplexUtil;

public class WorldSetDefaultPortal extends Command {
    private final PortalType type;

    public WorldSetDefaultPortal(PortalType type) {
        super(Permission.COMMAND_SETPORTAL, type.getCommandNode());
        this.type = type;
    }

    @Override
    public void execute() {
        // Specifying just /world endportal == /world endportal info
        if (args.length == 0) {
            args = new String[] {"info"};
        }

        String command = this.removeArg(0);
        PortalDestination dest;

        if (command.equalsIgnoreCase("info")) {
            this.genWorldname(0);
            if (!this.handleWorld()) {
                return;
            }

            WorldConfig config = WorldConfig.get(worldname);
            dest = config.getDefaultDestination(this.type);

        } else if (command.equalsIgnoreCase("autodetect")) {
            this.genWorldname(0);
            if (!this.handleWorld()) {
                return;
            }

            WorldConfig config = WorldConfig.get(worldname);
            PortalDestination old_dest = config.getDefaultDestination(this.type);
            config.setDefaultDestination(this.type, null);
            config.tryCreatePortalLink();
            dest = config.getDefaultDestination(this.type);
            if (dest == null) {
                dest = old_dest;
                config.setDefaultDestination(this.type, dest);
                message(ChatColor.RED + "Failed to automatically detect a link with another world");
            } else {
                message(ChatColor.GREEN + "Detected a " + dest.getMode().name() + " to " + dest.getName());
            }

        } else {
            // All these commands have 1 argument
            this.genWorldname(1);
            if (!this.handleWorld()) {
                return;
            }

            WorldConfig config = WorldConfig.get(worldname);
            dest = config.getDefaultDestination(this.type);

            if (command.equalsIgnoreCase("destination")) {
                if (args.length > 0) {
                    String new_destination = args[0];

                    // Verify the destination name is valid
                    if (Portal.getPortalLocation(new_destination, null) == null && WorldManager.matchWorld(new_destination) == null) {
                        message(ChatColor.RED + "Destination '"  + new_destination + "' is not a valid world or portal!");
                        return;
                    }

                    // Update destination. Detect other properties based on destination if previously none was configured.
                    if (dest == null) {
                        dest = new PortalDestination();
                        if (Portal.getPortalLocation(new_destination, null) != null) {
                            dest.setMode(PortalMode.DEFAULT);
                        } else {
                            World world = WorldManager.getWorld(WorldManager.matchWorld(new_destination));
                            if (world == null) {
                                dest.setMode(PortalMode.DEFAULT); // Dunno what this is, assume portal
                            } else {
                                WorldConfig link = WorldConfig.get(world);
                                if (this.type == PortalType.NETHER &&
                                        ((config.worldmode == WorldMode.NORMAL && link.worldmode == WorldMode.NETHER) ||
                                         (config.worldmode == WorldMode.NETHER && link.worldmode == WorldMode.NORMAL)))
                                {
                                    // nether -> world and world -> nether uses nether link by default
                                    dest.setMode(PortalMode.NETHER_LINK);
                                }
                                else if (this.type == PortalType.END &&
                                       (config.worldmode == WorldMode.NORMAL && link.worldmode == WorldMode.THE_END))
                                {
                                    // normal -> end uses the end platform
                                    dest.setMode(PortalMode.END_PLATFORM);
                                }
                                else if (this.type == PortalType.END &&
                                        (config.worldmode == WorldMode.THE_END && link.worldmode == WorldMode.NORMAL))
                                {
                                    // end -> normal shows credits, then teleports to (bed)spawn
                                    dest.setMode(PortalMode.RESPAWN);
                                    dest.setShowCredits(true);
                                    dest.setPlayersOnly(true);
                                }
                                else
                                {
                                    // Dunno. Default teleport
                                    dest.setMode(PortalMode.DEFAULT);
                                }
                            }
                        }
                    } else {
                        dest = dest.clone();
                    }

                    // Update destination
                    dest.setName(new_destination);

                    message(ChatColor.GREEN + "Destination set to: '" + ChatColor.WHITE + "'" +
                            new_destination + "'");
                } else {
                    message(ChatColor.YELLOW + "Destination is currently set to: " + ChatColor.WHITE + "'" +
                            ((dest==null?"None":dest.getName()) + "'"));
                }

            } else if (command.equalsIgnoreCase("mode")) {
                if (args.length > 0) {
                    PortalMode new_mode = ParseUtil.parseEnum(PortalMode.class, args[0], null);
                    if (new_mode == null) {
                        message(ChatColor.RED + "Not a valid portal mode: " + args[0]);
                        return;
                    }

                    dest = (dest != null) ? dest.clone() : new PortalDestination();
                    dest.setMode(new_mode);

                    message(ChatColor.GREEN + "Mode set to: '" + ChatColor.WHITE + "'" +
                            new_mode.name().toLowerCase(Locale.ENGLISH) + "'");
                } else {
                    message(ChatColor.YELLOW + "Mode is currently set to: " + ChatColor.WHITE + "'" +
                            ((dest==null?"default":dest.getMode().name().toLowerCase(Locale.ENGLISH)) + "'"));
                }

            } else if (command.equalsIgnoreCase("displayname")) {
                if (args.length > 0) {
                    dest = (dest != null) ? dest.clone() : new PortalDestination();
                    dest.setDisplayName(args[0]);

                    message(ChatColor.GREEN + "Display name set to: '" + ChatColor.WHITE + "'" +
                            args[0] + "'");
                } else {
                    message(ChatColor.YELLOW + "Display name is currently set to: " + ChatColor.WHITE + "'" +
                            ((dest==null?"None":dest.getDisplayName()) + "'"));
                }

            } else if (command.equalsIgnoreCase("playeronly")) {
                dest = handleBooleanProperty(dest,
                        PortalDestination::isPlayersOnly,
                        PortalDestination::setPlayersOnly,
                        "Player-only");
            } else if (command.equalsIgnoreCase("lastposition")) {
                dest = handleBooleanProperty(dest,
                        PortalDestination::isTeleportToLastPosition,
                        PortalDestination::setTeleportToLastPosition,
                        "Teleporting to last known position");
            } else if (command.equalsIgnoreCase("teleportmounts")) {
                dest = handleBooleanProperty(dest,
                        PortalDestination::canTeleportMounts,
                        PortalDestination::setCanTeleportMounts,
                        "Teleporting mounted entities");
            } else if (command.equalsIgnoreCase("showcredits")) {
                dest = handleBooleanProperty(dest,
                        PortalDestination::canTeleportMounts,
                        PortalDestination::setCanTeleportMounts,
                        "Showing end-game credits");
            } else if (command.equalsIgnoreCase("nonplayerscreateportals")) {
                dest = handleBooleanProperty(dest,
                        PortalDestination::canNonPlayersCreatePortals,
                        PortalDestination::setCanNonPlayersCreatePortals,
                        "Non-players creating portal links");
            } else {
                message(ChatColor.RED + "Unknown command: " + command);
                return;
            }

            // Set whatever has changed, if it has changed
            config.setDefaultDestination(this.type, dest);
        }

        if (dest == null) {
            dest = new PortalDestination(); // disabled
        }
        message(ChatColor.GREEN + "Default " + this.type.name().toLowerCase(Locale.ENGLISH) +
                " portal behavior of world " + ChatColor.WHITE + "'"  + this.worldname + "'" +
                ChatColor.GREEN + " is set to: " + dest.getInfoString());
    }

    private PortalDestination handleBooleanProperty(PortalDestination dest,
                                                    Function<PortalDestination, Boolean> getter,
                                                    BiConsumer<PortalDestination,
                                                    Boolean> setter,
                                                    String name)
    {
        if (args.length > 0) {
            if (ParseUtil.isBool(args[0])) {
                message(ChatColor.RED + "Not a valid yes/no value: " + args[0]);
                return dest;
            }

            dest = (dest != null) ? dest.clone() : new PortalDestination();
            boolean new_value = ParseUtil.parseBool(args[0], false);
            setter.accept(dest, new_value);

            message(ChatColor.GREEN + name + " set to: '" + ChatColor.WHITE + "'" +
                    (new_value?"Yes":"No") + "'");
            return dest;
        } else {
            message(ChatColor.YELLOW + name + " is currently set to: " + ChatColor.WHITE + "'" +
                    ((dest==null?"No":(getter.apply(dest)?"Yes":"No")) + "'"));
            return dest;
        }
    }

    @Override
    public List<String> autocomplete() {
        if (args.length <= 1) {
            return this.processBasicAutocomplete("info", "autodetect", "destination", "mode",
                    "displayname", "playeronly", "lastposition", "teleportmounts",
                    "showcredits", "nonplayerscreateportals");
        }

        // For info/autodetect, first arg is the world name. For other commands, its the arg following.
        if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("autodetect") || args.length > 2) {
            return this.processWorldNameAutocomplete();
        }

        // Different properties
        if (args[0].equalsIgnoreCase("destination")) {
            return processAutocomplete(Stream.concat(
                    Stream.of(PortalStore.getPortals()),
                    Bukkit.getWorlds().stream().map(World::getName)
                ));
        }
        if (args[0].equalsIgnoreCase("mode")) {
            return processAutocomplete(Stream.of(PortalMode.values())
                    .map(PortalMode::name).map(s -> s.toLowerCase(Locale.ENGLISH)));
        }
        if (args[0].equalsIgnoreCase("displayname")) {
            return processAutocomplete(MountiplexUtil.toStream("[Display name]"));
        }
        if (LogicUtil.containsIgnoreCase(args[0], "playeronly", "lastposition",
                "teleportmounts", "showcredits", "nonplayerscreateportals"))
        {
            return processAutocomplete(Stream.of("yes", "no"));
        }

        return null;
    }
}

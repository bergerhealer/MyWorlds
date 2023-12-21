package com.bergerkiller.bukkit.mw.commands;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import com.bergerkiller.bukkit.mw.events.MyWorldsTeleportCommandEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.Portal;
import com.bergerkiller.bukkit.mw.PortalStore;
import com.bergerkiller.bukkit.mw.Util;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.commands.registry.RegisteredCommand;

public class TeleportPortal extends Command {
    public static RegisteredCommand REGISTERED = RegisteredCommand.create(TeleportPortal::new, "tpp");

    public TeleportPortal() {
        super(Permission.COMMAND_TPP, "tpp");
    }

    @Override
    public boolean hasPermission() {
        if (super.hasPermission()) {
            return true;
        }

        // If no args specified, allow, just show invalid
        if (args.length == 0) {
            return true;
        }

        // If more than one argument is specified, that would teleport other players
        // Disallow at all times
        if (args.length != 1) {
            return false;
        }

        // Find the Location of the portal sign
        Location signLocation = Portal.getPortalLocation(args[0], player.getWorld().getName());
        if (signLocation != null) {
            Portal portal = Portal.getNear(signLocation, 3);
            if (portal == null) {
                return false;
            }
            return Permission.PORTAL_TELEPORT.has(player, portal.getName());
        }

        // World name, alternatively
        String worldname = WorldManager.matchWorld(args[0]);
        return Permission.GENERAL_TELEPORT.has(player, (worldname == null) ? args[0] : worldname);
    }

    public void execute() {
        if (args.length >= 1) {
            String dest = this.removeArg(0);
            Player[] targets = null;
            if (args.length >= 1) {
                HashSet<Player> found = new HashSet<Player>();
                for (String arg : args) {
                    Player player = Util.parsePlayerName(this.sender, arg);
                    if (player != null) {
                        found.add(player);
                    }
                }
                targets = found.toArray(new Player[0]);
            } else if (sender instanceof Player) {
                targets = new Player[] {(Player) sender};
            } else {
                sender.sendMessage("This command is only for players!");
                return;
            }
            if (targets.length > 0) {
                //Get prefered world
                World world = targets[0].getWorld();
                if (player != null) world = player.getWorld();
                //Get portal
                Location signLocation = Portal.getPortalLocation(dest, world.getName());
                if (signLocation != null) {
                    final Portal portal = Portal.getNear(signLocation, 3);
                    if (portal != null) {
                        //Perform portal teleports
                        int succcount = 0;
                        for (Player target : targets) {
                            if (portal.teleportSelf(target, (entity, loc) -> new MyWorldsTeleportCommandEvent(
                                    entity,
                                    MyWorldsTeleportCommandEvent.CommandType.TELEPORT_PORTAL,
                                    loc,
                                    portal.getName()
                            ))) {
                                //Success
                                succcount++;
                            }
                        }
                        if (targets.length > 1 || targets[0] != sender) {
                            message(ChatColor.YELLOW.toString() + succcount + "/" + targets.length + 
                                    " Players have been teleported to portal '" + dest + "'!");
                        }
                    } else {
                        message(ChatColor.RED + "The portal world is not loaded!");
                    }
                } else {
                    //Match world
                    String worldname = WorldManager.matchWorld(dest);
                    if (worldname != null) {
                        World w = WorldManager.getWorld(worldname);
                        if (w != null) {
                            //Perform world teleports
                            int succcount = 0;
                            for (Player target : targets) {
                                if (WorldManager.teleportToWorld(target, w, (entity, loc) -> new MyWorldsTeleportCommandEvent(
                                        entity,
                                        MyWorldsTeleportCommandEvent.CommandType.TELEPORT_WORLD,
                                        loc
                                ))) {
                                    //Success
                                    succcount++;
                                }
                            }

                            // Show message, but don't if only the sender was teleported
                            // He already receives an enter message by the teleport listener
                            if (targets.length > 1 || targets[0] != sender) {
                                message(ChatColor.YELLOW.toString() + succcount + "/" + targets.length + 
                                        " Players have been teleported to world '" + w.getName() + "'!");
                            }
                        } else {
                            message(ChatColor.YELLOW + "World '" + worldname + "' is not loaded!");
                        }
                    } else {
                        Localization.PORTAL_NOTFOUND.message(sender, dest);
                        listPortals(Portal.getPortals());
                    }
                }
            }
        } else {
            showInv();
        }    
    }

    @Override
    public List<String> autocomplete() {
        if (args.length <= 1) {
            return processAutocomplete(Stream.concat(
                        Stream.of(PortalStore.getPortals()),
                        /* Important: don't use World::getName(), that breaks on older mc */
                        Bukkit.getWorlds().stream().map(w -> w.getName())
                    ));
        } else {
            return processPlayerNameAutocomplete();
        }
    }
}

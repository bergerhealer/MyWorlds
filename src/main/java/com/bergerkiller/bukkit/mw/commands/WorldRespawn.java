package com.bergerkiller.bukkit.mw.commands;

import java.util.List;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.PortalStore;
import com.bergerkiller.bukkit.mw.RespawnPoint;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldRespawn extends Command {

    public WorldRespawn() {
        super(Permission.COMMAND_SETSPAWN, "world.respawn");
    }

    @Override
    public boolean allowConsole() {
        return false;
    }

    public void execute() {
        if (args.length == 0 || args[0].equals("info")) {
            this.genWorldname(1);
            if (!this.checkValidWorld()) {
                return;
            }

            WorldConfig wc = WorldConfig.get(worldname);
            message(ChatColor.GREEN + "Respawn location for world '" + worldname + "' set to the " +
                    wc.respawnPoint.getDescription());
        } else if (args[0].equals("bed")) {
            this.genWorldname(2);
            if (!this.checkValidWorld()) {
                return;
            }

            WorldConfig wc = WorldConfig.get(worldname);

            if (args.length >= 2) {
                wc.bedRespawnEnabled = ParseUtil.parseBool(args[1]);
                if (!wc.bedRespawnEnabled) {
                    World w = wc.getWorld();
                    if (w != null) {
                        for (Player p : w.getPlayers()) {
                            wc.updateBedSpawnPoint(p);
                        }
                    }
                }
            }

            if (wc.bedRespawnEnabled) {
                message(ChatColor.YELLOW + "Respawning at last slept beds on World: '" + ChatColor.WHITE + worldname +
                        ChatColor.YELLOW + "' is " + ChatColor.GREEN + "ENABLED");
                message(ChatColor.GREEN + "Players will respawn at the bed they last slept in, if set");
            } else {
                message(ChatColor.YELLOW + "Respawning at last slept beds on World: '" + ChatColor.WHITE + worldname +
                        ChatColor.YELLOW + "' is " + ChatColor.RED + "DISABLED");
                message(ChatColor.YELLOW + "Players will respawn at the world's spawn or home point when dying");
            }

        } else if (args.length >= 1 && args[0].equals("here")) {
            this.genWorldname(1);
            if (!this.checkValidWorld()) {
                return;
            }

            // Set respawn point to player's location
            WorldConfig.get(worldname).respawnPoint = new RespawnPoint.RespawnPointLocation(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Respawn location for world '" + worldname + "' set to your position!");

        } else if (args.length >= 1 && args[0].equals("previous")) {
            this.genWorldname(1);
            if (!this.checkValidWorld()) {
                return;
            }

            // Set respawn point to player's location
            WorldConfig.get(worldname).respawnPoint = new RespawnPoint.RespawnPointPreviousLocation();
            sender.sendMessage(ChatColor.GREEN + "Respawn location for world '" + worldname + "' set to the previous player position");
            sender.sendMessage(ChatColor.GREEN + "This respawns the players where they died");

        } else if (args.length >= 2 && args[0].equals("world")) {
            String destinationWorld = args[1];
            this.genWorldname(2);
            if (!this.checkValidWorld()) {
                return;
            }

            // Check valid destination world
            {
                WorldConfig destConfig = WorldConfig.getIfExists(destinationWorld);
                boolean valid;
                if (destConfig != null) {
                    valid = destConfig.isLoaded() || destConfig.isExisting();
                } else {
                    valid = WorldUtil.isLoadableWorld(destinationWorld);
                }
                if (!valid) {
                    sender.sendMessage(ChatColor.RED + "Can not find world '" + destinationWorld + "' to respawn at!");
                    return;
                }
            }

            // Set respawn point to world spawn point
            WorldConfig.get(worldname).respawnPoint = new RespawnPoint.RespawnPointWorldSpawn(destinationWorld);
            sender.sendMessage(ChatColor.GREEN + "Respawn location for world '" + worldname +
                    "' set to the world spawn of world '" + destinationWorld + "'!");

        } else if (args.length >= 2 && args[0].equals("portal")) {
            String destinationPortal = args[1];
            this.genWorldname(2);
            if (!this.checkValidWorld()) {
                return;
            }

            if (!LogicUtil.contains(destinationPortal, PortalStore.getPortals())) {
                Localization.PORTAL_NOTFOUND.message(sender, destinationPortal);
                return;
            }

            // Set respawn point to portal location
            WorldConfig.get(worldname).respawnPoint = new RespawnPoint.RespawnPointPortal(destinationPortal);
            sender.sendMessage(ChatColor.GREEN + "Respawn location for world '" + worldname +
                    "' set to portal '" + destinationPortal + "'!");

        } else {
            sender.sendMessage(ChatColor.RED + "Usage:");
            sender.sendMessage(ChatColor.RED + "/world respawn here");
            sender.sendMessage(ChatColor.RED + "/world respawn world [world_name]");
            sender.sendMessage(ChatColor.RED + "/world respawn portal [portal_name]");
        }
    }

    private boolean checkValidWorld() {
        if (!this.handleWorld()) {
            return false;
        }

        WorldConfig config = WorldConfig.getIfExists(worldname);
        boolean valid;
        if (config != null) {
            valid = config.isLoaded() || config.isExisting();
        } else {
            valid = WorldUtil.isLoadableWorld(worldname);
        }
        if (!valid) {
            Localization.WORLD_NOTFOUND.message(sender);
        }
        return valid;
    }

    @Override
    public List<String> autocomplete() {
        if (args.length <= 1) {
            return processAutocomplete(Stream.of(
                    "bed", "here", "world", "portal", "previous", "info"));
        }

        if (args[0].equals("bed")) {
            if (args.length >= 3) {
                return processWorldNameAutocomplete();
            } else {
                return processAutocomplete(Stream.of(
                        "enable", "disable"));
            }
        } else if (args[0].equals("here")) {
            return processWorldNameAutocomplete();
        } else if (args[0].equals("world")) {
            return processWorldNameAutocomplete();
        } else if (args[0].equals("portal")) {
            if (args.length >= 3) {
                return processWorldNameAutocomplete();
            } else {
                return processAutocomplete(Stream.of(PortalStore.getPortals()));
            }
        } else if (args[0].equals("previous")) {
            return processWorldNameAutocomplete();
        } else if (args[0].equals("info")) {
            return processWorldNameAutocomplete();
        } else {
            return null;
        }
    }
}

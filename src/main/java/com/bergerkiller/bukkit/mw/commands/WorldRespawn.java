package com.bergerkiller.bukkit.mw.commands;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.bergerkiller.bukkit.mw.BedRespawnMode;
import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.utils.LogicUtil;
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
                wc.setBedRespawnMode(BedRespawnMode.parse(args[1]));
            }

            wc.getBedRespawnMode().showAsMessage(sender, worldname);

        } else if (args[0].equals("here")) {
            this.genWorldname(1);
            if (!this.checkValidWorld()) {
                return;
            }

            // Set respawn point to player's location
            WorldConfig.get(worldname).respawnPoint = new RespawnPoint.RespawnPointLocation(player.getLocation());
            sender.sendMessage(ChatColor.GREEN + "Respawn location for world '" + worldname + "' set to your position!");

        } else if (args[0].equals("previous")) {
            this.genWorldname(1);
            if (!this.checkValidWorld()) {
                return;
            }

            // Set respawn point to player's location
            WorldConfig.get(worldname).respawnPoint = new RespawnPoint.RespawnPointPreviousLocation();
            sender.sendMessage(ChatColor.GREEN + "Respawn location for world '" + worldname + "' set to the previous player position");
            sender.sendMessage(ChatColor.GREEN + "This respawns the players where they died");

        } else if (args[0].equals("ignore")) {
            this.genWorldname(1);
            if (!this.checkValidWorld()) {
                return;
            }

            // Set respawn point to be ignored, MyWorlds will not alter it.
            WorldConfig.get(worldname).respawnPoint = RespawnPoint.IGNORED;
            sender.sendMessage(ChatColor.YELLOW + "Respawn location for world '" + worldname + "' set to be ignored by MyWorlds");
            sender.sendMessage(ChatColor.YELLOW + "The server and/or other plugins will handle the respawn location, instead");

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
                    "bed", "here", "world", "portal", "previous", "ignore", "info"));
        }

        if (args[0].equals("bed")) {
            if (args.length >= 3) {
                return processWorldNameAutocomplete();
            } else {
                return processAutocomplete(Stream.of(BedRespawnMode.values())
                                .map(BedRespawnMode::name)
                                .map(s -> s.toLowerCase(Locale.ENGLISH)));
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
        } else if (args[0].equals("ignore")) {
            return processWorldNameAutocomplete();
        } else if (args[0].equals("info")) {
            return processWorldNameAutocomplete();
        } else {
            return null;
        }
    }
}

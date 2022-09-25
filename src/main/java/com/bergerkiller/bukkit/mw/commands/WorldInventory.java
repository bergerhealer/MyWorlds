package com.bergerkiller.bukkit.mw.commands;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldInventory extends Command {

    public WorldInventory() {
        super(Permission.COMMAND_INVENTORY, "world.inventory");
    }
    
    public Set<String> worlds = new LinkedHashSet<String>();

    public boolean prepareWorlds() {
        this.removeArg(0);
        for (String world : this.args) {
            world = WorldManager.matchWorld(world);
            if (world != null) {
                this.worlds.add(world);
            }
        }
        if (this.worlds.isEmpty()) {
            message(ChatColor.RED + "Failed to find any of the worlds specified!");
            return false;
        } else {
            return true;
        }
    }

    public void sendWorldsMessage(Collection<String> worlds, String text) {
        MessageBuilder builder = new MessageBuilder();
        builder.green("Worlds ").setSeparator(ChatColor.WHITE, "/");
        for (String world : worlds) {
            builder.yellow(world);
        }
        builder.setSeparator(null).green(" " + text);
        builder.send(this.sender);
    }

    public void execute() {
        // Handle migration commands
        if (args.length >= 1 && args[0].equalsIgnoreCase("migrate")) {
            this.removeArg(0);
            if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
                plugin.getPlayerDataMigrator().showStatus(sender);
            } else {
                String migrationName = this.removeArg(0);
                this.executeMigration(migrationName);
            }
            return;
        }

        if (args.length > 1) {
            // While migrating data, can't change any of the configs...
            if (plugin.getPlayerDataMigrator().isRunning()) {
                sender.sendMessage(ChatColor.RED + "Can't change inventory configuration: busy migrating");
                plugin.getPlayerDataMigrator().showStatus(sender);
                return;
            }

            if (args[0].equalsIgnoreCase("merge")) {
                if (this.prepareWorlds()) {
                    Set<String> invWorlds = new LinkedHashSet<String>();
                    for (String world : worlds) {
                        invWorlds.add(world.toLowerCase());
                        invWorlds.addAll(com.bergerkiller.bukkit.mw.WorldConfig.get(world).inventory.getWorlds());
                    }
                    if (invWorlds.size() <= 1) {
                        message(ChatColor.RED + "You need to specify more than one world to merge!");
                    } else {
                        com.bergerkiller.bukkit.mw.WorldInventory.merge(invWorlds);
                        sendWorldsMessage(invWorlds, "now share the same player inventory!");
                    }
                }
                return;
            } else if (args[0].equalsIgnoreCase("split") || args[0].equalsIgnoreCase("detach")) {
                if (this.prepareWorlds()) {
                    com.bergerkiller.bukkit.mw.WorldInventory.detach(this.worlds);
                    if (this.worlds.size() > 1) {
                        sendWorldsMessage(worlds, "now have their own player inventories!");
                    } else {
                        for (String world : this.worlds) {
                            message(ChatColor.GREEN + "World " + ChatColor.WHITE + world + ChatColor.GREEN + " now has its own player inventory!");
                        }
                    }
                }
                return;
            } else if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
                boolean enable = args[0].equalsIgnoreCase("enable");
                if (this.prepareWorlds()) {
                    // Expand the worlds set based on the inventories they share
                    if (MyWorlds.useWorldInventories) {
                        Set<String> tmp = new HashSet<String>(worlds);
                        for (String world : tmp) {
                            WorldConfig config = WorldConfig.getIfExists(world);
                            if (config != null && config.inventory != null) {
                                worlds.addAll(config.inventory.getWorlds());
                            }
                        }
                    }

                    for (String world : worlds) {
                        WorldConfig config = WorldConfig.getIfExists(world);
                        if (config != null) {
                            config.clearInventory = !enable;
                        } else {
                            sender.sendMessage(ChatColor.RED + "Failed to update for " + world + ": not a world");
                        }
                    }
                    if (this.worlds.size() > 1) {
                        if (enable) {
                            sendWorldsMessage(worlds, "now allow inventories to be loaded when players join!");
                        } else {
                            sendWorldsMessage(worlds, "now deny inventories from being loaded when players join!");
                        }
                    } else {
                        for (String world : this.worlds) {
                            if (enable) {
                                message(ChatColor.GREEN + "World " + ChatColor.WHITE + world + ChatColor.GREEN + " now allows the inventory to be loaded when players join!");
                            } else {
                                message(ChatColor.GREEN + "World " + ChatColor.WHITE + world + ChatColor.GREEN + " now denies the inventory from being loaded when players join!");
                            }
                        }
                    }
                }
                return;
            } else {
                message(ChatColor.RED + "Unknown command: /world inventory " + args[0]);
            }
        }
        //usage
        message(ChatColor.YELLOW + "/world inventory [split/merge/enable/disable/migrate <sub>] [worldnames]");
    }

    public void executeMigration(String migrationName) {
        if (migrationName.equalsIgnoreCase("main")) {
            WorldConfig wc = prepareMigrateWorld();
            if (wc == null) {
                return;
            }
            if (!wc.isLoaded()) {
                sender.sendMessage(ChatColor.RED + "World " + wc.worldname + " is not loaded!");
                return;
            }

            message(ChatColor.GREEN + "Migrating to a new inventory main world...");
            plugin.getPlayerDataMigrator().changeMainWorld(wc);
            plugin.getPlayerDataMigrator().showStatus(sender);
        } else if (migrationName.equalsIgnoreCase("storage")) {
            WorldConfig wc = prepareMigrateWorld();
            if (wc == null) {
                return;
            }

            message("B");
        } else {
            message(ChatColor.RED + "Unknown command: /world inventory migrate " + migrationName);
        }
    }

    private WorldConfig prepareMigrateWorld() {
        if (args.length == 0) {
            message(ChatColor.RED + "Please specify the world name to migrate!");
            return null;
        }

        String worldName = this.removeArg(0);
        String matched = WorldManager.matchWorld(worldName);
        if (matched == null) {
            message(ChatColor.RED + "Failed to find world '" + worldName + "'!");
            return null;
        }

        return WorldConfig.get(matched);
    }

    @Override
    public List<String> autocomplete() {
        // Migration commands
        if (args.length > 1 && args[0].equalsIgnoreCase("migrate")) {
            if (args.length > 2) {
                return processWorldNameAutocomplete();
            } else {
                return processAutocomplete(Stream.of("status", "storage", "main"));
            }
        }

        // Default stuff
        return processBasicAutocompleteOrWorldName("merge", "split", "enable", "disable", "migrate");
    }
}

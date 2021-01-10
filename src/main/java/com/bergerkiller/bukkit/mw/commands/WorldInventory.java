package com.bergerkiller.bukkit.mw.commands;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        if (args.length > 1) {
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
        message(ChatColor.YELLOW + "/world inventory [split/merge/enable/disable] [worldnames]");
    }

    @Override
    public List<String> autocomplete() {
        return processBasicAutocompleteOrWorldName("merge", "split", "enable", "disable");
    }
}

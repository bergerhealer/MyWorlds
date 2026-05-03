package com.bergerkiller.bukkit.mw.commands;

import java.util.ArrayList;
import java.util.List;

import com.bergerkiller.bukkit.common.world.LoadableWorld;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.mw.Permission;

public class WorldList extends Command {

    public WorldList() {
        super(Permission.COMMAND_LIST, "world.list");
    }

    public void execute() {
        List<LoadableWorld> loadableWorlds = new ArrayList<>(LoadableWorld.listAll());
        loadableWorlds.sort((a, b) -> {
            boolean aLoaded = a.isLoaded();
            boolean bLoaded = b.isLoaded();
            if (aLoaded && !bLoaded) {
                return -1;
            } else if (!aLoaded && bLoaded) {
                return 1;
            } else {
                return a.getDisplayName().compareToIgnoreCase(b.getDisplayName());
            }
        });

        if (sender instanceof Player) {
            //perform some nice layout coloring
            MessageBuilder builder = new MessageBuilder();
            builder.newLine().green("[Loaded/Online] ").red("[Unloaded/Offline] ").gold("[Legacy]");
            builder.newLine().yellow("Available worlds: ");
            builder.setSeparator(ChatColor.WHITE, " / ").setIndent(2).newLine();
            for (LoadableWorld world : loadableWorlds) {
                if (world.getFormat() == LoadableWorld.Format.SPIGOT_CONVERTED) {
                    builder.gold(world.getDisplayName());
                } else if (world.isLoaded()) {
                    builder.green(world.getDisplayName());
                } else {
                    builder.red(world.getDisplayName());
                }
            }
            builder.send(sender);
        } else {
            //plain world per line
            sender.sendMessage("Available worlds:");
            for (LoadableWorld world : loadableWorlds) {
                String status;
                if (world.getFormat() == LoadableWorld.Format.SPIGOT_CONVERTED) {
                    status = "[Legacy]";
                } else if (world.isLoaded()) {
                    status = "[Loaded]";
                } else {
                    status = "[Unloaded]";
                }
                sender.sendMessage("    " + world.getDisplayName() + " " + status);
            }
        }
    }
}

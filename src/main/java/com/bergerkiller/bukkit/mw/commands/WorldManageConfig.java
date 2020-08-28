package com.bergerkiller.bukkit.mw.commands;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfigStore;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldManageConfig extends Command {

    public WorldManageConfig() {
        super(Permission.COMMAND_CONFIG, "world.config");
    }

    public void execute() {
        if (args.length >= 1) {
            String cmd = this.removeArg(0);
            if (cmd.equalsIgnoreCase("load")) {
                com.bergerkiller.bukkit.mw.WorldConfig.init();
                message(ChatColor.GREEN + "World configuration has been loaded!");
            } else if (cmd.equalsIgnoreCase("save")) {
                com.bergerkiller.bukkit.mw.WorldConfig.saveAll();
                message(ChatColor.GREEN + "World configuration has been saved!");
            } else if (cmd.equalsIgnoreCase("reset")) {
                List<String> toReset = new ArrayList<String>();
                if (args.length >= 1) {
                    // Reset only the configurations for the worlds specified
                    LogicUtil.addArray(toReset, args);
                } else {
                    // Reset all world configurations
                    toReset.addAll(WorldUtil.getLoadableWorlds());
                }
                Set<String> affected = new LinkedHashSet<String>();
                for (String worldName : toReset) {
                    if (WorldConfigStore.exists(worldName)) {
                        WorldConfigStore.get(worldName).reset();
                        affected.add(worldName);
                    } else if (WorldManager.worldExists(worldName)) {
                        WorldConfigStore.get(worldName);
                        affected.add(worldName);
                    }
                }
                if (affected.isEmpty()) {
                    message(ChatColor.RED + "None of the worlds specified were identified, nothing is reset!");
                } else {
                    message(ChatColor.GREEN + "The following world configurations have been reset to the defaults:");
                    MessageBuilder message = new MessageBuilder().setSeparator(ChatColor.WHITE, " / ");
                    for (String worldName : affected) {
                        message.append(ChatColor.YELLOW, worldName);
                    }
                    message.send(sender);
                }
            } else {
                this.showInv();
            }
        } else {
            this.showInv();
        }
    }

    @Override
    public List<String> autocomplete() {
        if (args.length > 1 && args[0].equalsIgnoreCase("reset")) {
            return processWorldNameAutocomplete();
        }
        return processBasicAutocomplete("load", "save", "reset");
    }
}

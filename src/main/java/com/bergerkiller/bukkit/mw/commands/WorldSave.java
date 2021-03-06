package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldSave extends Command {

    public WorldSave() {
        super(Permission.COMMAND_SAVE, "world.save");
    }

    public void execute() {
        if (args.length != 0 && (args[0].equals("*") || args[0].equalsIgnoreCase("all"))) {
            //save all worlds
            message(ChatColor.YELLOW + "Forcing a global world save...");    
            for (World ww : Bukkit.getServer().getWorlds()) {
                ww.save();
            }
            message(ChatColor.GREEN + "All worlds have been saved!");        
        } else {
            this.genWorldname(0);
            if (this.handleWorld()) {
                World w = WorldManager.getWorld(worldname);
                if (w != null) {
                    message(ChatColor.YELLOW + "Saving world '" + worldname + "'...");
                    w.save();
                    message(ChatColor.GREEN + "World saved!");
                } else {
                    Localization.WORLD_NOTLOADED.message(sender, worldname);
                }
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return processWorldNameAutocomplete();
    }
}

package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.AsyncHandler;
import com.bergerkiller.bukkit.mw.LoadChunksTask;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldDelete extends Command {

    public WorldDelete() {
        super(Permission.COMMAND_DELETE, "world.delete");
    }

    public void execute() {
        if (args.length == 1) {
            worldname = args[0];
            if (!WorldManager.worldExists(worldname)) {
                worldname = null;
            }
            if (this.handleWorld()) {
                World loadedWorld = Bukkit.getWorld(worldname);
                if (loadedWorld != null) {
                    // Try to unload the world first
                    if (!Permission.COMMAND_UNLOAD.has(sender)) {
                        message(ChatColor.RED + "World is loaded and you have no permission to unload it!");
                        return;
                    }
                    message(ChatColor.GREEN + "Unloading world before deletion...");

                    // Unload the world, do not save as we are deleting it afterwards
                    LoadChunksTask.abortWorld(loadedWorld, false);
                    if (Bukkit.unloadWorld(loadedWorld, false)) {
                        WorldUtil.closeWorldStreams(loadedWorld);
                        message(ChatColor.GREEN + "World unloaded, now moving on to world deletion");
                    } else {
                        message(ChatColor.RED + "Could not unload world (players on it or main world?)");
                        return;
                    }
                }

                // Delete the world
                logAction("Issued a world deletion command for world: " + worldname);
                WorldConfig.remove(worldname);
                AsyncHandler.delete(sender, worldname);
            }
        } else {
            showInv();
        }
    }
    
}

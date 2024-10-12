package com.bergerkiller.bukkit.mw.commands;

import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.AsyncHandler;
import com.bergerkiller.bukkit.mw.LoadChunksTask;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldRegenerateOptions;
import com.bergerkiller.bukkit.mw.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import java.util.List;

public class WorldRegenerate extends Command {

    public WorldRegenerate() {
        super(Permission.COMMAND_REGENERATE, "world.regenerate");
    }

    public void execute() {
        if (args.length >= 1) {
            worldname = args[0];
            if (!WorldManager.worldExists(worldname)) {
                worldname = null;
            }
            if (this.handleWorld()) {
                this.removeArg(0);

                WorldRegenerateOptions options = WorldRegenerateOptions.parseExtraArgs(sender, args);
                if (options == null) {
                    return;
                }

                final boolean wasLoaded;
                {
                    World loadedWorld = Bukkit.getWorld(worldname);
                    if (loadedWorld == null) {
                        wasLoaded = false;
                    } else {
                        wasLoaded = true;

                        // Try to unload the world first
                        if (!Permission.COMMAND_UNLOAD.has(sender)) {
                            message(ChatColor.RED + "World is loaded and you have no permission to unload it!");
                            return;
                        }
                        message(ChatColor.GREEN + "Unloading world before regenerating...");

                        // Unload the world, do not save as we are deleting it afterwards
                        LoadChunksTask.abortWorld(loadedWorld, false);
                        if (Bukkit.unloadWorld(loadedWorld, false)) {
                            WorldUtil.closeWorldStreams(loadedWorld);
                            message(ChatColor.GREEN + "World unloaded, now moving on to resetting world chunk data");
                        } else {
                            message(ChatColor.RED + "Could not unload world (players on it or main world?)");
                            return;
                        }
                    }
                }

                // Reset the world, regenerate it if it was loaded
                logAction("Issued a world regenerate command for world: " + worldname);
                AsyncHandler.reset(sender, worldname, options).thenAccept(unused -> {
                    if (wasLoaded) {
                        message(ChatColor.GREEN + "Re-generating the world after the reset...");
                        World world = WorldManager.createWorld(worldname, 0, sender);
                        if (world != null) {
                            loadSpawnArea(world);
                        }
                    }
                });
            }
        } else {
            showInv();
        }
    }

    @Override
    public List<String> autocomplete() {
        if (args.length <= 1) {
            return processWorldNameAutocomplete();
        } else {
            return WorldRegenerateOptions.getExtraArgNames();
        }
    }
}

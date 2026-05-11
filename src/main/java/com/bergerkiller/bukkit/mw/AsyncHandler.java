package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.bergerkiller.bukkit.common.world.LoadableWorld;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.AsyncTask;
import com.bergerkiller.bukkit.common.utils.CommonUtil;

public class AsyncHandler {
    public static CompletableFuture<Boolean> reset(final CommandSender sender, String worldname, WorldRegenerateOptions options) {
        final WorldConfig worldConfig = WorldConfig.get(worldname);
        if (worldConfig.isLoaded()) {
            CommonUtil.sendMessage(sender, ChatColor.RED + "Can not reset chunk data of a loaded world!");
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        new AsyncTask("World reset thread") {
            public void run() {
                // World was likely unloaded previously, wait a short time for server for the server to save all the data
                // In particular, the world level.dat must be written out before we can modify it
                // Until this is done, the world might appear as not loadable
                for (int n = 50; n >= 0; n--) {
                    sleep(100);
                    if (isWorldFullySaved(worldConfig)) {
                        break;
                    }
                    if (n == 0) {
                        MyWorlds.plugin.getLogger().log(Level.WARNING, "World '" + worldConfig.worldname + "' was not fully saved after waiting for 5 seconds! Attempting to reset the world anyway...");
                    }
                }

                future.complete(worldConfig.regenerateWorldData(options));
            }
        }.start();

        return future.thenApplyAsync(success -> {
            if (success) {
                CommonUtil.sendMessage(sender, ChatColor.GREEN + "World '" + worldConfig.worldname + "' chunk data has been reset!");
            } else {
                CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to (completely) reset the world chunk data!");
            }
            return success;
        }, CommonUtil.getMainThreadExecutor());
    }

    private static boolean isWorldFullySaved(WorldConfig worldConfig) {
        LoadableWorld loadableWorld = worldConfig.getLoadableWorld();
        if (loadableWorld == null) {
            return false;
        }

        File levelFile = loadableWorld.getLevelFile();
        if (!levelFile.exists()) {
            return false;
        }

        return true;
    }

    public static void delete(final CommandSender sender, String worldname) {
        final WorldConfig worldConfig = WorldConfig.get(worldname);
        if (worldConfig.isLoaded()) {
            CommonUtil.sendMessage(sender, ChatColor.RED + "Can not delete a loaded world!");
            return;
        }
        WorldConfig.remove(worldConfig.worldname);
        new AsyncTask("World deletion thread") {
            public void run() {
                if (worldConfig.deleteWorld()) {
                    CommonUtil.sendMessage(sender, ChatColor.GREEN + "World '" + worldConfig.worldname + "' has been removed!");
                } else {
                    CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to (completely) remove the world!");
                }
            }
        }.start();
    }
    public static void copy(final CommandSender sender, final String oldworld, final String newworld) {
        final WorldConfig oldconfig = WorldConfig.get(oldworld);
        final WorldConfig newconfig = WorldConfig.get(newworld);
        new AsyncTask("World copy thread") {
            public void run() {
                if (oldconfig.copyTo(newconfig)) {
                    CommonUtil.sendMessage(sender, ChatColor.GREEN + "World '" + oldworld + "' has been copied as '" + newworld + "'!");
                } else {
                    CommonUtil.sendMessage(sender, ChatColor.RED + "Failed to copy world to '" + newworld + "'!");
                }
            }
        }.start();
    }
}

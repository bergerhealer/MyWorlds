package com.bergerkiller.bukkit.mw.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.chunk.ForcedChunk;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.mw.MyWorlds;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;
import com.bergerkiller.bukkit.mw.WorldMode;
import com.bergerkiller.bukkit.mw.utils.GeneratorStructuresParser;

public class WorldCreate extends Command {

    public WorldCreate() {
        super(Permission.COMMAND_CREATE, "world.create");
    }

    public void execute() {
        if (args.length != 0) {
            this.worldname = this.removeArg(0);
            String gen = this.getGeneratorName();
            this.genForcedWorldMode();
            if (!WorldManager.worldExists(worldname)) {
                long seedval = WorldManager.getRandomSeed(StringUtil.join(" ", this.args));
                logAction("Issued a world creation command for world: " + worldname);
                WorldConfig.remove(worldname);
                WorldConfig wc = WorldConfig.get(worldname, this.forcedWorldMode);

                if (gen == null) {
                    message(ChatColor.YELLOW + "Creating world '" + worldname + "' (this can take a while) ...");
                    message(ChatColor.WHITE + "World generator: " + ChatColor.YELLOW + "Default (Vanilla)");
                } else {
                    String cgenName = WorldManager.fixGeneratorName(gen);
                    if (cgenName == null || cgenName.length() <= 1) {
                        message(ChatColor.RED + "Failed to create world because the generator '" + gen + "' is missing!");
                        return;
                    }
                    message(ChatColor.YELLOW + "Creating world '" + worldname + "' (this can take a while) ...");
                    wc.setChunkGeneratorName(cgenName);
                    if (cgenName.indexOf(':') == 0) {
                        String args = cgenName.substring(1);

                        // Check whether nostructures option is specified, and write it up-front if so
                        GeneratorStructuresParser structuresOption = new GeneratorStructuresParser();
                        args = structuresOption.process(args);

                        // Write a level.dat with the options changed
                        // This is needed because the options passed to WorldCreator don't support special syntaxes.
                        wc.resetData(seedval);

                        // Log options, limit it to 200 chars
                        if (args.length() > 200) {
                            message(ChatColor.WHITE + "World options: " + ChatColor.YELLOW + args.substring(0, 200) + "(...)");
                        } else {
                            message(ChatColor.WHITE + "World options: " + ChatColor.YELLOW + args);
                        }
                        if (structuresOption.hasNoStructures) {
                            message(ChatColor.WHITE + "World structures: " + ChatColor.RED + "DISABLED");
                        } else if (structuresOption.hasStructures) {
                            message(ChatColor.WHITE + "World structures: " + ChatColor.GREEN + "ENABLED");
                        }

                        message(ChatColor.WHITE + "World generator: " + ChatColor.YELLOW + "Default (Vanilla)");
                    } else {
                        message(ChatColor.WHITE + "World generator: " + ChatColor.YELLOW + cgenName);
                    }
                }
                message(ChatColor.WHITE + "World seed: " + ChatColor.YELLOW + seedval);
                message(ChatColor.WHITE + "World environment: " + ChatColor.YELLOW + wc.worldmode.getName());
                plugin.initDisableSpawn(worldname);
                final World world = WorldManager.createWorld(worldname, seedval, sender);
                if (world != null) {
                    //load chunks
                    final int keepdimension = 11;
                    final int keepedgedim = 2 * keepdimension + 1;
                    final int total = keepedgedim * keepedgedim;

                    // Callback called every time a new chunk is loaded (or fails to load-)
                    // Show a progress message every so often
                    final AtomicInteger lastPercentage = new AtomicInteger(-10);
                    final IntConsumer onChunkLoaded = loadedCount -> {
                        int percent = (loadedCount==total) ? 100 : (100 * loadedCount / total);
                        if ((percent - lastPercentage.get()) == 10) {
                            lastPercentage.set(percent);
                            message(ChatColor.YELLOW + "Preparing spawn area (" + percent + "%)...");
                            plugin.log(Level.INFO, "Preparing spawn area (" + percent + "%)...");
                        }
                    };
                    onChunkLoaded.accept(0); // Initial

                    // Start loading all the spawn chunks asynchronously
                    // Every time a new chunk is loaded, increment a counter
                    int spawnx = world.getSpawnLocation().getBlockX() >> 4;
                    int spawnz = world.getSpawnLocation().getBlockZ() >> 4;
                    final List<ForcedChunk> forcedChunks = new ArrayList<ForcedChunk>();
                    final AtomicInteger chunkCtr = new AtomicInteger();
                    for (int x = -keepdimension; x <= keepdimension; x++) {
                        for (int z = -keepdimension; z <= keepdimension; z++) {
                            int cx = spawnx + x;
                            int cz = spawnz + z;
                            ForcedChunk chunk = WorldUtil.forceChunkLoaded(world, cx, cz);
                            chunk.getChunkAsync().whenComplete((a, t) -> {
                                onChunkLoaded.accept(chunkCtr.incrementAndGet());
                            });
                            forcedChunks.add(chunk);
                        }
                    }

                    // Run this once loading of spawn completes
                    CompletableFuture.allOf(forcedChunks.stream()
                        .map(ForcedChunk::getChunkAsync)
                        .toArray(CompletableFuture[]::new))
                        .whenComplete((v, error) -> {
                            // If something went wrong, log it and show a message
                            // Still do the post-load logic, because that stuff must run anyway
                            if (error != null) {
                                MyWorlds.plugin.getLogger().log(Level.SEVERE, "Failed to load some spawn chunks of " + world.getName(), error);
                                message(ChatColor.RED + "Failed to fully load spawn area of '" + world.getName() + "': " + error.getMessage());
                            }

                            WorldConfig loaded_wc = WorldConfig.get(world);

                            // Set to True, any mistakes in loading chunks will be corrected here
                            world.setKeepSpawnInMemory(true);

                            // Fix up the spawn location
                            loaded_wc.fixSpawnLocation();

                            // Call onLoad (it was ignored while initing to avoid chunk loads when finding spawn)
                            loaded_wc.onWorldLoad(world);

                            // It's now safe to release the chunks
                            forcedChunks.forEach(ForcedChunk::close);
                            forcedChunks.clear();

                            // Confirmation message
                            message(ChatColor.GREEN + "World '" + world.getName() + "' has been loaded and is ready for use!");
                            plugin.log(Level.INFO, "World '"+ world.getName() + "' loaded.");
                        });
                }
            } else {
                message(ChatColor.RED + "World already exists!");
            }
        } else {
            showInv();
            MessageBuilder modes = new MessageBuilder();
            modes.yellow("Available environments: ").setIndent(2).setSeparator(ChatColor.YELLOW, " / ");
            for (WorldMode mode : WorldMode.values()) {
                modes.green(mode.getName());
            }
            modes.send(sender);
        }
    }
    
}

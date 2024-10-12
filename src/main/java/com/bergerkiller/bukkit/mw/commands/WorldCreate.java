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
                    loadSpawnArea(world);
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

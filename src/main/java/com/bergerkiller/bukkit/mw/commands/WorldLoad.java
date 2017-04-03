package com.bergerkiller.bukkit.mw.commands;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfigStore;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldLoad extends Command {

    public WorldLoad() {
        super(Permission.COMMAND_LOAD, "world.load");
    }

    public void execute() {
        if (args.length != 0) {
            this.worldname = args[0];
            this.genForcedWorldMode();
            final String gen = this.getGeneratorName();
            this.worldname = WorldManager.matchWorld(this.worldname);
            if (this.handleWorld()) {
                if (WorldManager.isLoaded(worldname)) {
                    message(ChatColor.YELLOW + "World '" + worldname + "' is already loaded!");
                } else if (gen != null && !Permission.COMMAND_LOADSPECIAL.has(sender)) {
                    // Permission handling to change chunk generator
                    message(ChatColor.RED + "You are not allowed to change world chunk generators!");
                } else if (this.forcedWorldMode != null && !Permission.COMMAND_LOADSPECIAL.has(sender)) {
                    // Permission handling to change world environments
                    message(ChatColor.RED + "You are not allowed to change world environments!");
                } else {
                    com.bergerkiller.bukkit.mw.WorldConfig config = WorldConfigStore.get(this.worldname, this.forcedWorldMode);
                    String msg = "Loading world: '" + this.worldname + "'...";
                    if (gen != null) {
                        if (gen.equalsIgnoreCase("none")) {
                            if (config.getChunkGeneratorName() != null) {
                                msg = "Cleared old chunk generator set and loading world: '" + this.worldname + "'...";
                            }
                        } else {
                            String cgenName = WorldManager.fixGeneratorName(gen);
                            if (cgenName == null) {
                                message(ChatColor.RED + "Can not load world: Chunk generator '" + gen + "' does not exist on this server!");
                                return;
                            } else {
                                config.setChunkGeneratorName(cgenName);
                                message(ChatColor.YELLOW + "Loading world: '" + this.worldname + "' using chunk generator '" + cgenName + "'...");
                            }
                        }
                    }
                    if (this.forcedWorldMode != null) {
                        message(ChatColor.YELLOW + "World will be loaded using environment " + this.forcedWorldMode.getName());
                    }
                    message(ChatColor.YELLOW + msg);
                    logAction("Issued a load command for world: " + this.worldname);
                    if (WorldManager.createWorld(worldname, 0, sender) != null) {
                        message(ChatColor.GREEN + "World loaded!");
                    }
                }
            }
        } else {
            showInv();
        }
    }
}

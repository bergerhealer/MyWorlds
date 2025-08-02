package com.bergerkiller.bukkit.mw.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;

public class WorldInfo extends Command {
    
    public WorldInfo() {
        super(Permission.COMMAND_INFO, "world.info");
    }
    
    public void execute() {
        this.genWorldname(0);
        if (worldname != null) {
            WorldConfig wc = WorldConfig.get(worldname);
            com.bergerkiller.bukkit.mw.WorldInfo info = wc.getInfo();
            if (info == null) {
                message(ChatColor.RED + "' " + worldname + "' is broken, no information can be shown!");
            } else {
                message(ChatColor.YELLOW + "Information about the world: " + worldname);
                message(ChatColor.WHITE + "Environment: " + ChatColor.YELLOW + wc.worldmode.getName());
                String chunkGenerator = wc.getChunkGeneratorName();
                if (chunkGenerator == null) {
                    message(ChatColor.WHITE + "Chunk generator: " + ChatColor.YELLOW + "Default");
                } else {
                    message(ChatColor.WHITE + "Chunk generator: " + ChatColor.YELLOW + chunkGenerator);
                }
                message(ChatColor.WHITE + "Auto-saving: " + ChatColor.YELLOW + wc.autosave);
                message(ChatColor.WHITE + "Keep spawn loaded: " + ChatColor.YELLOW + wc.keepSpawnInMemory);
                message(ChatColor.WHITE + "World seed: " + ChatColor.YELLOW + info.seed);
                if (info.size > 1000000) {
                    message(ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size / 1000000) + " Megabytes");
                } else if (info.size > 1000) {
                    message(ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size / 1000) + " Kilobytes");
                } else if (info.size > 1) {
                    message(ChatColor.WHITE + "World size: " + ChatColor.YELLOW + (info.size) + " Bytes");
                } else {
                    message(ChatColor.WHITE + "World size: " + ChatColor.YELLOW + "Unknown (calculation is disabled)");
                }
                //Default portals
                if (wc.getDefaultNetherPortalDestination() == null) {
                    message(ChatColor.WHITE + "Nether portal: " + ChatColor.YELLOW + "None (will auto-detect when available)");
                } else {
                    message(ChatColor.WHITE + "Nether portal: " + wc.getDefaultNetherPortalDestination().getInfoString());
                }
                if (wc.getDefaultEndPortalDestination() == null) {
                    message(ChatColor.WHITE + "End portal: " + ChatColor.YELLOW + "None (will auto-detect when available)");
                } else {
                    message(ChatColor.WHITE + "End portal: " + wc.getDefaultEndPortalDestination().getInfoString());
                }
                //PvP
                if (wc.pvp) { 
                    message(ChatColor.WHITE + "PvP: " + ChatColor.GREEN + "Enabled");
                } else {
                    message(ChatColor.WHITE + "PvP: " + ChatColor.YELLOW + "Disabled");
                }
                //Bed respawn
                switch (wc.getBedRespawnMode()) {
                    case ENABLED:
                        message(ChatColor.WHITE + "Bed Respawn: " + ChatColor.GREEN + "Yes, players respawn at a bed or world anchor they set");
                        break;
                    case DISABLED:
                        message(ChatColor.WHITE + "Bed Respawn: " + ChatColor.RED + "No, players respawn at the world respawn point");
                        break;
                    case SAVE_ONLY:
                        message(ChatColor.WHITE + "Bed Respawn: " + ChatColor.GREEN + "Saved Only, players can set a bed but won't respawn in it");
                        break;
                }
                //Remember last position
                if (wc.rememberLastPlayerPosition) {
                    message(ChatColor.WHITE + "Remember last pos.: " + ChatColor.GREEN + "Yes, teleport to last known position");
                } else {
                    message(ChatColor.WHITE + "Remember last pos.: " + ChatColor.GREEN + "No, teleport to spawn point");
                }
                //Advancements
                if (wc.isAdvancementsEnabled()) {
                    message(ChatColor.WHITE+"Advancements: " + ChatColor.GREEN + "Enabled");
                } else {
                    message(ChatColor.WHITE+"Advancements: " + ChatColor.RED + "Disabled");
                }
                //Difficulty
                message(ChatColor.WHITE + "Difficulty: " + ChatColor.YELLOW + wc.difficulty.toString().toLowerCase());
                //Game mode
                GameMode mode = wc.gameMode;
                if (mode == null) {
                    message(ChatColor.WHITE + "Game mode: " + ChatColor.YELLOW + "Not set");
                } else {
                    message(ChatColor.WHITE + "Game mode: " + ChatColor.YELLOW + mode.name().toLowerCase());
                }
                //Time
                String timestr = wc.timeControl.getTime(info.time);
                message(ChatColor.WHITE + "Time: " + ChatColor.YELLOW + timestr);
                //Weather
                if (info.weather_endless) {
                    if (info.raining) {
                        if (info.thundering) {
                            message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Endless storm with lightning");
                        } else {
                            message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Endless rain and snow");
                        }
                    } else {
                        message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "No bad weather expected");
                    }
                } else {
                    if (info.raining) {
                        if (info.thundering) {
                            message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Stormy with lightning");
                        } else {
                            message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "Rain and snow");
                        }
                    } else {
                        message(ChatColor.WHITE + "Weather: " + ChatColor.YELLOW + "The sky is clear");
                    }
                }                            
                World w = Bukkit.getServer().getWorld(worldname);
                if (w != null) {
                    int playercount = w.getPlayers().size();
                    if (playercount > 0) {
                        String msg = ChatColor.WHITE + "Status: " + ChatColor.GREEN + "Loaded" + ChatColor.WHITE + " with ";
                        msg += playercount + ((playercount <= 1) ? " player" : " players");
                        message(msg);
                    } else {
                        message(ChatColor.WHITE + "Status: " + ChatColor.YELLOW + "Stand-by");
                    }
                } else {
                    message(ChatColor.WHITE + "Status: " + ChatColor.RED + "Unloaded");
                }
            }
        } else {
            message(ChatColor.RED + "World not found!");
        }
    }

    @Override
    public List<String> autocomplete() {
        return processWorldNameAutocomplete();
    }
}

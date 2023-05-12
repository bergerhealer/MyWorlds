package com.bergerkiller.bukkit.mw.commands;

import java.util.List;
import java.util.stream.Stream;

import com.bergerkiller.bukkit.common.utils.TimeUtil;
import com.bergerkiller.bukkit.mw.Util;
import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.TimeControl;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldInfo;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldTime extends Command {

    public WorldTime() {
        super(Permission.COMMAND_TIME, "world.time");
    }

    public void execute() {
        boolean lock = false;
        boolean useWorld = false;
        long time = -1;
        for (String command : args) {
            //Time reading
            if (command.equalsIgnoreCase("lock")) {
                lock = true;
            } else if (command.equalsIgnoreCase("locked")) {
                lock = true;
            } else if (command.equalsIgnoreCase("always")) {
                lock = true;
            } else if (command.equalsIgnoreCase("endless")) {
                lock = true;
            } else if (command.equalsIgnoreCase("l")) {
                lock = true;
            } else if (command.equalsIgnoreCase("-l")) {
                lock = true;    
            } else if (command.equalsIgnoreCase("stop")) {
                lock = true;
            } else if (command.equalsIgnoreCase("freeze")) {
                lock = true;
            } else {
                long newtime = TimeUtil.getTime(command);
                if (newtime != -1) {
                    time = newtime;
                } else {
                    //Used the last argument as command?
                    if (command == args[args.length - 1]) useWorld = true;
                }
            }
        }
        worldname = WorldManager.getWorldName(sender, args, useWorld);
        if (this.handleWorld()) {
            WorldConfig wc = WorldConfig.get(worldname);
            if (time == -1) {
                World w = WorldManager.getWorld(worldname);
                if (w == null) {
                    WorldInfo i = wc.getInfo();
                    if (i == null) {
                        time = 0;
                    } else {
                        time = i.time;
                    }
                } else {
                    time = w.getFullTime();
                }
            }
            if (args.length == 0) {
                message(ChatColor.YELLOW + "The current time of world '" + 
                        worldname + "' is " + Util.formatWorldTime(time));
            } else {
                TimeControl tc = wc.timeControl;
                boolean wasLocked = tc.isLocked();
                tc.setLocking(lock);
                tc.setTime(time);
                if (lock) {
                    if (wc.isLoaded()) {
                        message(ChatColor.GREEN + "Time of world '" + worldname + "' locked to " +
                                Util.formatWorldTime(time) + "!");
                    } else {
                        Localization.WORLD_NOTLOADED.message(sender, worldname);
                        message(ChatColor.YELLOW + "Time will be locked to " +
                                Util.formatWorldTime(time) + " as soon it is loaded!");
                    }
                } else {
                    World w = wc.getWorld();
                    if (w != null) {
                        if (wasLocked) {
                            message(ChatColor.GREEN + "Time of world '" + worldname + "' unlocked and set to " +
                                    Util.formatWorldTime(time) + "!");
                        } else {
                            message(ChatColor.GREEN + "Time of world '" + worldname + "' set to " +
                                    Util.formatWorldTime(time) + "!");
                        }
                    } else {
                        Localization.WORLD_NOTLOADED.message(sender, worldname);
                        message(ChatColor.YELLOW + "Time has not been changed!");
                    }
                }
            }
        }
    }

    @Override
    public List<String> autocomplete() {
        return this.processAutocomplete(Stream.of(
                "always", "dawn", "sunrise", "morning", "day", "midday",
                "noon", "afternoon", "evening", "sunset", "dusk", "night",
                "midnight"));
    }
}

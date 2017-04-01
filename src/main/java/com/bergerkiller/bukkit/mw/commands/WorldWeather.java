package com.bergerkiller.bukkit.mw.commands;

import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.World;

import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.WeatherState;
import com.bergerkiller.bukkit.mw.Permission;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

public class WorldWeather extends Command {

    public WorldWeather() {
        super(Permission.COMMAND_WEATHER, "world.weather");
    }

    public void execute() {
        if (args.length == 0) {
            showInv();
            return;
        }

        boolean hasWorldArgument = true;
        boolean setStorm = false;
        boolean setSun = false;
        boolean setHold = false;
        boolean setThunder = false;
        for (int i = 0; i < args.length; i++) {
            String command = args[i];
            if (command.equalsIgnoreCase("hold")) {
                setHold = true;
            } else if (command.toLowerCase(Locale.ENGLISH).contains("lock")) { 
                setHold = true;
            } else if (command.equalsIgnoreCase("always")) {
                setHold = true;
            } else if (command.equalsIgnoreCase("endless")) { 
                setHold = true;
            } else if (command.equalsIgnoreCase("forever")) { 
                setHold = true;
            } else if (command.equalsIgnoreCase("clear")) {
                setSun = true;
            } else if (command.equalsIgnoreCase("sun")) {
                setSun = true;
            } else if (command.equalsIgnoreCase("sunny")) { 
                setSun = true;
            } else if (command.equalsIgnoreCase("storm")) {
                setStorm = true;
                setThunder = true;
            } else if (command.equalsIgnoreCase("stormy")) {
                setStorm = true;
                setThunder = true;
            } else if (command.equalsIgnoreCase("rain")) {
                setStorm = true;
            } else if (command.equalsIgnoreCase("rainy")) {
                setStorm = true;
            } else if (command.equalsIgnoreCase("snow")) {
                setStorm = true;
            } else if (command.equalsIgnoreCase("snowy")) {
                setStorm = true;
            } else if (command.equalsIgnoreCase("thunder")) {
                setThunder = true;
            } else if (command.equalsIgnoreCase("lightning")) {
                setThunder = true;
            } else if (command.equalsIgnoreCase("heavy")) {
                setThunder = true;
            } else if (command.equalsIgnoreCase("big")) {
                setThunder = true;
            } else if (command.equalsIgnoreCase("huge")) {
                setThunder = true;
            } else {
                continue;
            }
            //Used the last argument as command?
            if (i == (args.length - 1)) hasWorldArgument = false;
        }

        String worldname = WorldManager.getWorldName(sender, args, hasWorldArgument);
        if (worldname == null) {
            message(ChatColor.RED + "World not found!");
            return;
        }

        WorldConfig wc = WorldConfig.get(worldname);
        World w = wc.getWorld();
        if (w == null) {
            message(ChatColor.RED + "World: '" + worldname + "' is not loaded");
            return;
        }

        // Translate these booleans to a WeatherState as BKC uses
        WeatherState weather_old = WorldUtil.getWeatherState(w);
        WeatherState weather = WorldUtil.getWeatherState(w);
        if (setStorm) {
            if (setThunder) {
                weather = WeatherState.STORM;
            } else {
                weather = WeatherState.RAIN;
            }
        } else if (setSun) {
            weather = WeatherState.CLEAR;
        }
        boolean weather_changed = (weather_old != weather) || setHold;
        if (!weather_changed) {
            message(ChatColor.YELLOW + "No changes occurred; world weather was already set up this way!");
            return;
        }

        WorldUtil.setWeatherState(w, weather);
        if (setHold) {
            WorldUtil.setWeatherDuration(w, Integer.MAX_VALUE);
        }

        // Send an appropriate message
        if (setStorm) {
            if (setHold) {
                if (setThunder) {
                    message(ChatColor.GREEN + "You started an endless storm on world: '" + worldname + "'!");
                } else {
                    message(ChatColor.GREEN + "You started never-ending rainfall and snowfall on world: '" + worldname + "'!");
                }
            } else {
                if (setThunder) {
                    message(ChatColor.GREEN + "You started a storm on world: '" + worldname + "'!");
                } else {
                    message(ChatColor.GREEN + "You started rainfall and snowfall on world: '" + worldname + "'!");
                }
            }
        } else if (setSun) {
            if (setHold) {
                message(ChatColor.GREEN + "You stopped all storms and rainfall on world: '" + worldname + "' forever!");
            } else {
                message(ChatColor.GREEN + "You stopped a storm on world: '" + worldname + "'!");
            }
        } else if (setHold) {
            message(ChatColor.GREEN + "Weather changes on world: '" + worldname + "' are now being prevented!");
        }
    }
}

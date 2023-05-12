package com.bergerkiller.bukkit.mw.papi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.bergerkiller.bukkit.mw.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.Common;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.common.wrappers.WeatherState;
import com.bergerkiller.bukkit.mw.Localization;
import com.bergerkiller.bukkit.mw.WorldConfig;
import com.bergerkiller.bukkit.mw.WorldManager;

/**
 * Handles the placeholders MyWorlds provides
 */
public class MyWorldsPAPIHandlerImpl implements MyWorldsPAPIHandler {
    private final Map<String, SingleHandler> handlers = new HashMap<>();
    private final List<String> names = new ArrayList<>();

    public MyWorldsPAPIHandlerImpl() {
        register("world_name", (world, player) -> world.worldname);
        register("world_alias", (world, player) -> world.alias);
        register("world_environment", (world, player) -> world.worldmode.getPAPIEnvName());
        register("world_type", (world, player) -> world.worldmode.getPAPITypeName());
        register("world_time", (world, player) -> Util.formatWorldTime(world.getWorld().getFullTime()));
        register("world_difficulty", (world, player) -> world.difficulty.toString().toLowerCase(Locale.ENGLISH));
        register("world_gamemode", (world, player) -> (world.gameMode == null)
                ? "none" : world.gameMode.name().toLowerCase(Locale.ENGLISH));
        register("world_weather", (world, player) -> {
            return stringifyWeatherState(player, WorldUtil.getWeatherState(world.getWorld()));
        });
        register("world_weather_duration", (world, player) -> {
            int duration = WorldUtil.getWeatherDuration(world.getWorld());
            return getDurationString(duration);
        });
        register("world_weather_forecast", (world, player) -> {
            if (isWeatherForever(WorldUtil.getWeatherDuration(world.getWorld()))) {
                WeatherState state = WorldUtil.getWeatherState(world.getWorld());
                return stringifyWeatherState(player, state);
            } else {
                return stringifyWeatherState(player, WorldUtil.getFutureWeatherState(world.getWorld()));
            }
        });
    }

    private void register(String name, SingleHandler handler) {
        handlers.put(name, handler);
        names.add(name);
    }

    @Override
    public List<String> getPlaceholderNames() {
        return names;
    }

    @Override
    public String getPlaceholder(OfflinePlayer player, String identifier) {
        // Promote to online Player if online
        // TODO: Is this really needed though?
        if (!(player instanceof Player)) {
            Player onlinePlayer = Bukkit.getPlayer(player.getUniqueId());
            if (onlinePlayer != null) {
                player = onlinePlayer;
            }
        }

        // If identifier starts with 'world_' and has a [] block, resolve world name
        WorldConfig configOfWorld = null;
        if (identifier.startsWith("world_") && identifier.endsWith("]")) {
            int openerIdx = identifier.lastIndexOf('[', identifier.length()-2);
            if (openerIdx != -1) {
                String options = identifier.substring(openerIdx, identifier.length()-1);
                identifier = identifier.substring(0, openerIdx);

                // Try to match the world name
                configOfWorld = WorldConfig.getIfExists(options);
            }
        }

        SingleHandler handler = handlers.get(identifier);
        if (handler != null) {
            if (configOfWorld == null) {
                configOfWorld = WorldConfig.get(WorldManager.getPlayerCurrentWorld(player));
            }
            if (configOfWorld == null) {
                return "unknown world";
            } else {
                return handler.get(configOfWorld, player);
            }
        }

        return null;
    }

    @FunctionalInterface
    private static interface SingleHandler {
        String get(WorldConfig world, OfflinePlayer player);
    }

    private static final long UNIT_SECONDS = 20L;
    private static final long UNIT_MINUTES = 60L * UNIT_SECONDS;
    private static final long UNIT_HOURS = 60L * UNIT_MINUTES;
    private static final long UNIT_DAYS = 24L * UNIT_HOURS;

    private static String getDurationString(long ticks) {
        if (isWeatherForever(ticks)) {
            return Localization.PAPI_TIME_FOREVER.get();
        } else if (ticks < UNIT_MINUTES) {
            return Localization.PAPI_TIME_SECONDS.get(Long.toString(ticks / UNIT_SECONDS));
        } else if (ticks < (5L * UNIT_MINUTES)) {
            String num_minutes = Long.toString(ticks / UNIT_MINUTES);
            String num_seconds = Long.toString((ticks % UNIT_MINUTES) / UNIT_SECONDS);
            return Localization.PAPI_TIME_MINUTES.get(num_minutes) +
                   ", " +
                   Localization.PAPI_TIME_SECONDS.get(num_seconds);
        } else if (ticks < UNIT_HOURS) {
            return Localization.PAPI_TIME_MINUTES.get(Long.toString(ticks / UNIT_MINUTES));
        } else if (ticks < (5L * UNIT_HOURS)) {
            String num_hours = Long.toString(ticks / UNIT_HOURS);
            String num_minutes = Long.toString((ticks % UNIT_HOURS) / UNIT_MINUTES);
            return Localization.PAPI_TIME_HOURS.get(num_hours) +
                   ", " +
                   Localization.PAPI_TIME_MINUTES.get(num_minutes);
        } else if (ticks < UNIT_DAYS) {
            return Localization.PAPI_TIME_HOURS.get(Long.toString(ticks / UNIT_HOURS));
        } else if (ticks < (5L * UNIT_DAYS)) {
            String num_days = Long.toString(ticks / UNIT_DAYS);
            String num_hours = Long.toString((ticks % UNIT_DAYS) / UNIT_HOURS);
            return Localization.PAPI_TIME_DAYS.get(num_days) +
                   ", " +
                   Localization.PAPI_TIME_HOURS.get(num_hours);
        } else {
            return Localization.PAPI_TIME_DAYS.get(Long.toString(ticks / UNIT_DAYS));
        }
    }

    private static boolean isWeatherForever(long duration) {
        return duration == 0L || duration >= (30L * UNIT_DAYS);
    }

    private static String stringifyWeatherState(OfflinePlayer player, WeatherState state) {
        double temperature = 0.5; // rain default for offline players
        if (player instanceof Player) {
            Player p = (Player) player;
            Location loc = p.getLocation();
            if (HAS_3D_GET_BIOME) {
                temperature = get3DTemp(p.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            } else {
                temperature = get2DTemp(p.getWorld(), loc.getBlockX(), loc.getBlockZ());
            }
        }

        if (temperature > 0.95) {
            // Desert climate. There is no precipitation
            switch (state) {
            case CLEAR: return Localization.PAPI_WEATHER_CLEAR.get();
            case RAIN: return Localization.PAPI_WEATHER_DESERT_RAIN.get();
            case STORM: return Localization.PAPI_WEATHER_DESERT_STORM.get();
            default: return state.name().toString().toLowerCase(Locale.ENGLISH);
            }
        }

        if (temperature < 0.15) {
            // Snow climate.
            switch (state) {
            case CLEAR: return Localization.PAPI_WEATHER_CLEAR.get();
            case RAIN: return Localization.PAPI_WEATHER_SNOW.get();
            case STORM: return Localization.PAPI_WEATHER_SNOW_STORM.get();
            default: return state.name().toString().toLowerCase(Locale.ENGLISH);
            }
        }

        // Default climate - rain
        switch (state) {
        case CLEAR: return Localization.PAPI_WEATHER_CLEAR.get();
        case RAIN: return Localization.PAPI_WEATHER_RAIN.get();
        case STORM: return Localization.PAPI_WEATHER_STORM.get();
        default: return state.name().toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private static final boolean HAS_3D_GET_BIOME = Common.evaluateMCVersion(">=", "1.15");

    private static double get3DTemp(World w, int x, int y, int z) {
        return w.getTemperature(x, y, z);
    }

    @SuppressWarnings("deprecation")
    private static double get2DTemp(World w, int x, int z) {
        return w.getTemperature(x, z);
    }
}

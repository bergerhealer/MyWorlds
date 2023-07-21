package com.bergerkiller.bukkit.mw;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.localization.LocalizationEnum;

public class Localization extends LocalizationEnum {
    public static final Localization COMMAND_NOPERM = new Localization("command.nopermission", ChatColor.DARK_RED + "You do not have permission to use this command!");
    public static final Localization COMMAND_UNKNOWN = new Localization("command.unknown", ChatColor.RED + "Unknown command: %0%");
    public static final Localization COMMAND_HELP = new Localization("command.help", ChatColor.RED + "For command help, use /help my_worlds");
    public static final Localization COMMAND_LASTPOSITION_TP_SUCCESS = new Localization("command.lastposition.tp.success", ChatColor.YELLOW + "Teleporting to last position of player '%0%' on world '%1%'!");
    public static final Localization COMMAND_LASTPOSITION_TP_NEVERVISITED = new Localization("command.lastposition.tp.nevervisited", ChatColor.RED + "Player %0% never visited world %1%!");
    public static final Localization COMMAND_LASTPOSITION_TP_SAME = new Localization("command.lastposition.tp.same", ChatColor.YELLOW + "You are already in this world");
    public static final Localization WORLD_ENTER = new Localization("world.enter", ChatColor.GREEN + "You teleported to world '%0%'!");
    public static final Localization WORLD_NOACCESS = new Localization("world.noaccess", ChatColor.DARK_RED + "You are not allowed to enter this world!");
    public static final Localization WORLD_NOCHATACCESS = new Localization("world.nochataccess", ChatColor.DARK_RED + "You are not allowed to use the chat in this world!");
    public static final Localization WORLD_NOTFOUND = new Localization("world.notfound", ChatColor.RED + "World not found!");
    public static final Localization WORLD_NOTLOADED = new Localization("world.notloaded", ChatColor.YELLOW + "World '%0%' is not loaded!");
    public static final Localization WORLD_NOUSE = new Localization("world.nouse", ChatColor.RED + "You are not allowed to use this in this world!");
    public static final Localization WORLD_NOBUILD = new Localization("world.nobuild", ChatColor.RED + "You are not allowed to place blocks in this world!");
    public static final Localization WORLD_NOBREAK = new Localization("world.nobreak", ChatColor.RED + "You are not allowed to break blocks in this world!");
    public static final Localization WORLD_FULL = new Localization("world.full", ChatColor.RED + "World %0% is full!");
    public static final Localization WORLD_JOIN_REMOVED = new Localization("world.joinremoved", ChatColor.RED + "The world you were last on no longer exists!");
    public static final Localization PORTAL_ENTER = new Localization("portal.enter", ChatColor.GREEN + "You teleported to '%0%'!");
    public static final Localization PORTAL_NOTFOUND = new Localization("portal.notfound", ChatColor.RED + "Portal '%0%' was not found!");
    public static final Localization PORTAL_NODESTINATION = new Localization("portal.nodestination", ChatColor.YELLOW + "This portal has no destination!");
    public static final Localization PORTAL_NOACCESS = new Localization("portal.noaccess", ChatColor.YELLOW + "You are not allowed to enter this portal!");
    public static final Localization PORTAL_BUILD_NOPERM = new Localization("portal.nobuildperm", ChatColor.DARK_RED + "You do not have permission to create new portals!");
    public static final Localization PORTAL_CREATE_TO = new Localization("portal.create.to", ChatColor.GREEN + "You created a new portal to " + ChatColor.WHITE + "%0%" + ChatColor.GREEN + "!");
    public static final Localization PORTAL_REMOVED = new Localization("portal.removed", ChatColor.RED + "You removed portal " + ChatColor.WHITE + "%0%" + ChatColor.RED + "!");
    public static final Localization PORTAL_CREATE_MISSING = new Localization("portal.create.missing", ChatColor.YELLOW + "The destination portal still has to be placed");
    public static final Localization PORTAL_CREATE_END = new Localization("portal.create.end", ChatColor.GREEN + "You created a new destination portal!");
    public static final Localization PORTAL_CREATE_REJOIN = new Localization("portal.create.rejoin", ChatColor.GREEN + "This is a rejoin portal: players will be sent to the last position they were at, if known");
    public static final Localization PORTAL_PREPARING = new Localization("portal.preparing", ChatColor.YELLOW + "The other portal is being prepared (this can take a while...)");
    public static final Localization PORTAL_LINK_UNAVAILABLE = new Localization("portal.link.unavailable", ChatColor.YELLOW + "The other end of this portal is missing, and you cannot create it");
    public static final Localization PORTAL_LINK_FAILED = new Localization("portal.link.unavailable", ChatColor.YELLOW + "The other end of this portal could not be generated");
    public static final Localization PAPI_WEATHER_CLEAR = new Localization("papi.weather.clear", "clear");
    public static final Localization PAPI_WEATHER_DESERT_RAIN = new Localization("papi.weather.desert_rain", "humid");
    public static final Localization PAPI_WEATHER_DESERT_STORM = new Localization("papi.weather.desert_storm", "windy");
    public static final Localization PAPI_WEATHER_RAIN = new Localization("papi.weather.rain", "raining");
    public static final Localization PAPI_WEATHER_SNOW = new Localization("papi.weather.snow", "snowing");
    public static final Localization PAPI_WEATHER_STORM = new Localization("papi.weather.storm", "storming");
    public static final Localization PAPI_WEATHER_SNOW_STORM = new Localization("papi.weather.snow_storm", "snow storm");
    public static final Localization PAPI_TIME_FOREVER = new Localization("papi.time.forever", "forever");
    public static final Localization PAPI_TIME_SECONDS = new Localization("papi.time.seconds", "%0% seconds") {
        @Override
        public void writeDefaults(ConfigurationNode config, String path) {
            ConfigurationNode node = config.getNode(path);
            node.set("default", this.getDefault());
            node.set("1", "1 second");
        }
    };
    public static final Localization PAPI_TIME_MINUTES = new Localization("papi.time.minutes", "%0% minutes") {
        @Override
        public void writeDefaults(ConfigurationNode config, String path) {
            ConfigurationNode node = config.getNode(path);
            node.set("default", this.getDefault());
            node.set("1", "1 minute");
        }
    };
    public static final Localization PAPI_TIME_HOURS = new Localization("papi.time.hours", "%0% hours") {
        @Override
        public void writeDefaults(ConfigurationNode config, String path) {
            ConfigurationNode node = config.getNode(path);
            node.set("default", this.getDefault());
            node.set("1", "1 hour");
        }
    };
    public static final Localization PAPI_TIME_DAYS = new Localization("papi.time.days", "%0% days") {
        @Override
        public void writeDefaults(ConfigurationNode config, String path) {
            ConfigurationNode node = config.getNode(path);
            node.set("default", this.getDefault());
            node.set("1", "1 day");
        }
    };
    public static final Localization PAPI_WORLDTYPE_NORMAL = new Localization("papi.worldtype.normal", "Overworld");
    public static final Localization PAPI_WORLDTYPE_THE_END = new Localization("papi.worldtype.the_end", "The End");
    public static final Localization PAPI_WORLDTYPE_THE_NETHER = new Localization("papi.worldtype.the_nether", "The Nether");
    public static final Localization PAPI_WORLDTYPE_FLAT_NORMAL = new Localization("papi.worldtype.flat_normal", "Flat Overworld");
    public static final Localization PAPI_WORLDTYPE_FLAT_END = new Localization("papi.worldtype.flat_end", "Flat End");
    public static final Localization PAPI_WORLDTYPE_FLAT_NETHER = new Localization("papi.worldtype.flat_nether", "Flat Nether");

    private Localization(String name, String defValue) {
        super(name, defValue);
    }

    @Override
    public String get(String... arguments) {
        return MyWorlds.plugin.getLocale(this.getName(), arguments);
    }
}

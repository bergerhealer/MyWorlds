package com.bergerkiller.bukkit.mw;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.localization.LocalizationEnum;

public class Localization extends LocalizationEnum {
	public static final Localization COMMAND_NOPERM = new Localization("command.nopermission", ChatColor.DARK_RED + "You do not have permission to use this command!");
	public static final Localization COMMAND_UNKNOWN = new Localization("command.unknown", ChatColor.RED + "Unknown command: %0%");
	public static final Localization COMMAND_HELP = new Localization("command.help", ChatColor.RED + "For command help, use /help myworlds");
	public static final Localization WORLD_ENTER = new Localization("world.enter", ChatColor.GREEN + "You teleported to world '%0%'!");
	public static final Localization WORLD_NOACCESS = new Localization("world.noaccess", ChatColor.DARK_RED + "You are not allowed to enter this world!");
	public static final Localization WORLD_NOCHATACCESS = new Localization("world.nochataccess", ChatColor.DARK_RED + "You are not allowed to use the chat in this world!");
	public static final Localization WORLD_NOTFOUND = new Localization("world.notfound", ChatColor.RED + "World not found!");
	public static final Localization WORLD_NOTLOADED = new Localization("world.notloaded", ChatColor.YELLOW + "World '%0%' is not loaded!");
	public static final Localization WORLD_NOUSE = new Localization("world.nouse", ChatColor.RED + "You are not allowed to use this in this world!");
	public static final Localization WORLD_NOBUILD = new Localization("world.nobuild", ChatColor.RED + "You are not allowed to place blocks in this world!");
	public static final Localization WORLD_NOBREAK = new Localization("world.nobreak", ChatColor.RED + "cYou are not allowed to break blocks in this world!");
	public static final Localization PORTAL_ENTER = new Localization("portal.enter", ChatColor.GREEN + "You teleported to '%0%'!");
	public static final Localization PORTAL_NOTFOUND = new Localization("portal.notfound", ChatColor.RED + "Portal '%0%' was not found!");
	public static final Localization PORTAL_NODESTINATION = new Localization("portal.nodestination", ChatColor.YELLOW + "This portal has no destination!");
	public static final Localization PORTAL_NOACCESS = new Localization("portal.noaccess", ChatColor.YELLOW + "You are not allowed to enter this portal!");
	public static final Localization PORTAL_CREATE_TO = new Localization("portal.create.to", ChatColor.GREEN + "You created a new portal to " + ChatColor.WHITE + "%0%" + ChatColor.GREEN + "!");
	public static final Localization PORTAL_CREATE_MISSING = new Localization("portal.create.missing", ChatColor.YELLOW + "The destination portal still has to be placed");
	public static final Localization PORTAL_CREATE_END = new Localization("portal.create.end", ChatColor.GREEN + "You created a new destination portal!");

	private Localization(String name, String defValue) {
		super(name, defValue);
	}

	@Override
	public String get(String... arguments) {
		return MyWorlds.plugin.getLocale(this.getName(), arguments);
	}
}

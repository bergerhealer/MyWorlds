package com.bergerkiller.bukkit.mw;

import org.bukkit.command.CommandSender;

import com.bergerkiller.bukkit.common.localization.ILocalizationDefault;

public enum Localization implements ILocalizationDefault {
	COMMAND_NOPERM("command.nopermission", "§4You do not have permission to use this command!"),
	WORLD_ENTER("world.enter", "§aYou teleported to world '%0%'!"),
	WORLD_NOACCESS("world.noaccess", "§4You are not allowed to enter this world!"),
	WORLD_NOCHATACCESS("world.nochataccess", "§4You are not allowed to use the chat in this world!"),
	WORLD_NOTFOUND("world.notfound", "§cWorld not found!"),
	WORLD_NOTLOADED("world.notloaded", "§eWorld '%0%' is not loaded!"),
	PORTAL_ENTER("portal.enter", "§aYou teleported to '%0%'!"),
	PORTAL_NOTFOUND("portal.notfound", "§cPortal not found!"),
	PORTAL_NODESTINATION("portal.nodestination", "§eThis portal has no destination!"),
	PORTAL_NOACCESS("portal.noaccess", "§4You are not allowed to enter this portal!");

	private final String name;
	private final String defValue;

	private Localization(String name, String defValue) {
		this.name = name;
		this.defValue = defValue;
	}

	@Override
	public String getDefault() {
		return this.defValue;
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Sends this Localization message to the sender specified
	 * 
	 * @param sender to send to
	 * @param arguments for the node
	 */
	public void message(CommandSender sender, String...arguments) {
		sender.sendMessage(get(arguments));
	}

	/**
	 * Gets the locale value for this Localization node
	 * 
	 * @param arguments for the node
	 * @return Locale value
	 */
	public String get(String... arguments) {
		return MyWorlds.plugin.getLocale(this.getName(), arguments);
	}
}

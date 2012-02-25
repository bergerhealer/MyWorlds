package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.utils.CommonUtil;

public class Localization {
	private static HashMap<String, String> messages = new HashMap<String, String>();
	private static String getRaw(String message) {
		return message.replace("\n", "\\n").replace('§', '&');
	}
	private static String getFormatted(String message) {
		return message.replace("\\n", "\n").replace('&', '§');
	}
	static {
		messages.put("command.nopermission", "§4You do not have permission to use this command!");
		messages.put("world.enter", "§aYou teleported to world '%name%'!");
		messages.put("world.enter.world1", "§aYou teleported to world 1, have a nice stay!");
		messages.put("world.noaccess", "§4You are not allowed to enter this world!");
		messages.put("world.nochataccess", "§4You are not allowed to use the chat in this world!");
		messages.put("world.notfound", "§cWorld not found!");
		messages.put("world.notloaded", "§eWorld '%name%' is not loaded!");
		messages.put("portal.enter", "§aYou teleported to '%name%'!");
		messages.put("portal.enter.portal1", "§aYou teleported to portal 1, have a nice stay!");
		messages.put("portal.notfound", "§cPortal not found!");
		messages.put("portal.nodestination", "§eThis portal has no destination!");
		messages.put("portal.noaccess", "§4You are not allowed to enter this portal!");
		messages.put("portal.noaccess", "§eYou are not allowed to enter this portal!");
		messages.put("help.world.list", "§e/world list - Lists all worlds of this server");
		messages.put("help.world.info", "§e/world info ([world]) - Shows information about a world");
		messages.put("help.world.portals", "§e/world portals ([world]) - Lists all the portals");
		messages.put("help.world.load", "§e/world load [world] - Loads a world into memory");
		messages.put("help.world.unload", "§e/world unload [world] - Unloads a world from memory");
		messages.put("help.world.create", "§e/world create [world] ([seed]) - Creates a new world");
		messages.put("help.world.delete", "§e/world delete [world] - Permanently deletes a world");
		messages.put("help.world.spawn", "§e/world spawn [world] - Teleport to the world spawn");
		messages.put("help.world.copy", "§e/world copy [world] [newname] - Copies a world under a new name");
		messages.put("help.world.evacuate", "§e/world evacuate [world]&n&e    Removes all players by teleportation or kicking");
		messages.put("help.world.repair", "§e/world repair [world] ([Seed]) - Repairs the world");
		messages.put("help.world.save", "§e/world save [world] - Saves the world");
		messages.put("help.world.togglepvp", "§e/world togglepvp ([world]) - Toggles PvP on or off");
		messages.put("help.world.weather", "§e/world weather [states] ([world])\n§e    Changes and holds weather states");
		messages.put("help.world.time", "§e/world time ([lock/always]) [time] ([world])\n§e    Changes and locks the time");
		messages.put("help.world.allowspawn", "§e/world allowspawn [creature] ([world])\n§e    Allow creatures to spawn");
		messages.put("help.world.denyspawn", "§e/world denyspawn [creature] ([world])\n§e    Deny creatures from spawning");
		messages.put("help.world.setportal", "§e/world setportal [destination] ([world]) - Set default portal destination");
		messages.put("help.world.setspawn", "§e/world setspawn - Sets the world spawn point to your location");
		messages.put("help.world.gamemode", "§e/world gamemode [mode] ([world]) - Sets or clears the gamemode for a world");
		messages.put("help.world.togglespawnloaded", "§e/world togglespawnloaded ([world])\n§e    Toggles keep-spawn-in-memory on or off");
		messages.put("help.world.difficulty", "§e/world difficuly [difficulty] ([world])\n§e    Sets or displays the difficulty");
		messages.put("help.world.op", "§e/world op [player] ([world]) - Add an operator for a world");
		messages.put("help.world.deop", "§e/world deop [player] ([world]) - Remove an operator for a world");
		messages.put("help.world.config", "§e/world [save/load] - Loads or saves the world configuration");
	}
	
	public static String getWorldEnter(World w) {
		return getWorldEnter(w.getName());
	}
	public static String getPortalEnter(Portal portal) {
		return getPortalEnter(portal.getDestinationName(), portal.getDestinationDisplayName());
	}	
	public static String getWorldNotLoaded(String worldname) {
		String msg = get("world.notloaded");
		return msg.replace("%name%", worldname);
	}
	public static String getWorldEnter(String worldname) {
		String msg = get("world.enter." + worldname, get("world.enter", ""));
		return msg.replace("%name%", worldname);
	}
	public static String getPortalEnter(String portalname) {
		return getPortalEnter(portalname, portalname);
	}
	public static String getPortalEnter(String portalname, String portaldispname) {
		String msg = get("portal.enter." + portalname, get("portal.enter", ""));
		return msg.replace("%name%", portaldispname);
	}
	
	public static void message(Object sender, String key) {
		CommonUtil.sendMessage(sender, get(key, ""));
	}
	public static void message(Object sender, String key, String def) {
		CommonUtil.sendMessage(sender, get(key, def));
	}
		
	public static String get(String key) {
		return get(key, "");
	}
	public static String get(String key, String def) {
		String message = messages.get(key.toLowerCase());
		if (message == null) {
			return def;
		} else {
			return message;
		}
	}
	
	public static void init(JavaPlugin plugin, String localname) {
		File locale = new File(plugin.getDataFolder() + File.separator + "locale");
		locale.mkdirs();
		
		//Write out the default
		File defaultloc = new File(locale + File.separator + "default.txt");
		if (!defaultloc.exists()) {
			SafeWriter writer = new SafeWriter(locale + File.separator + "default.txt");
			writer.writeLine("# This is a localization file. In here key-sentence pairs are stored.");
			writer.writeLine("# &x characters can be used for special text effects and colors. (e.g. &f for white)");
			writer.writeLine("# &n can be used to indicate a new line");
			writer.writeLine("# Remove the 'default' localization file to re-generate it. (new nodes could be added)");
			for (Map.Entry<String, String> message : messages.entrySet()) {
				writer.writeLine(message.getKey() + ": " + getRaw(message.getValue()));
			}
			writer.close();
		}
		
		//Read the locale
		for (String textline : SafeReader.readAll(locale + File.separator + localname + ".txt", true)) {
			int index = textline.indexOf(":");
			if (index > 0 && index < textline.length() - 2) {
				String key = textline.substring(0, index);
				String message = textline.substring(index + 2);
				messages.put(key.toLowerCase(), getFormatted(message));
			}
		}
	}
	public static void deinit() {
		messages.clear();
		messages = null;
	}

}

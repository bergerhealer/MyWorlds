package com.bergerkiller.bukkit.mw;

import java.io.File;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class Localization {
	private static HashMap<String, String> messages = new HashMap<String, String>();
	
	public static String getWorldEnter(World w) {
		return getWorldEnter(w.getName());
	}
	public static String getPortalEnter(Portal portal) {
		return getPortalEnter(portal.getDestinationName());
	}	
	public static String getWorldEnter(String worldname) {
		String msg = get("world.enter." + worldname, get("world.enter", ""));
		return msg.replace("%name%", worldname);
	}
	public static String getPortalEnter(String portalname) {
		String msg = get("portal.enter." + portalname, get("portal.enter", ""));
		return msg.replace("%name%", portalname);
	}
	
	public static void message(Object sender, String key) {
		MyWorlds.message(sender, get(key, ""));
	}
	public static void message(Object sender, String key, String def) {
		MyWorlds.message(sender, get(key, def));
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
			writer.writeLine("command.nopermission: &4You do not have permission to use this command!");
			writer.writeLine("world.enter: &aYou teleported to world '%name%'!");
			writer.writeLine("world.enter.world1: &aYou teleported to world 1, have a nice stay!");
			writer.writeLine("world.noaccess: &4You are not allowed to enter this world!");
			writer.writeLine("portal.enter: &aYou teleported to '%name%'!");
			writer.writeLine("portal.enter.portal1: &aYou teleported to portal 1, have a nice stay!");
			writer.writeLine("portal.notfound: &cPortal not found!");
			writer.writeLine("portal.nodestination: &eThis portal has no destination!");
			writer.writeLine("portal.noaccess: &4You are not allowed to enter this portal!");
			writer.writeLine("portal.noaccess: &eYou are not allowed to enter this portal!");
			writer.writeLine("help.world.list: &e/world list - Lists all worlds of this server");
			writer.writeLine("help.world.info: &e/world info ([worldname]) - Shows information about a world");
			writer.writeLine("help.world.portals: &e/world portals ([worldname]) - Lists all the portals");
			writer.writeLine("help.world.load: &e/world load [worldname] - Loads a world into memory");
			writer.writeLine("help.world.unload: &e/world unload [worldname] - Unloads a world from memory");
			writer.writeLine("help.world.create: &e/world create [worldname] ([seed]) - Creates a new world");
			writer.writeLine("help.world.delete: &e/world delete [worldname] - Permanently deletes a world");
			writer.writeLine("help.world.spawn: &e/world spawn [worldname] - Teleport to the world spawn");
			writer.writeLine("help.world.copy: &e/world copy [worldname] [newname] - Copies a world under a new name");
			writer.writeLine("help.world.evacuate: &e/world evacuate [worldname]&n&e - Removes all players by teleportation or kicking");
			writer.writeLine("help.world.repair: &e/world repair [worldname] ([Seed]) - Repairs the world");
			writer.writeLine("help.world.save: &e/world save [worldname] - Saves the world");
			writer.writeLine("help.world.togglepvp: &e/world togglepvp ([world]) - Toggles PvP on or off");
			writer.writeLine("help.world.weather: &e/world weather [states] ([worldname])&n&e    Changes and holds weather states");
			writer.writeLine("help.world.time: &e/world time ([lock/always]) [time] ([worldname])&n&e    Changes and locks the time");
			writer.writeLine("help.world.allowspawn: &e/world allowspawn [creature] ([worldname])&n&e    Allow creatures to spawn");
			writer.writeLine("help.world.denyspawn: &e/world denyspawn [creature] ([worldname])&n&e    Deny creatures from spawning");
			writer.writeLine("help.world.setportal: &e/world setportal [destination] ([worldname]) - Set default portal destination");
			writer.writeLine("help.world.setspawn: &e/world setspawn - Sets the world spawn point to your location");
			writer.writeLine("help.world.gamemode: &e/world gamemode [mode] ([worldname]) - Sets or clears the gamemode for a world");
			writer.close();
		}
		
		//Read the locale
		for (String textline : SafeReader.readAll(locale + File.separator + localname + ".txt", true)) {
			int index = textline.indexOf(":");
			if (index > 0 && index < textline.length() - 2) {
				String key = textline.substring(0, index);
				String message = textline.substring(index + 2);
				index = 0;
				while (true) {
					index = message.indexOf("&", index);
					if (index >= 0 && index < message.length() - 1) {
						String n = message.substring(index + 1, index + 2);
						String repl = "&" + n;
						//set repl
						if (n.equals("0")) {
							repl = ChatColor.BLACK.toString();
						} else if (n.equals("1")) {
							repl = ChatColor.DARK_BLUE.toString();
						} else if (n.equals("2")) {
							repl = ChatColor.DARK_GREEN.toString();
						} else if (n.equals("3")) {
							repl = ChatColor.DARK_AQUA.toString();
						} else if (n.equals("4")) {
							repl = ChatColor.DARK_RED.toString();
						} else if (n.equals("5")) {
							repl = ChatColor.DARK_PURPLE.toString();
						} else if (n.equals("6")) {
							repl = ChatColor.GOLD.toString();
						} else if (n.equals("7")) {
							repl = ChatColor.GRAY.toString();
						} else if (n.equals("8")) {
							repl = ChatColor.DARK_GRAY.toString();
						} else if (n.equals("9")) {
							repl = ChatColor.BLUE.toString();
						} else if (n.equals("a")) {
							repl = ChatColor.GREEN.toString();
						} else if (n.equals("b")) {
							repl = ChatColor.AQUA.toString();
						} else if (n.equals("c")) {
							repl = ChatColor.RED.toString();
						} else if (n.equals("d")) {
							repl = ChatColor.LIGHT_PURPLE.toString();
						} else if (n.equals("e")) {
							repl = ChatColor.YELLOW.toString();
						} else if (n.equals("f")) {
							repl = ChatColor.WHITE.toString();
						} else if (n.equals("n")) {
							repl = "\n";
						}
						//repl
						message = message.substring(0, index) + repl + message.substring(index + 2);
						messages.put(key, message);
					} else {
						break;
					}
					index += 1;
				}
				messages.put(key.toLowerCase(), message);
			}
		}
	}

}

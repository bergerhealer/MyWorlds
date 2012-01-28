package com.bergerkiller.bukkit.mw;

import java.util.ArrayList;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Util {

	public static boolean isSolid(Block b, BlockFace direction) {
		int maxwidth = 10;
		while (true) {
			int id = b.getTypeId();
			if (id == 0) return false;
			if (id != 9 && id != 8) return true;
			b = b.getRelative(direction);
			--maxwidth;
			if (maxwidth <= 0) return false;
		}
	}
	
	public static <E extends Enum<E>> E parse(E[] enumeration, String name, E def, boolean contain) {
		if (name == null) return def;
		name = name.toLowerCase();
		if (contain) {
			for (E e : enumeration) {
				if (name.contains(e.toString().toLowerCase())) {
					return e;
				}
			}
		} else {
			for (E e : enumeration) {
				if (e.toString().equalsIgnoreCase(name)) {
					return e;
				}
			}
			for (E e : enumeration) {
				if (e.toString().toLowerCase().contains(name)) {
					return e;
				}
			}
		}
		return def;
	}
	public static GameMode parseGameMode(String name, GameMode def) {
		return parse(GameMode.values(), name, def, false);
	}
	public static Environment parseEnvironment(String name, Environment def) {
		return parse(Environment.values(), name, def, true);
	}
	public static Difficulty parseDifficulty(String name, Difficulty def) {
		return parse(Difficulty.values(), name, def, false);
	}

	public static boolean getBool(String name) {
		name = name.toLowerCase().trim();
		if (name.equals("yes")) return true;
		if (name.equals("allow")) return true;
		if (name.equals("true")) return true;
		if (name.equals("ye")) return true;
		if (name.equals("y")) return true;
		if (name.equals("t")) return true;
		if (name.equals("on")) return true;
		if (name.equals("enabled")) return true;
		if (name.equals("enable")) return true;
		return false;
	}
	public static boolean isBool(String name) {
		name = name.toLowerCase().trim();
		if (name.equals("yes")) return true;
		if (name.equals("allow")) return true;
		if (name.equals("true")) return true;
		if (name.equals("ye")) return true;
		if (name.equals("y")) return true;
		if (name.equals("t")) return true;
		if (name.equals("on")) return true;
		if (name.equals("enabled")) return true;
		if (name.equals("enable")) return true;
		if (name.equals("no")) return true;
		if (name.equals("deny")) return true;
		if (name.equals("false")) return true;
		if (name.equals("n")) return true;
		if (name.equals("f")) return true;
		if (name.equals("off")) return true;
		if (name.equals("disabled")) return true;
		if (name.equals("disable")) return true;
		return false;
	}
	
	public static void message(Object sender, String message) {
		if (message != null && message.length() > 0) {
			if (sender instanceof CommandSender) {
				if (!(sender instanceof Player)) {
					message = ChatColor.stripColor(message);
				}
				for (String line : message.split("\n")) {
					((CommandSender) sender).sendMessage(line);
				}
			}
		}
	}
	
	public static void notifyConsole(CommandSender sender, String message) {
		if (sender instanceof Player) {
			MyWorlds.log(Level.INFO, ((Player) sender).getName() + " " + message);
		}
	}
	
	public static String[] convertArgs(String[] args) {
		ArrayList<String> tmpargs = new ArrayList<String>();
		boolean isCommenting = false;
		for (String arg : args) {
			if (!isCommenting && (arg.startsWith("\"") || arg.startsWith("'"))) {
				if (arg.endsWith("\"") && arg.length() > 1) {
					tmpargs.add(arg.substring(1, arg.length() - 1));
				} else {
					isCommenting = true;
					tmpargs.add(arg.substring(1));
				}
			} else if (isCommenting && (arg.endsWith("\"") || arg.endsWith("'"))) {
				arg = arg.substring(0, arg.length() - 1);
				arg = tmpargs.get(tmpargs.size() - 1) + " " + arg;
				tmpargs.set(tmpargs.size() - 1, arg);
				isCommenting = false;
			} else if (isCommenting) {
				arg = tmpargs.get(tmpargs.size() - 1) + " " + arg;
				tmpargs.set(tmpargs.size() - 1, arg);
			} else {
				tmpargs.add(arg);
			}
		}
		return tmpargs.toArray(new String[0]);
	}
	

	public static void list(CommandSender sender, String delimiter, String... items) {
		String msgpart = "";
		for (String item : items) {
			//display it
			if (msgpart.length() + item.length() < 70) {
				if (msgpart != "") msgpart += ChatColor.WHITE + delimiter;
				msgpart += item;
			} else {
				message(sender, msgpart);
				msgpart = item;
			}
		}
		//possibly forgot one?
		if (msgpart != "") message(sender, msgpart);
	}
}

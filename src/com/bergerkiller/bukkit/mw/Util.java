package com.bergerkiller.bukkit.mw;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World.Environment;

public class Util {

	public static <E extends Enum<E>> E parse(E[] enumeration, String name, E def) {
		if (name == null) return def;
		name = name.toLowerCase();
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
		return def;
	}
	public static GameMode parseGameMode(String name, GameMode def) {
		return parse(GameMode.values(), name, def);
	}
	public static Environment parseEnvironment(String name, Environment def) {
		return parse(Environment.values(), name, def);
	}
	public static Difficulty parseDifficulty(String name, Difficulty def) {
		return parse(Difficulty.values(), name, def);
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
}

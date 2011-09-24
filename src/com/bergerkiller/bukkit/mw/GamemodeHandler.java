package com.bergerkiller.bukkit.mw;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GamemodeHandler {
	private static HashMap<String, GameMode> worldModes = new HashMap<String, GameMode>();
	
	public static GameMode getMode(String gamemmodename, GameMode def) {
		for (GameMode mode : GameMode.values()) {
			if (mode.name().equalsIgnoreCase(gamemmodename)) {
				return mode;
			}
		}
		return def;
	}
	
	public static GameMode get(World world, GameMode def) {
		return get(world.getName(), def);
	}
	public static GameMode get(String worldname, GameMode def) {
		GameMode mode = worldModes.get(worldname.toLowerCase());
		if (mode == null) return def;
		return mode;
	}
	public static void set(String worldname, GameMode mode) {
		if (mode == null) {
			worldModes.remove(worldname.toLowerCase());
		} else {
			worldModes.put(worldname.toLowerCase(), mode);
		}

	}
	public static void set(String worldname, String modename) {
		set(worldname, getMode(modename, null));
	}
	
	public static void updatePlayer(Player p, World world) {
		GameMode mode = get(world, null);
		if (mode != null) p.setGameMode(mode);
	}
	public static void updatePlayers(World world) {
		for (Player p : world.getPlayers()) {
			updatePlayer(p, world);
		}
	}
	public static void updateAll() {
		for (World w : Bukkit.getServer().getWorlds()) {
			updatePlayers(w);
		}
	}

	public static void save(String filename) {
		SafeWriter writer = new SafeWriter(filename);
		for (Map.Entry<String, GameMode> mode : worldModes.entrySet()) {
			writer.writeLine("\"" + mode.getKey() + "\" " + mode.getValue().name());
		}
		writer.close();
	}
	public static void load(String filename) {
		for (String textline : SafeReader.readAll(filename)) {
			int index = textline.indexOf("\"");
			if (index >= 0) {
				textline = textline.substring(index + 1);
				index = textline.indexOf("\"");
				if (index > 0) {
					String worldname = textline.substring(0, index);
					textline = textline.substring(index + 1).trim();
					//read mode
					GameMode mode = getMode(textline, null);
					if (mode != null) {
						worldModes.put(worldname, mode);
					}
				}
			}
		}
		updateAll();
	}
	
}
